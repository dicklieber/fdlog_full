#!/usr/bin/env bash
set -euo pipefail

./mill fdswarm.assembly + manager.assembly

MANAGER_JAR="out/manager/assembly.dest/out.jar"
if [[ ! -f "$MANAGER_JAR" ]]; then
  echo "Could not find manager assembly JAR in out/manager/assembly.dest" >&2
  exit 1
fi

exec java -jar "$MANAGER_JAR" "$@"
