#!/usr/bin/env bash
#
# Copyright (c) 2026. Dick Lieber, WA9NNN
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
#

set -euo pipefail

PIDS=()

cleanup() {
  echo
  echo "Stopping instances..."
  for pid in "${PIDS[@]:-}"; do
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
    fi
  done

  sleep 1

  for pid in "${PIDS[@]:-}"; do
    if kill -0 "$pid" 2>/dev/null; then
      kill -9 "$pid" 2>/dev/null || true
    fi
  done

  echo "All instances stopped."
}

trap cleanup EXIT INT TERM

read -r -p "How many server nodes? [2]: " NUM_NODES
NUM_NODES=${NUM_NODES:-2}

if ! [[ "$NUM_NODES" =~ ^[0-9]+$ ]] || [ "$NUM_NODES" -lt 1 ]; then
  echo "Please enter a positive integer."
  exit 1
fi

echo "Building fat jar..."
./mill fdswarm.assembly

JAR="out/fdswarm/assembly.dest/fdswarm-all.jar"

if [ ! -f "$JAR" ]; then
  echo "ERROR: fat jar not found: $JAR"
  exit 1
fi

for ((i=0; i<NUM_NODES; i++)); do
  PORT=$((8080 + i))
  LOG_DIR="$HOME/fdswarm/$PORT"

  mkdir -p "$LOG_DIR"

  echo "Starting node $i on port $PORT"
  echo "Logs: $LOG_DIR/stdout.log"


  PORT="$PORT" SHOW_STARTUP="false" java -jar "$JAR" \
      >"$LOG_DIR/stdout.log" \
      2>&1 &

  PID=$!
  PIDS+=("$PID")

  echo "  pid=$PID"
done

echo
echo "Started ${#PIDS[@]} node(s)."
echo "Press any key to stop..."

read -r -n 1

echo