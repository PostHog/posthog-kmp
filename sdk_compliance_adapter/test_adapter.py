import gzip
import http.client
from http.server import BaseHTTPRequestHandler, HTTPServer
import json
import os
from pathlib import Path
import subprocess
import sys
import threading
import time
import unittest
from unittest.mock import MagicMock, patch

from adapter import Worker
from check_report import check


class ReportTest(unittest.TestCase):
    def setUp(self):
        self.inventory = json.loads(Path(__file__).with_name("inventory.json").read_text())
        suites = {}
        for name in self.inventory["tests"]:
            suite, test = name.split(".", 1)
            suites.setdefault(suite, []).append({"name": test, "passed": False, "message": "SDK mismatch"})
        self.report = {
            "sdk_name": "posthog-kmp-jvm",
            "summary": {"total": 47, "passed": 0, "failed": 47},
            "suites": [{"name": name, "tests": tests} for name, tests in suites.items()],
        }

    def test_all_47_results_are_required_but_assertions_are_advisory(self):
        with patch("builtins.print"):
            check(self.report, self.inventory)

    def test_empty_report_is_rejected(self):
        self.report["suites"] = []
        with self.assertRaises(ValueError):
            check(self.report, self.inventory)

    def test_missing_and_duplicate_cases_are_rejected(self):
        tests = self.report["suites"][0]["tests"]
        tests[0] = tests[1]
        with self.assertRaises(ValueError):
            check(self.report, self.inventory)

    def test_inconsistent_summary_is_rejected(self):
        self.report["summary"]["passed"] = 47
        with self.assertRaises(ValueError):
            check(self.report, self.inventory)


class WorkerTest(unittest.TestCase):
    def test_reset_waits_for_only_owned_worker_before_removing_its_home(self):
        worker = Worker()
        worker.process = MagicMock()
        worker.home = "/owned/test/home"
        actions = MagicMock()
        actions.attach_mock(worker.process, "process")
        with patch("adapter.shutil.rmtree") as remove:
            actions.attach_mock(remove, "remove")
            worker.stop()
            self.assertEqual(
                [call[0] for call in actions.mock_calls],
                ["process.kill", "process.wait", "remove"],
            )
            remove.assert_called_once_with("/owned/test/home")
        self.assertIsNone(worker.process)
        self.assertIsNone(worker.home)


class ProcessIsolationTest(unittest.TestCase):
    def test_reset_discards_old_process_without_delivering_its_pending_events(self):
        events = []

        class MockHandler(BaseHTTPRequestHandler):
            def do_GET(self):
                self.respond()

            def do_POST(self):
                raw = self.rfile.read(int(self.headers.get("Content-Length", "0")))
                if self.path.rstrip("/") == "/batch":
                    events.extend(json.loads(gzip.decompress(raw))["batch"])
                self.respond()

            def respond(self):
                self.send_response(200)
                self.send_header("Content-Length", "2")
                self.end_headers()
                self.wfile.write(b"{}")

            def log_message(self, *_):
                pass

        def request(path, body=None):
            connection = http.client.HTTPConnection("127.0.0.1", 18322, timeout=40)
            try:
                connection.request("GET" if body is None else "POST", path, json.dumps(body or {}))
                response = connection.getresponse()
                self.assertEqual(response.status, 200)
                return json.loads(response.read())
            finally:
                connection.close()

        mock = HTTPServer(("127.0.0.1", 19325), MockHandler)
        thread = threading.Thread(target=mock.serve_forever)
        thread.start()
        process = subprocess.Popen(
            [sys.executable, str(Path(__file__).with_name("adapter.py"))],
            env=dict(os.environ, PORT="18322", WORKER_PORT="18323", PROXY_PORT="19324"),
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
        try:
            for _ in range(300):
                self.assertIsNone(process.poll())
                try:
                    request("/health")
                    break
                except OSError:
                    time.sleep(0.1)
            else:
                self.fail("Controller did not become healthy")
            config = {"host": "http://127.0.0.1:19325", "api_key": "phc_isolation_test", "flush_at": 100}
            request("/init", config)
            request("/capture", {"distinct_id": "old-user", "event": "old-pending-event"})
            request("/reset", {})
            request("/init", config)
            request("/capture", {"distinct_id": "new-user", "event": "new-event"})
            result = request("/flush", {})
            self.assertFalse(result["success"])
            self.assertTrue(result["flush_invoked"])
            self.assertNotIn("old-pending-event", [event["event"] for event in events])
            self.assertIn("new-event", [event["event"] for event in events])
            request("/reset", {})
        finally:
            process.terminate()
            process.wait(timeout=15)
            mock.shutdown()
            mock.server_close()
            thread.join(timeout=5)


if __name__ == "__main__":
    unittest.main()
