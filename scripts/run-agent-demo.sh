#!/usr/bin/env bash
set -euo pipefail

APP="${1:-mvc}"
PORT="${2:-8080}"
STOP_AFTER_REPORT="${3:-}"

if [[ "$APP" != "mvc" && "$APP" != "webflux" ]]; then
    echo "usage: $0 [mvc|webflux] [port] [--stop-after-report]" >&2
    exit 1
fi

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

./gradlew :reqover-agent:jar ":examples:${APP}-sample:bootJar"

AGENT_JAR="reqover-agent/build/libs/reqover-agent-0.1.0-SNAPSHOT.jar"
if [[ "$APP" == "mvc" ]]; then
    APP_JAR="examples/mvc-sample/build/libs/mvc-sample-0.1.0-SNAPSHOT.jar"
    INCLUDE="io.reqover.example.mvc.auto"
    ENDPOINT="http://localhost:${PORT}/auto/orders/42"
else
    APP_JAR="examples/webflux-sample/build/libs/webflux-sample-0.1.0-SNAPSHOT.jar"
    INCLUDE="io.reqover.example.webflux.auto"
    ENDPOINT="http://localhost:${PORT}/auto/reactive/orders/42"
fi

java "-javaagent:${AGENT_JAR}=include=${INCLUDE}" \
    -jar "$APP_JAR" \
    "--server.port=${PORT}" \
    --spring.main.banner-mode=off &
APP_PID=$!
trap 'kill "$APP_PID" 2>/dev/null || true' EXIT

REPORT_URL="http://localhost:${PORT}/reqover/report"
for _ in $(seq 1 90); do
    if curl -sf --max-time 2 "$REPORT_URL" >/dev/null 2>&1; then
        break
    fi
    sleep 0.5
done

curl -sf --max-time 5 "$ENDPOINT"
echo
curl -sf --max-time 5 "$REPORT_URL"
echo
echo "HTML report: http://localhost:${PORT}/reqover/report.html"

if [[ "$STOP_AFTER_REPORT" != "--stop-after-report" ]]; then
    read -r -p "Press Enter to stop the sample application: "
fi
