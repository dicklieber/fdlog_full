#!/bin/bash

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

# Prompt for howManyNodes, default to 2
read -p "How many nodes? [2]: " howManyNodes
howManyNodes=${howManyNodes:-2}

# Pre-compile to avoid multiple nodes trying to compile at once
echo "Pre-compiling..."
./mill fdswarm.compile

# Run each node
for (( i=0; i<howManyNodes; i++ ))
do
    PORT=$((8080 + i))
    echo "Starting node $i on port $PORT..."
    MILL_OUTPUT_DIR=out-$PORT PORT=$PORT ./mill --no-server fdswarm.run &
    sleep 1
done

wait
