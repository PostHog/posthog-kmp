#!/usr/bin/env python3
"""HTTP lifecycle controller. Each init owns a fresh, real KMP JVM process."""

import http.client
import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import sys
import tempfile
import time
from http.server import BaseHTTPRequestHandler, HTTPServer


class Worker:
    def __init__(self):
        self.process = None
        self.home = None
        self.port = int(os.environ.get("WORKER_PORT", "18321"))
        self.health = None

    def request(self, method, path, body=b""):
        connection = http.client.HTTPConnection("127.0.0.1", self.port, timeout=40)
        try:
            connection.request(method, path, body, {"Content-Type": "application/json"})
            response = connection.getresponse()
            return response.status, response.read()
        finally:
            connection.close()

    def start(self):
        self.stop()
        self.home = tempfile.mkdtemp(prefix="posthog-kmp-worker-")
        executable = Path(__file__).parent / "build/install/sdk_compliance_adapter/bin/sdk_compliance_adapter"
        env = dict(os.environ, PORT=str(self.port))
        env["JAVA_OPTS"] = f'-Duser.home={self.home} -Djava.io.tmpdir={self.home}'
        self.process = subprocess.Popen([str(executable)], env=env)
        deadline = time.monotonic() + 30
        while time.monotonic() < deadline:
            if self.process.poll() is not None:
                raise RuntimeError(f"KMP JVM exited with {self.process.returncode}")
            try:
                status, body = self.request("GET", "/health")
                if status == 200:
                    self.health = body
                    return
            except OSError:
                pass
            time.sleep(0.1)
        raise TimeoutError("KMP JVM health timed out")

    def stop(self):
        if self.process is not None:
            # Public close queues a final flush and is not a quiescence barrier. Terminate only
            # this owned test process so no previous case can write into the next mock session.
            self.process.kill()
            self.process.wait(timeout=10)
            self.process = None
        if self.home is not None:
            shutil.rmtree(self.home)
            self.home = None


def main():
    worker = Worker()

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            self.dispatch()

        def do_POST(self):
            self.dispatch()

        def dispatch(self):
            body = self.rfile.read(int(self.headers.get("Content-Length", "0")))
            path = self.path.split("?", 1)[0]
            try:
                if path == "/init":
                    worker.start()
                if path == "/reset":
                    worker.stop()
                    status, response = 200, b'{"success":true}'
                elif path == "/health":
                    status, response = 200, worker.health
                elif worker.process is None:
                    status, response = 400, b'{"success":false,"error":"Initialize the SDK first"}'
                else:
                    status, response = worker.request(self.command, path, body)
            except (OSError, RuntimeError, TimeoutError) as error:
                status, response = 500, json.dumps({"success": False, "error": str(error)}).encode()
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(response)))
            self.end_headers()
            self.wfile.write(response)

    signal.signal(signal.SIGTERM, lambda *_: sys.exit(0))
    try:
        worker.start()
        with HTTPServer(("127.0.0.1", int(os.environ.get("PORT", "18320"))), Handler) as server:
            server.serve_forever()
    finally:
        worker.stop()


if __name__ == "__main__":
    main()
