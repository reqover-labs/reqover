#!/usr/bin/env bash
set -euo pipefail

PORT="${1:-18180}"
WARMUP_REQUESTS="${2:-50}"
MEASURED_REQUESTS="${3:-300}"
OUTPUT_DIR="${4:-}"

if [[ ! "$PORT" =~ ^[0-9]+$ ]] || (( PORT < 1 || PORT > 65535 )); then
    echo "port must be an integer from 1 to 65535" >&2
    exit 1
fi

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

if [[ -n "$(git status --porcelain)" ]]; then
    echo "performance evidence requires a clean worktree so the commit SHA identifies the measured code" >&2
    exit 1
fi

if (: >"/dev/tcp/127.0.0.1/${PORT}") 2>/dev/null; then
    echo "port ${PORT} is already in use" >&2
    exit 1
fi

COMMIT_SHA="$(git rev-parse HEAD)"
SHORT_SHA="$(git rev-parse --short=12 HEAD)"
DATE_UTC="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
if [[ -z "$OUTPUT_DIR" ]]; then
    OUTPUT_DIR="docs/evidence/performance/${SHORT_SHA}"
fi
mkdir -p "$OUTPUT_DIR"

./gradlew :reqover-agent:shadowJar :examples:mvc-sample:bootJar --no-daemon --console=plain

AGENT_JAR="reqover-agent/build/libs/reqover-agent-0.1.0.jar"
APP_JAR="examples/mvc-sample/build/libs/mvc-sample-0.1.0.jar"
ENDPOINT="http://127.0.0.1:${PORT}/auto/orders/1"
APP_PID=""

stop_app() {
    if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
        kill "$APP_PID" 2>/dev/null || true
        for _ in $(seq 1 20); do
            kill -0 "$APP_PID" 2>/dev/null || break
            sleep 0.25
        done
        kill -9 "$APP_PID" 2>/dev/null || true
    fi
    if [[ -n "$APP_PID" ]]; then
        wait "$APP_PID" 2>/dev/null || true
    fi
    APP_PID=""
}
trap stop_app EXIT

wait_for_app() {
    local ready=0
    for _ in $(seq 1 120); do
        if ! kill -0 "$APP_PID" 2>/dev/null; then
            wait "$APP_PID" || true
            echo "sample application exited before readiness" >&2
            return 1
        fi
        if curl -sf --max-time 2 "$ENDPOINT" >/dev/null 2>&1; then
            ready=1
            break
        fi
        sleep 0.5
    done
    if [[ "$ready" -ne 1 ]]; then
        echo "sample application did not become ready within 60 seconds" >&2
        return 1
    fi
}

start_app() {
    local mode="$1"
    local log_file="$OUTPUT_DIR/${mode}-server.log"
    if [[ "$mode" == "baseline" ]]; then
        java -jar "$APP_JAR" \
            --server.address=127.0.0.1 \
            "--server.port=${PORT}" \
            --spring.main.banner-mode=off >"$log_file" 2>&1 &
    else
        java "-javaagent:${AGENT_JAR}=include=io.reqover.example.mvc.auto" \
            -jar "$APP_JAR" \
            --server.address=127.0.0.1 \
            "--server.port=${PORT}" \
            --spring.main.banner-mode=off >"$log_file" 2>&1 &
    fi
    APP_PID=$!
    wait_for_app
}

start_app baseline
./scripts/measure-demo-latency.sh \
    "$ENDPOINT" "$WARMUP_REQUESTS" "$MEASURED_REQUESTS" \
    "$OUTPUT_DIR/baseline-samples-ms.txt" >"$OUTPUT_DIR/baseline.json"
stop_app

start_app agent
./scripts/measure-demo-latency.sh \
    "$ENDPOINT" "$WARMUP_REQUESTS" "$MEASURED_REQUESTS" \
    "$OUTPUT_DIR/agent-samples-ms.txt" >"$OUTPUT_DIR/agent.json"
stop_app

python3 - "$OUTPUT_DIR/baseline.json" "$OUTPUT_DIR/agent.json" "$OUTPUT_DIR/comparison.json" <<'PY'
import json, sys
from pathlib import Path

baseline = json.loads(Path(sys.argv[1]).read_text())
agent = json.loads(Path(sys.argv[2]).read_text())
metrics = ["averageMs", "p50Ms", "p95Ms", "p99Ms", "minMs", "maxMs"]
comparison = {"baseline": baseline, "agent": agent, "relativeChangePercent": {}}
for metric in metrics:
    before = baseline[metric]
    after = agent[metric]
    comparison["relativeChangePercent"][metric] = round((after - before) / before * 100, 2) if before else None
Path(sys.argv[3]).write_text(json.dumps(comparison, indent=2) + "\n")
PY

{
    printf 'measuredAtUtc=%s\n' "$DATE_UTC"
    printf 'commitSha=%s\n' "$COMMIT_SHA"
    printf 'endpoint=%s\n' "$ENDPOINT"
    printf 'warmupRequests=%s\n' "$WARMUP_REQUESTS"
    printf 'measuredRequests=%s\n' "$MEASURED_REQUESTS"
    printf 'percentileMethod=nearest-rank\n'
    if command -v sw_vers >/dev/null 2>&1; then
        printf 'os=%s %s (%s)\n' \
            "$(sw_vers -productName)" \
            "$(sw_vers -productVersion)" \
            "$(sw_vers -buildVersion)"
    else
        printf 'os=%s\n' "$(uname -s)"
    fi
    printf 'kernel=%s\n' "$(uname -srm)"
    printf 'cpu=%s\n' "$(sysctl -n machdep.cpu.brand_string 2>/dev/null || lscpu 2>/dev/null | sed -n 's/^Model name:[[:space:]]*//p' | head -n 1 || true)"
    printf 'memoryBytes=%s\n' "$(sysctl -n hw.memsize 2>/dev/null || awk '/MemTotal/{print $2*1024}' /proc/meminfo 2>/dev/null || true)"
    java -version 2>&1 | sed 's/^/java=/'
} >"$OUTPUT_DIR/environment.txt"

echo "Performance evidence written to $OUTPUT_DIR"
cat "$OUTPUT_DIR/comparison.json"
