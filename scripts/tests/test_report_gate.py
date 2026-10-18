"""Synthetic XML tests for the report gate only. These are NOT application test reports."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location("report_gate", Path(__file__).parents[1] / "assert-integration-reports.py")
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)

class ReportGateTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.file = Path(self.directory.name) / "synthetic-unit-fixture.xml"
    def report(self, **changes):
        values = dict(tests=15, errors=0, failures=0, skipped=0)
        values.update(changes)
        self.file.write_text('<testsuite ' + ' '.join(f'{k}="{v}"' for k, v in values.items()) + '/>')
    def test_complete_report_passes(self):
        self.report(); self.assertTrue(gate.inspect_report(self.file, 15)[0])
    def test_additional_cases_are_allowed(self):
        self.report(tests=20); self.assertTrue(gate.inspect_report(self.file, 15)[0])
    def test_missing_report_fails(self):
        self.assertFalse(gate.inspect_report(self.file, 15)[0])
    def test_skipped_case_fails(self):
        self.report(skipped=1); self.assertFalse(gate.inspect_report(self.file, 15)[0])
    def test_failure_fails(self):
        self.report(failures=1); self.assertFalse(gate.inspect_report(self.file, 15)[0])
    def test_error_fails(self):
        self.report(errors=1); self.assertFalse(gate.inspect_report(self.file, 15)[0])
    def test_too_few_cases_fail(self):
        self.report(tests=14); self.assertFalse(gate.inspect_report(self.file, 15)[0])
    def test_malformed_xml_fails(self):
        self.file.write_text('<broken'); self.assertFalse(gate.inspect_report(self.file, 15)[0])
    def test_missing_count_fails(self):
        self.file.write_text('<testsuite tests="15"/>'); self.assertFalse(gate.inspect_report(self.file, 15)[0])

if __name__ == "__main__":
    unittest.main()
