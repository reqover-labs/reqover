#!/usr/bin/env bash
set -euo pipefail

# Measures the latency cost of running the sample application under the Reqover
# agent, against the same application without it.
#
# The two modes are measured in alternating order across several rounds, each
# round restarting both JVMs. A single baseline-then-agent run cannot separate
# the agent's cost from cache, JIT, scheduling, and thermal drift that happens
# to move in the same direction; alternating the order and reporting the median
# of the per-round paired differences does.

PORT="${1:-18180}"
WARMUP_REQUESTS="${2:-50}"
MEASURED_REQUESTS="${3:-300}"
OUTPUT_DIR="${4:-}"
ROUNDS="${5:-${REQOVER_BENCHMARK_ROUNDS:-6}}"

if [[ ! "$PORT" =~ ^[0-9]+$ ]] || (( PORT < 1 || PORT > 65535 )); then
    echo "port must be an integer from 1 to 65535" >&2
    exit 1
fi

if [[ ! "$ROUNDS" =~ ^[1-9][0-9]*$ ]]; then
    echo "rounds must be a positive integer" >&2
    exit 1
fi

if (( ROUNDS < 2 )); then
    echo "warning: fewer than 2 rounds cannot cancel run-order effects; the result is a smoke check, not an overhead measurement" >&2
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

# Read the version the build stamps into the artifact names so a release bump
# does not have to be repeated in this script.
REQOVER_VERSION="${REQOVER_VERSION:-$(awk -F'"' '/^[[:space:]]*version = "/ { print $2; exit }' build.gradle.kts)}"
if [[ -z "$REQOVER_VERSION" ]]; then
    echo "could not read the project version from build.gradle.kts" >&2
    exit 1
fi

AGENT_JAR="reqover-agent/build/libs/reqover-agent-${REQOVER_VERSION}.jar"
APP_JAR="examples/mvc-sample/build/libs/mvc-sample-${REQOVER_VERSION}.jar"
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
    local log_file="$2"
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

# One JVM lifetime: start, warm up, measure, stop. Every round pays the same
# start-up and warm-up cost in both modes, so neither inherits the other's
# warmed JIT state.
measure_mode() {
    local mode="$1"
    local round="$2"
    local round_dir="$OUTPUT_DIR/rounds/round-$(printf '%02d' "$round")"
    mkdir -p "$round_dir"
    start_app "$mode" "$round_dir/${mode}-server.log"
    ./scripts/measure-demo-latency.sh \
        "$ENDPOINT" "$WARMUP_REQUESTS" "$MEASURED_REQUESTS" \
        "$round_dir/${mode}-samples-ms.txt" >"$round_dir/${mode}.json"
    stop_app
}

for (( round = 1; round <= ROUNDS; round++ )); do
    if (( round % 2 == 1 )); then
        order="baseline agent"
    else
        order="agent baseline"
    fi
    echo "round ${round}/${ROUNDS} (order: ${order})"
    for mode in $order; do
        measure_mode "$mode" "$round"
    done
done

python3 - "$OUTPUT_DIR" "$ROUNDS" <<'PY'
import json
import statistics
import sys
from pathlib import Path

output_dir = Path(sys.argv[1])
rounds = int(sys.argv[2])
metrics = ["averageMs", "p50Ms", "p95Ms", "p99Ms", "minMs", "maxMs"]


def nearest_rank(sorted_samples, percent):
    rank = -(-len(sorted_samples) * percent // 100)
    return sorted_samples[rank - 1]


def summarize(samples):
    ordered = sorted(samples)
    return {
        "measuredRequests": len(ordered),
        "averageMs": round(statistics.fmean(ordered), 3),
        "p50Ms": nearest_rank(ordered, 50),
        "p95Ms": nearest_rank(ordered, 95),
        "p99Ms": nearest_rank(ordered, 99),
        "minMs": ordered[0],
        "maxMs": ordered[-1],
    }


per_round = []
pooled = {"baseline": [], "agent": []}

for round_number in range(1, rounds + 1):
    round_dir = output_dir / "rounds" / f"round-{round_number:02d}"
    entry = {"round": round_number, "orderFirst": "baseline" if round_number % 2 else "agent"}
    for mode in ("baseline", "agent"):
        entry[mode] = json.loads((round_dir / f"{mode}.json").read_text())
        samples = [
            float(line)
            for line in (round_dir / f"{mode}-samples-ms.txt").read_text().splitlines()
            if line.strip()
        ]
        pooled[mode].extend(samples)
    # The paired difference within a round is the number that survives drift:
    # both modes saw the same machine state minutes apart, in an order that
    # flips every round.
    entry["deltaMs"] = {
        metric: round(float(entry["agent"][metric]) - float(entry["baseline"][metric]), 3)
        for metric in metrics
    }
    per_round.append(entry)

paired = {}
for metric in metrics:
    deltas = [entry["deltaMs"][metric] for entry in per_round]
    baselines = [float(entry["baseline"][metric]) for entry in per_round]
    paired[metric] = {
        "medianDeltaMs": round(statistics.median(deltas), 3),
        "minDeltaMs": round(min(deltas), 3),
        "maxDeltaMs": round(max(deltas), 3),
        "medianBaselineMs": round(statistics.median(baselines), 3),
        "medianDeltaPercent": (
            round(statistics.median(deltas) / statistics.median(baselines) * 100, 2)
            if statistics.median(baselines)
            else None
        ),
        "roundsWhereAgentSlower": sum(1 for delta in deltas if delta > 0),
        "rounds": len(deltas),
    }

report = {
    "method": {
        "rounds": rounds,
        "order": "alternating; odd rounds run baseline first, even rounds run agent first",
        "jvmRestartsPerMode": rounds,
        "headline": "medianDeltaMs of p50Ms across rounds",
        "percentileMethod": "nearest-rank",
    },
    "pairedComparison": paired,
    "pooled": {mode: summarize(samples) for mode, samples in pooled.items()},
    "perRound": per_round,
}
(output_dir / "comparison.json").write_text(json.dumps(report, indent=2) + "\n")

p50 = paired["p50Ms"]
print(
    f"median per-round p50 delta: {p50['medianDeltaMs']:+.3f} ms "
    f"({p50['medianDeltaPercent']:+.2f}% of a {p50['medianBaselineMs']:.3f} ms baseline), "
    f"agent slower in {p50['roundsWhereAgentSlower']}/{p50['rounds']} rounds"
)
PY

{
    printf 'measuredAtUtc=%s\n' "$DATE_UTC"
    printf 'commitSha=%s\n' "$COMMIT_SHA"
    printf 'endpoint=%s\n' "$ENDPOINT"
    printf 'rounds=%s\n' "$ROUNDS"
    printf 'roundOrder=alternating\n'
    printf 'warmupRequestsPerRoundPerMode=%s\n' "$WARMUP_REQUESTS"
    printf 'measuredRequestsPerRoundPerMode=%s\n' "$MEASURED_REQUESTS"
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
