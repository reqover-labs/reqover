#!/usr/bin/env python3
"""Verify that a generated CycloneDX SBOM matches the checked-in dependency lock."""

from __future__ import annotations

import argparse
import copy
import difflib
import json
import sys
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("generated", type=Path)
    parser.add_argument("checked_in", type=Path)
    return parser.parse_args()


def normalized(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    data = copy.deepcopy(data)
    data.pop("serialNumber", None)
    metadata = data.get("metadata", {})
    metadata.pop("timestamp", None)

    # CycloneDX adds run-specific provenance when Gradle executes in GitHub
    # Actions. That URL changes on every run and the plugin also canonicalizes
    # the repository URL by dropping a trailing ".git". Neither changes the
    # dependency inventory that this checked-in lock is intended to protect.
    root_component = metadata.get("component", {})
    external_references = root_component.get("externalReferences", [])
    normalized_references = []
    for reference in external_references:
        if reference.get("type") == "build-system":
            continue
        normalized_reference = copy.deepcopy(reference)
        if normalized_reference.get("type") == "vcs":
            url = normalized_reference.get("url")
            if isinstance(url, str) and url.endswith(".git"):
                normalized_reference["url"] = url[:-4]
        normalized_references.append(normalized_reference)
    if "externalReferences" in root_component:
        root_component["externalReferences"] = normalized_references
    return data


def main() -> int:
    args = parse_args()
    generated = normalized(args.generated)
    checked_in = normalized(args.checked_in)
    if generated == checked_in:
        print("SBOM lock matches generated dependency inventory")
        return 0

    before = json.dumps(checked_in, ensure_ascii=False, indent=2, sort_keys=True).splitlines()
    after = json.dumps(generated, ensure_ascii=False, indent=2, sort_keys=True).splitlines()
    diff = list(
        difflib.unified_diff(
            before,
            after,
            fromfile=str(args.checked_in),
            tofile=str(args.generated),
            lineterm="",
        )
    )
    print("\n".join(diff[:300]), file=sys.stderr)
    print(
        "Generated SBOM differs from the checked-in lock; regenerate and review sbom/reqover.cdx.json",
        file=sys.stderr,
    )
    return 1


if __name__ == "__main__":
    sys.exit(main())
