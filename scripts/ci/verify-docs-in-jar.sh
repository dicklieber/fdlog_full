#!/usr/bin/env bash

set -euo pipefail

jar_path="${1:-out/fdswarm/assembly.dest/fdswarm.jar}"
docs_index="FDSwarmDocs/index.html"

echo "Verifying docs in JAR"
echo "JAR: $jar_path"

test -f "$jar_path"
jar tf "$jar_path" | grep -q "$docs_index" ||
  {
    echo "Error: $jar_path does not contain $docs_index" >&2
    exit 1
  }

echo "Found $docs_index"
