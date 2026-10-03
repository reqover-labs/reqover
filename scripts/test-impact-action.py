#!/usr/bin/env python3
"""Exercise the composite Action's analysis script with the real source-built CLI."""

import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
ARGS = argparse.ArgumentParser()
ARGS.add_argument("--cli", required=True)
ARGS.add_argument("--bash", default="bash")
OPTIONS, TEST_ARGS = ARGS.parse_known_args()
CLI = Path(OPTIONS.cli).resolve()


class ImpactActionTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="reqover action ")
        self.addCleanup(self.temp.cleanup)
        self.repo = Path(self.temp.name)
        self.git("init", "-q")
        self.git("config", "user.name", "Action test")
        self.git("config", "user.email", "test@example.invalid")
        self.source = self.repo / "src/main/java/example/SharedService.java"
        self.source.parent.mkdir(parents=True)
        self.source.write_text("class SharedService {}\n", encoding="utf-8")
        self.git("add", "src")
        self.git("commit", "-qm", "baseline")
        self.base = self.git("rev-parse", "HEAD").stdout.strip()
        self.source.write_text("class SharedService { void run() {} }\n", encoding="utf-8")
        self.git("add", "src")
        self.git("commit", "-qm", "change")
        self.report = self.repo / "recorded report.json"
        self.report.write_text(json.dumps({
            "schemaVersion": 1, "generatedAt": "2026-10-03T00:00:00Z", "completedRequestCount": 1,
            "endpoints": [], "reverseIndex": [{"className": "example.SharedService",
                "methodName": "run", "descriptor": "()V", "endpoints": ["GET /orders/{id}"]}]
        }), encoding="utf-8")
        (self.repo / ".env").write_text("SENTINEL_DO_NOT_UPLOAD=local_fixture\n", encoding="utf-8")
        self.runner = self.repo / "runner temp"
        self.runner.mkdir()
        self.output = self.repo / "outputs.txt"
        self.summary = self.repo / "summary.md"

    def git(self, *args):
        return subprocess.run(["git", *args], cwd=self.repo, text=True, capture_output=True, check=True)

    def run_analysis(self, **overrides):
        env = dict(os.environ, REPORT=self.report.as_posix(), CLI_JAR=CLI.as_posix(),
                   BASE_REF=self.base, PR_BASE="", RUNNER_TEMP=self.runner.as_posix(),
                   GITHUB_OUTPUT=self.output.as_posix(), GITHUB_STEP_SUMMARY=self.summary.as_posix(),
                   REQOVER_VERSION="0.2.0", REQOVER_PYTHON=Path(sys.executable).as_posix())
        env.update(overrides)
        return subprocess.run([OPTIONS.bash, (ROOT / ".github/actions/impact/analyse.sh").as_posix()],
                              cwd=self.repo, env=env, text=True, capture_output=True)

    def values(self):
        return dict(line.split("=", 1) for line in self.output.read_text(encoding="utf-8").splitlines()
                    if "=" in line and not line.startswith("<!--"))

    def test_real_cli_generates_summary_html_and_structured_outputs(self):
        result = self.run_analysis()
        self.assertEqual(0, result.returncode, result.stderr)
        outputs = self.values()
        self.assertEqual("true", outputs["has-impact"])
        self.assertEqual("1", outputs["impacted-endpoint-count"])
        self.assertEqual("0", outputs["unmatched-path-count"])
        files = {p.name for p in Path(outputs["artifact-directory"]).iterdir()}
        self.assertEqual({"report.html", "impact.md", "impact.json"}, files)
        self.assertIn("GET /orders/{id}", self.summary.read_text(encoding="utf-8"))
        self.assertIn("reqover-workspace", Path(outputs["html-path"]).read_text(encoding="utf-8"))
        self.assertTrue(Path(outputs["markdown-path"]).read_text(encoding="utf-8").startswith("<!-- reqover-impact -->"))
        self.assertNotIn("SENTINEL_DO_NOT_UPLOAD", "".join(p.read_text(encoding="utf-8")
                                                        for p in Path(outputs["artifact-directory"]).iterdir()))

    def test_unobserved_change_stays_visible_and_is_not_a_safety_verdict(self):
        self.report.write_text(json.dumps({"schemaVersion": 1, "generatedAt": "2026-10-03T00:00:00Z",
                                            "completedRequestCount": 0, "endpoints": [], "reverseIndex": []}), encoding="utf-8")
        result = self.run_analysis()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("false", self.values()["has-impact"])
        self.assertEqual("1", self.values()["unmatched-path-count"])

    def test_missing_report_fails_with_an_actionable_message(self):
        result = self.run_analysis(REPORT=(self.repo / "missing.json").as_posix())
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Record", result.stderr)
        self.assertFalse(self.output.exists())

    def test_bad_base_ref_fails_before_publishing_results(self):
        result = self.run_analysis(BASE_REF="not-a-git-ref")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("base-ref", result.stderr)
        self.assertFalse(self.output.exists())

    def test_pr_base_advancement_does_not_truncate_checkout_history(self):
        head = self.git("rev-parse", "HEAD").stdout.strip()
        remote = self.repo / "origin.git"
        self.git("init", "--bare", "-q", remote.as_posix())
        self.git("remote", "add", "origin", remote.as_posix())
        self.git("switch", "--detach", self.base)
        (self.repo / "README.md").write_text("Base branch advanced after the PR diverged.\n", encoding="utf-8")
        self.git("add", "README.md")
        self.git("commit", "-qm", "advance base")
        self.git("push", "origin", "HEAD:refs/heads/main")
        self.git("switch", "--detach", head)
        result = self.run_analysis(BASE_REF="", PR_BASE="main")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("true", self.values()["has-impact"])
        self.assertFalse((self.repo / ".git/shallow").exists(), "full checkout history must not be made shallow")

    def test_action_uploads_before_optional_gate_and_does_not_comment_on_forks(self):
        action = (ROOT / ".github/actions/impact/action.yml").read_text(encoding="utf-8")
        self.assertIn("github.event.pull_request.head.repo.full_name == github.repository", action)
        self.assertLess(action.index("name: Upload diagnostic artifacts"), action.index("name: Apply the optional impact gate"))
        self.assertIn("continue-on-error: true", action, "a read-only token must not discard successful analysis")


if __name__ == "__main__":
    if not CLI.is_file():
        raise SystemExit("Build the CLI JAR before running this test: " + str(CLI))
    if not shutil.which(OPTIONS.bash):
        raise SystemExit("Bash not found: " + OPTIONS.bash)
    unittest.main(argv=[sys.argv[0], *TEST_ARGS], verbosity=2)
