#!/usr/bin/env python3
"""Require the full pinned inventory, while keeping compliance assertions advisory."""

from collections import Counter
import json
from pathlib import Path
import sys


def check(report, inventory):
    expected = Counter(inventory["tests"])
    actual = Counter(
        f'{suite["name"]}.{test["name"]}'
        for suite in report["suites"] for test in suite["tests"]
    )
    if not expected or actual != expected:
        raise ValueError(f"Inventory mismatch: missing={expected - actual}, unexpected={actual - expected}")
    if report["sdk_name"] != inventory["sdk_name"]:
        raise ValueError("Report does not describe the KMP JVM facade")
    tests = [test for suite in report["suites"] for test in suite["tests"]]
    passed = sum(test["passed"] is True for test in tests)
    failed = sum(test["passed"] is False for test in tests)
    if report["summary"] != {"total": len(tests), "passed": passed, "failed": failed} or passed + failed != len(tests):
        raise ValueError("Report summary does not match individual results")
    print(f"KMP JVM: {len(tests)} selected, {passed} passed, {failed} failed")
    for suite in report["suites"]:
        for test in suite["tests"]:
            if not test["passed"]:
                print(f'FAIL {suite["name"]}.{test["name"]}: {test["message"]}')


if __name__ == "__main__":
    report = json.loads(Path(sys.argv[1]).read_text())
    inventory = json.loads(Path(__file__).with_name("inventory.json").read_text())
    check(report, inventory)
