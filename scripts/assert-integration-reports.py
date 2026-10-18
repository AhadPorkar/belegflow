#!/usr/bin/env python3
"""Reject full-stack runs with missing, skipped or failed real-PostgreSQL suites."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

REQUIRED = {"IntegrationTest": 15, "ModuleIntegrationTest": 29, "ConcurrencyIntegrationTest": 3}

def inspect_report(file: Path, minimum: int) -> tuple[bool, str]:
    """Validate Surefire evidence, not the business implementation; fail closed on malformed XML."""
    if not file.is_file():
        return False, "report missing"
    try:
        xml = ET.parse(file).getroot()
        counts = {key: int(xml.attrib[key]) for key in ("tests", "errors", "failures", "skipped")}
    except (ET.ParseError, KeyError, ValueError, OSError) as error:
        return False, "invalid report: " + type(error).__name__
    okay = counts["tests"] >= minimum and all(counts[key] == 0 for key in ("errors", "failures", "skipped"))
    return okay, str(counts)

def main(root: Path | None = None) -> int:
    root = root or Path(__file__).resolve().parents[1]
    okay = True
    for suite, minimum in REQUIRED.items():
        file = root / "server/target/surefire-reports" / ("TEST-com.belegflow.server." + suite + ".xml")
        passed, detail = inspect_report(file, minimum)
        print("PASS" if passed else "FAIL", suite, detail)
        okay = okay and passed
    return 0 if okay else 1

if __name__ == "__main__":
    sys.exit(main())
