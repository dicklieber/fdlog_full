#!/usr/bin/env bash
set -euo pipefail

./mill fdswarm.assembly + manager.assembly

MANAGER_JAR="out/manager/assembly.dest/out.jar"
if [[ ! -f "$MANAGER_JAR" ]]; then
  echo "Could not find manager assembly JAR in out/manager/assembly.dest" >&2
  exit 1
fi

READY_MARKER="MANAGER_START_ALL_COMPLETE"
WAIT_TIMEOUT_SECONDS="${RUN_MANAGER_WAIT_TIMEOUT_SECONDS:-60}"
LOG_FILE="${RUN_MANAGER_LOG_FILE:-$(mktemp -t fdlog-manager.XXXXXX.log)}"

AUTOSTART_ARG="${1:-}"
JAVA_ARGS=()
WAIT_FOR_READY=false
if [[ "$AUTOSTART_ARG" == "startAll" ]]; then
  shift
  JAVA_ARGS+=(--autostart true)
  WAIT_FOR_READY=true
fi

JAVA_ARGS+=("$@")

java -jar "$MANAGER_JAR" "${JAVA_ARGS[@]}" >"$LOG_FILE" 2>&1 &
MANAGER_PID=$!

echo "Manager started in background (pid=$MANAGER_PID)"
echo "Manager log: $LOG_FILE"

if [[ "$WAIT_FOR_READY" == "true" ]]; then
  deadline=$((SECONDS + WAIT_TIMEOUT_SECONDS))
  while true; do
    if grep -Fq "$READY_MARKER" "$LOG_FILE"; then
      echo "Manager reported all instances started."
      exit 0
    fi

    if ! kill -0 "$MANAGER_PID" 2>/dev/null; then
      echo "Manager exited before signaling readiness. Last log lines:" >&2
      tail -n 80 "$LOG_FILE" >&2 || true
      exit 1
    fi

    if (( SECONDS >= deadline )); then
      echo "Timed out waiting (${WAIT_TIMEOUT_SECONDS}s) for readiness marker '$READY_MARKER'." >&2
      tail -n 80 "$LOG_FILE" >&2 || true
      exit 1
    fi

    sleep 0.2
  done
fi
