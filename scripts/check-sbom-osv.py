#!/usr/bin/env python3
"""Check exact Maven component versions from a CycloneDX SBOM against OSV."""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

OSV_QUERY_BATCH = "https://api.osv.dev/v1/querybatch"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("sbom", type=Path, help="CycloneDX JSON file")
    parser.add_argument("--output", type=Path, help="Optional JSON evidence output")
    parser.add_argument("--timeout", type=int, default=30)
    return parser.parse_args()


def maven_components(sbom: dict) -> list[dict[str, str]]:
    components: list[dict[str, str]] = []
    seen: set[tuple[str, str]] = set()
    for component in sbom.get("components", []):
        group = component.get("group")
        name = component.get("name")
        version = component.get("version")
        purl = component.get("purl", "")
        if not group or group == "io.reqover" or not name or not version:
            continue
        if purl and not purl.startswith("pkg:maven/"):
            continue
        coordinate = f"{group}:{name}"
        key = (coordinate, version)
        if key in seen:
            continue
        seen.add(key)
        components.append({"coordinate": coordinate, "version": version, "purl": purl})
    return components


def query_osv(components: list[dict[str, str]], timeout: int) -> dict:
    request_body = {
        "queries": [
            {
                "version": component["version"],
                "package": {"ecosystem": "Maven", "name": component["coordinate"]},
            }
            for component in components
        ]
    }
    request = urllib.request.Request(
        OSV_QUERY_BATCH,
        data=json.dumps(request_body).encode("utf-8"),
        headers={"Content-Type": "application/json", "User-Agent": "reqover-sbom-check/0.1.0"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.load(response)
    except (urllib.error.URLError, TimeoutError) as error:
        raise SystemExit(f"OSV request failed: {error}") from error


def main() -> int:
    args = parse_args()
    with args.sbom.open(encoding="utf-8") as handle:
        sbom = json.load(handle)

    components = maven_components(sbom)
    response = query_osv(components, args.timeout)
    results = response.get("results", [])
    if len(results) != len(components):
        raise SystemExit(
            f"OSV returned {len(results)} results for {len(components)} queries; refusing a partial verdict"
        )

    findings = []
    for component, result in zip(components, results, strict=True):
        vulnerabilities = result.get("vulns", [])
        if vulnerabilities:
            findings.append(
                {
                    **component,
                    "vulnerabilities": [
                        {"id": item.get("id"), "modified": item.get("modified")}
                        for item in vulnerabilities
                    ],
                }
            )

    evidence = {
        "checkedAt": datetime.now(timezone.utc).isoformat(),
        "source": OSV_QUERY_BATCH,
        "sbom": str(args.sbom),
        "componentQueries": len(components),
        "vulnerableComponents": len(findings),
        "findings": findings,
    }

    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(json.dumps(evidence, ensure_ascii=False, indent=2))
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main())
