#!/usr/bin/env bash
set -euo pipefail

./mill fdswarm.assembly + manager.run "$@"
