#!/usr/bin/env bash
set -euo pipefail

URL="${1:-}"
WARMUP_REQUESTS="${2:-50}"
MEASURED_REQUESTS="${3:-300}"
SAMPLES_FILE="${4:-}"
TIMEOUT_SECONDS="${REQOVER_BENCHMARK_TIMEOUT_SECONDS:-5}"

if [[ -z "$URL" || ! "$WARMUP_REQUESTS" =~ ^[0-9]+$ || ! "$MEASURED_REQUESTS" =~ ^[1-9][0-9]*$ ]]; then
    echo "usage: $0 <url> [warmup-requests] [measured-requests] [samples-file]" >&2
    exit 1
fi

TASK_TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TASK_TMP_DIR"' EXIT
RAW="$TASK_TMP_DIR/raw-ms.txt"
SORTED="$TASK_TMP_DIR/sorted-ms.txt"

timed_request() {
    curl --fail --silent --show-error --output /dev/null \
        --max-time "$TIMEOUT_SECONDS" \
        --write-out '%{time_total}\n' \
        "$URL" | awk '{ printf "%.6f\n", $1 * 1000 }'
}

for ((i = 0; i < WARMUP_REQUESTS; i++)); do
    timed_request >/dev/null
done

for ((i = 0; i < MEASURED_REQUESTS; i++)); do
    timed_request >>"$RAW"
done

sort -n "$RAW" >"$SORTED"

average="$(awk '{ sum += $1 } END { printf "%.3f", sum / NR }' "$RAW")"
minimum="$(sed -n '1p' "$SORTED")"
maximum="$(sed -n '${p;}' "$SORTED")"

percentile() {
    local numerator="$1"
    local rank=$(( (MEASURED_REQUESTS * numerator + 99) / 100 ))
    sed -n "${rank}p" "$SORTED"
}

p50="$(percentile 50)"
p95="$(percentile 95)"
p99="$(percentile 99)"

if [[ -n "$SAMPLES_FILE" ]]; then
    mkdir -p "$(dirname "$SAMPLES_FILE")"
    cp "$RAW" "$SAMPLES_FILE"
fi

printf '{\n'
printf '  "url": "%s",\n' "$URL"
printf '  "warmupRequests": %d,\n' "$WARMUP_REQUESTS"
printf '  "measuredRequests": %d,\n' "$MEASURED_REQUESTS"
printf '  "percentileMethod": "nearest-rank",\n'
printf '  "averageMs": %s,\n' "$average"
printf '  "p50Ms": %s,\n' "$p50"
printf '  "p95Ms": %s,\n' "$p95"
printf '  "p99Ms": %s,\n' "$p99"
printf '  "minMs": %s,\n' "$minimum"
printf '  "maxMs": %s\n' "$maximum"
printf '}\n'
