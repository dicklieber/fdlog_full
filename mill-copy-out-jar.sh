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

# Build the assembly jar
./mill fdswarm.assembly

# The assembly JAR name is defined in build.mill as ${jpackageName()}-${version}-assembly.jar
SRC_JAR=$(ls out/fdswarm/assembly.dest/*.jar | head -n 1)

# Default destination: macOS Desktop
DEST_JAR="$HOME/parallels-deploy"

# If a Parallels/Windows UNC path is explicitly provided, use it instead
# Usage: ./mill-copy-out-jar.sh "\\\\Mac\\Home\\Desktop\\out.jar"
if [[ $# -ge 1 ]]; then
  DEST_JAR="$1"
fi

# Copy the jar
cp -f "$SRC_JAR" "$DEST_JAR"

echo "Copied $SRC_JAR -> $DEST_JAR"