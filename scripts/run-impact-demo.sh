#!/usr/bin/env bash
set -euo pipefail

# The whole CI loop in one run: record traffic against the demo application,
# export the report to a file on shutdown, then ask the CLI which endpoints a
# given source change would affect.
#
# usage: run-impact-demo.sh [port]

PORT="${1:-8080}"

if [[ ! "$PORT" =~ ^[0-9]+$ ]] || (( PORT < 1 || PORT > 65535 )); then
    echo "port must be an integer from 1 to 65535" >&2
    exit 1
fi

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

REQOVER_VERSION="${REQOVER_VERSION:-$(awk -F'"' '/^[[:space:]]*version = "/ { print $2; exit }' build.gradle.kts)}"
if [[ -z "$REQOVER_VERSION" ]]; then
    echo "could not read the project version from build.gradle.kts" >&2
    exit 1
fi

OUT_DIR="build/reqover-impact-demo"
REPORT_JSON="${OUT_DIR}/report.json"
REPORT_HTML="${OUT_DIR}/report.html"
CHANGED_FILE="examples/mvc-sample/src/main/java/io/reqover/example/mvc/auto/AutoOrderService.java"
EXPECTED_ENDPOINT="GET /auto/orders/{id}"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

./gradlew :reqover-agent:shadowJar :reqover-cli:shadowJar :examples:mvc-sample:bootJar

AGENT_JAR="reqover-agent/build/libs/reqover-agent-${REQOVER_VERSION}.jar"
CLI_JAR="reqover-cli/build/libs/reqover-cli-${REQOVER_VERSION}.jar"
APP_JAR="examples/mvc-sample/build/libs/mvc-sample-${REQOVER_VERSION}.jar"

if (: >"/dev/tcp/127.0.0.1/${PORT}") 2>/dev/null; then
    echo "port ${PORT} is already in use; stop the existing process or choose another port" >&2
    exit 1
fi

echo
echo "== 1. record traffic with the agent attached"
java "-javaagent:${AGENT_JAR}=include=io.reqover.example.mvc" \
    -jar "$APP_JAR" \
    --server.address=127.0.0.1 \
    "--server.port=${PORT}" \
    --spring.main.banner-mode=off \
    "--reqover.report.export.json-path=${REPORT_JSON}" &
APP_PID=$!
trap 'kill "$APP_PID" 2>/dev/null || true' EXIT

READY=0
for _ in $(seq 1 90); do
    if ! kill -0 "$APP_PID" 2>/dev/null; then
        echo "sample application exited before it became ready" >&2
        exit 1
    fi
    if curl -sf --max-time 2 "http://127.0.0.1:${PORT}/reqover/report" >/dev/null 2>&1; then
        READY=1
        break
    fi
    sleep 0.5
done

if [[ "$READY" -ne 1 ]]; then
    echo "sample application did not become ready within 45 seconds" >&2
    exit 1
fi

curl -sf --max-time 5 "http://127.0.0.1:${PORT}/auto/orders/42" >/dev/null
curl -sf --max-time 5 -X POST "http://127.0.0.1:${PORT}/payments" >/dev/null
echo "drove two requests through the application"

echo
echo "== 2. shut down so the report is exported"
kill "$APP_PID"
trap - EXIT
APP_STATUS=0
wait "$APP_PID" || APP_STATUS=$?
if [[ ! -f "$REPORT_JSON" ]]; then
    echo "the application exited (status ${APP_STATUS}) without writing ${REPORT_JSON}" >&2
    exit 1
fi
echo "exported ${REPORT_JSON}"

echo
echo "== 3. which endpoints execute the code in ${CHANGED_FILE}?"
IMPACT_OUTPUT="$(java -jar "$CLI_JAR" impact \
    --report "$REPORT_JSON" \
    --changed "$CHANGED_FILE" \
    --format text)"
echo "$IMPACT_OUTPUT"

if ! grep -qF "$EXPECTED_ENDPOINT" <<<"$IMPACT_OUTPUT"; then
    echo "expected the impact analysis to name ${EXPECTED_ENDPOINT}" >&2
    exit 1
fi

echo
echo "== 4. render the recorded report to a standalone page"
java -jar "$CLI_JAR" render --report "$REPORT_JSON" --out "$REPORT_HTML"

echo
echo "done. open ${REPORT_HTML} to read the report this run recorded."
