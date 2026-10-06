#!/usr/bin/env bash
set -euo pipefail

if [ "$(git rev-parse --is-shallow-repository)" = "true" ]; then
    echo "::error::Reqover needs full checkout history. Set actions/checkout fetch-depth: 0 before this Action." >&2
    exit 1
fi
if ! command -v "${REQOVER_PYTHON:-python3}" > /dev/null 2>&1; then
    echo "::error::Python 3 is required; install python3 in the job/container before using this Action." >&2
    exit 1
fi

if [ ! -s "${REPORT}" ]; then
    echo "::error::Record a Reqover JSON report before this Action; no readable data at ${REPORT}" >&2
    exit 1
fi

base="${BASE_REF:-}"
if [ -z "${base}" ]; then
    if [ -z "${PR_BASE:-}" ]; then
        echo "::error::base-ref is required outside a pull request" >&2
        exit 1
    fi
    # Do not make a full checkout shallow when the base has advanced past the PR.
    git fetch --no-tags origin "${PR_BASE}"
    base="FETCH_HEAD"
fi
if ! base_sha="$(git rev-parse --verify --end-of-options "${base}^{commit}" 2>/dev/null)"; then
    echo "::error::base-ref does not resolve to a commit: ${base}" >&2
    exit 1
fi
if ! git merge-base "${base_sha}" HEAD > /dev/null; then
    echo "::error::No merge base. Use actions/checkout with fetch-depth: 0 before Reqover." >&2
    exit 1
fi

work="$(mktemp -d "${RUNNER_TEMP}/reqover.XXXXXX")"
artifacts="${work}/artifacts"
mkdir -p "${artifacts}"
cli="${CLI_JAR:-}"
if [ -n "${cli}" ]; then
    if [ ! -s "${cli}" ]; then
        echo "::error::cli-jar does not exist or is empty: ${cli}" >&2
        exit 1
    fi
else
    if [[ ! "${REQOVER_VERSION}" =~ ^[0-9][0-9A-Za-z.+-]*$ ]]; then
        echo "::error::version must be a release version without the leading v" >&2
        exit 1
    fi
    cli="${work}/reqover-cli.jar"
    curl --silent --show-error --fail --location --retry 2 \
        --output "${cli}" \
        "https://github.com/reqover-labs/reqover/releases/download/v${REQOVER_VERSION}/reqover-cli-${REQOVER_VERSION}.jar"
fi

git diff --name-only "${base_sha}...HEAD" > "${work}/changed.txt"
java -jar "${cli}" impact --report "${REPORT}" --changed-files "${work}/changed.txt" \
    --format markdown --out "${work}/impact-body.md"
java -jar "${cli}" impact --report "${REPORT}" --changed-files "${work}/changed.txt" \
    --format json --out "${artifacts}/impact.json"
java -jar "${cli}" render --report "${REPORT}" --out "${artifacts}/report.html"
{
    echo '<!-- reqover-impact -->'
    cat "${work}/impact-body.md"
} > "${artifacts}/impact.md"

export REQOVER_ARTIFACT_DIRECTORY="${artifacts}"
"${REQOVER_PYTHON:-python3}" - <<'PY'
import json
import os
from pathlib import Path
import uuid

directory = Path(os.environ["REQOVER_ARTIFACT_DIRECTORY"])
impact = json.loads((directory / "impact.json").read_text(encoding="utf-8"))
if not isinstance(impact.get("hasImpact"), bool):
    raise SystemExit("CLI impact JSON must contain a boolean hasImpact")
markdown = (directory / "impact.md").read_text(encoding="utf-8")
values = {
    "has-impact": str(impact["hasImpact"]).lower(),
    "impacted-endpoint-count": len(impact["endpoints"]),
    "unmatched-path-count": len(impact["unmatchedPaths"]),
    "artifact-directory": str(directory),
    "html-path": str(directory / "report.html"),
    "json-path": str(directory / "impact.json"),
    "markdown-path": str(directory / "impact.md"),
}
delimiter = "REQOVER_" + uuid.uuid4().hex
with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8", newline="\n") as output:
    for name, value in values.items():
        output.write(f"{name}={value}\n")
    output.write(f"markdown<<{delimiter}\n{markdown}\n{delimiter}\n")
with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8", newline="\n") as summary:
    summary.write(markdown + "\n")
PY
