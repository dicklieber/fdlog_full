#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MILL_CMD="${MILL_CMD:-./mill}"
TARGET="${1:-fdswarm.compile}"

echo "[mill-safe] workspace: $ROOT_DIR"
echo "[mill-safe] target: $TARGET"

echo "[mill-safe] stopping likely hung mill/test processes (best effort)"
pkill -f "$ROOT_DIR/./mill" 2>/dev/null || true
pkill -f "mill.*fdlog_full" 2>/dev/null || true
pkill -f "java.*mill.*fdlog_full" 2>/dev/null || true

# Wait briefly for process shutdown to release file locks.
sleep 1

echo "[mill-safe] removing stale lock/daemon state"
rm -rf "$ROOT_DIR/out/mill-daemon"
rm -f "$ROOT_DIR/out/mill-out-lock"
rm -f "$ROOT_DIR/out/.mill-lock"

echo "[mill-safe] running: $MILL_CMD -i $TARGET"
exec "$MILL_CMD" -i "$TARGET"
