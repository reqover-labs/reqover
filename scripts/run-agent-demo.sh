#!/usr/bin/env bash
set -euo pipefail

APP="${1:-mvc}"
PORT="${2:-8080}"
STOP_AFTER_REPORT="${3:-}"

if [[ "$APP" != "mvc" && "$APP" != "webflux" ]]; then
    echo "usage: $0 [mvc|webflux] [port] [--stop-after-report]" >&2
    exit 1
fi

if [[ ! "$PORT" =~ ^[0-9]+$ ]] || (( PORT < 1 || PORT > 65535 )); then
    echo "port must be an integer from 1 to 65535" >&2
    exit 1
fi

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

# Read the version the build stamps into the artifact names so a release bump
# does not have to be repeated in this script.
REQOVER_VERSION="${REQOVER_VERSION:-$(awk -F'"' '/^[[:space:]]*version = "/ { print $2; exit }' build.gradle.kts)}"
if [[ -z "$REQOVER_VERSION" ]]; then
    echo "could not read the project version from build.gradle.kts" >&2
    exit 1
fi

./gradlew :reqover-agent:shadowJar ":examples:${APP}-sample:bootJar"

AGENT_JAR="reqover-agent/build/libs/reqover-agent-${REQOVER_VERSION}.jar"
if [[ "$APP" == "mvc" ]]; then
    APP_JAR="examples/mvc-sample/build/libs/mvc-sample-${REQOVER_VERSION}.jar"
    INCLUDE="io.reqover.example.mvc.auto"
    ENDPOINT="http://127.0.0.1:${PORT}/auto/orders/42"
else
    APP_JAR="examples/webflux-sample/build/libs/webflux-sample-${REQOVER_VERSION}.jar"
    INCLUDE="io.reqover.example.webflux.auto"
    ENDPOINT="http://127.0.0.1:${PORT}/auto/reactive/orders/42"
fi

if (: >"/dev/tcp/127.0.0.1/${PORT}") 2>/dev/null; then
    echo "port ${PORT} is already in use; stop the existing process or choose another port" >&2
    exit 1
fi

java "-javaagent:${AGENT_JAR}=include=${INCLUDE}" \
    -jar "$APP_JAR" \
    --server.address=127.0.0.1 \
    "--server.port=${PORT}" \
    --spring.main.banner-mode=off &
APP_PID=$!
trap 'kill "$APP_PID" 2>/dev/null || true' EXIT

REPORT_URL="http://127.0.0.1:${PORT}/reqover/report"
READY=0
for _ in $(seq 1 90); do
    if ! kill -0 "$APP_PID" 2>/dev/null; then
        APP_STATUS=0
        wait "$APP_PID" || APP_STATUS=$?
        echo "sample application exited before it became ready (status ${APP_STATUS})" >&2
        exit 1
    fi
    if curl -sf --max-time 2 "$REPORT_URL" >/dev/null 2>&1; then
        # A stale server on the same port must not be mistaken for this child.
        sleep 0.5
        if ! kill -0 "$APP_PID" 2>/dev/null; then
            APP_STATUS=0
            wait "$APP_PID" || APP_STATUS=$?
            echo "sample application exited after the readiness probe (status ${APP_STATUS})" >&2
            exit 1
        fi
        READY=1
        break
    fi
    sleep 0.5
done

if [[ "$READY" -ne 1 ]]; then
    echo "sample application did not become ready within 45 seconds" >&2
    exit 1
fi

if ! kill -0 "$APP_PID" 2>/dev/null; then
    echo "sample application stopped before the demo request" >&2
    exit 1
fi

curl -sf --max-time 5 "$ENDPOINT"
echo
curl -sf --max-time 5 "$REPORT_URL"
echo
echo "HTML report: http://127.0.0.1:${PORT}/reqover/report.html"

if [[ "$STOP_AFTER_REPORT" != "--stop-after-report" ]]; then
    read -r -p "Press Enter to stop the sample application: "
fi
