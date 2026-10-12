#!/usr/bin/env python3
# Copyright 2026 Reqover contributors. All Rights Reserved.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# SPDX-License-Identifier: Apache-2.0
"""Offline regression checks for narrowly documented Spring advisory exceptions."""

import importlib.util
import io
import json
import tempfile
import tomllib
import unittest
import zipfile
from contextlib import redirect_stdout
from datetime import datetime, timezone
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

ROOT = Path(__file__).resolve().parent.parent
SPEC = importlib.util.spec_from_file_location("osv_check", ROOT / "scripts/check-sbom-osv.py")
CHECK = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(CHECK)
EXPECTED = {
    "GHSA-pc63-qcmh-9cmg": {"spring-webmvc"},
    "GHSA-9qf2-26p9-2q2q": {"spring-webflux"},
    "GHSA-j9f9-w8pj-32f8": {"spring-webmvc", "spring-webflux"},
}


class AdvisoryPolicyTest(unittest.TestCase):
    def test_build_and_release_verify_the_exception_scope_after_building(self):
        for name in ("build.yml", "release.yml"):
            workflow = (ROOT / ".github/workflows" / name).read_text()
            verify_job = workflow.split("  osv:", 1)[0]
            command = "run: python3 scripts/test-osv-policy.py"
            self.assertIn(command, verify_job, name)
            self.assertLess(verify_job.index("run: ./gradlew clean build"), verify_job.index(command))
            self.assertIn("fail-on-vuln: true", workflow)

    def test_only_reviewed_advisories_have_dated_reasons(self):
        with (ROOT / "osv-scanner.toml").open("rb") as handle:
            entries = tomllib.load(handle)["IgnoredVulns"]
        self.assertEqual(set(EXPECTED), {entry["id"] for entry in entries})
        self.assertEqual(len(EXPECTED), len(entries))
        reviewed = datetime(2026, 10, 12, tzinfo=timezone.utc)
        for entry in entries:
            self.assertGreater(entry["ignoreUntil"], reviewed)
            self.assertLessEqual((entry["ignoreUntil"] - reviewed).days, 90)
            for phrase in ("6.2.19", "7.0.9", "compileOnly"):
                self.assertIn(phrase, entry["reason"])

    def test_exception_inventory_requires_the_reviewed_spring_version(self):
        components = CHECK.maven_components(json.loads((ROOT / "sbom/reqover.cdx.json").read_text()))
        versions = {c["coordinate"]: c["version"] for c in components}
        for name in set().union(*EXPECTED.values()):
            self.assertEqual("6.2.19", versions[f"org.springframework:{name}"],
                             "A version change requires revisiting, not silently carrying exceptions")

    def test_samples_and_report_routes_keep_exception_preconditions_absent(self):
        source_roots = [ROOT / "examples", ROOT / "reqover-spring-boot-starter/src/main"]
        forbidden = ("XsltView", "SseEmitter", "ServerSentEvent", "FragmentsRendering",
                     "RenderingResponse", "TEXT_EVENT_STREAM", "APPLICATION_EVENT_STREAM")
        for source_root in source_roots:
            for source in source_root.rglob("*.java"):
                if "src/main" not in source.as_posix():
                    continue
                content = source.read_text(encoding="utf-8")
                for token in forbidden:
                    self.assertNotIn(token, content, f"Re-review the exception scope: {source}")
        sample = (ROOT / "examples/webflux-sample/build.gradle.kts").read_text()
        self.assertIn('"org.springframework.boot:spring-boot-starter-webflux"', sample)
        self.assertNotIn('"org.springframework.boot:spring-boot-starter-web"', sample)
        self.assertNotIn("starter-tomcat", sample)
        route = (ROOT / "reqover-spring-boot-starter/src/main/java/io/reqover/spring/boot/ReqoverWebFluxReportEndpointAutoConfiguration.java").read_text()
        self.assertIn("ConditionalOnWebApplication.Type.REACTIVE", route)
        self.assertNotIn(".headers(", route)
        for name in ("reqover-spring-mvc", "reqover-spring-webflux", "reqover-spring-boot-starter"):
            build = (ROOT / name / "build.gradle.kts").read_text()
            for line in build.splitlines():
                if "org.springframework:" in line:
                    self.assertTrue(line.strip().startswith(("compileOnly(", "testImplementation(")), line)

    def test_built_libraries_do_not_ship_spring_and_reactive_sample_has_no_servlet_stack(self):
        version = json.loads((ROOT / "sbom/reqover.cdx.json").read_text())["metadata"]["component"]["version"]
        for name in ("reqover-spring-mvc", "reqover-spring-webflux", "reqover-spring-boot-starter"):
            with zipfile.ZipFile(ROOT / name / "build/libs" / f"{name}-{version}.jar") as jar:
                self.assertFalse(any(path.startswith("org/springframework/") for path in jar.namelist()))
                self.assertFalse(any(path.endswith(".jar") for path in jar.namelist()))
        with zipfile.ZipFile(ROOT / "examples/webflux-sample/build/libs" / f"webflux-sample-{version}.jar") as jar:
            libs = [Path(path).name for path in jar.namelist() if path.startswith("BOOT-INF/lib/")]
        self.assertTrue(any(name.startswith("reactor-netty-http-") for name in libs))
        self.assertFalse(any(name.startswith(("spring-webmvc-", "tomcat-", "jakarta.servlet-api-")) for name in libs))

    def test_expired_exception_is_inactive_and_missing_reason_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            config = Path(directory) / "policy.toml"
            config.write_text('[[IgnoredVulns]]\nid="expired"\nignoreUntil=2020-01-01T00:00:00Z\nreason="reviewed"\n')
            self.assertEqual({}, CHECK.active_exceptions(config))
            config.write_text('[[IgnoredVulns]]\nid="unreviewed"\nignoreUntil=2099-01-01T00:00:00Z\n')
            with self.assertRaises(SystemExit):
                CHECK.active_exceptions(config)

    def test_unknown_findings_still_fail_and_exceptions_remain_visible(self):
        with tempfile.TemporaryDirectory() as directory:
            sbom = Path(directory) / "bom.json"
            sbom.write_text(json.dumps({"components": [{"group": "example", "name": "lib", "version": "1"}]}))
            args = SimpleNamespace(sbom=sbom, output=None, timeout=1, config=Path(directory) / "policy.toml")
            response = {"results": [{"vulns": [{"id": "reviewed"}, {"id": "new-finding"}]}]}
            out = io.StringIO()
            with patch.object(CHECK, "parse_args", return_value=args), \
                    patch.object(CHECK, "query_osv", return_value=response), \
                    patch.object(CHECK, "active_exceptions", return_value={"reviewed": "2026-11-12"}), \
                    redirect_stdout(out):
                self.assertEqual(1, CHECK.main())
            evidence = json.loads(out.getvalue())
            self.assertEqual("new-finding", evidence["findings"][0]["vulnerabilities"][0]["id"])
            self.assertEqual("reviewed", evidence["exceptions"][0]["id"])

    def test_partial_osv_response_does_not_pass(self):
        with tempfile.TemporaryDirectory() as directory:
            sbom = Path(directory) / "bom.json"
            sbom.write_text(json.dumps({"components": [{"group": "example", "name": "lib", "version": "1"}]}))
            args = SimpleNamespace(sbom=sbom, output=None, timeout=1, config=Path(directory) / "policy.toml")
            with patch.object(CHECK, "parse_args", return_value=args), \
                    patch.object(CHECK, "query_osv", return_value={"results": []}):
                with self.assertRaisesRegex(SystemExit, "refusing a partial verdict"):
                    CHECK.main()


if __name__ == "__main__":
    unittest.main(verbosity=2)
