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

APP_NAME="FdSwarm"
VERSION="1.0.0" # macOS app version must start with a positive integer
MAIN_JAR="fdswarm.jar"
MAIN_CLASS="fdswarm.FdLogApp"

echo "Building JAR..."
./build-jars.sh

echo "Building macOS Application Bundle..."

# Clean up any previous builds
rm -rf out/jpackage

# Create directory for jpackage
rm -rf out/jpackage
mkdir -p out/jpackage/input

# Copy only the JAR to the input directory to avoid recursive copying of 'out' or other unwanted files
cp "$MAIN_JAR" out/jpackage/input/

# Run jpackage to create a native macOS .app bundle
jpackage \
  --type app-image \
  --dest out/jpackage \
  --name "$APP_NAME" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --input out/jpackage/input \
  --app-version "$VERSION" \
  --java-options "-Xdock:name=$APP_NAME" \
  --java-options "-Dapple.awt.application.name=$APP_NAME" \
  --java-options "-Dcom.apple.mrj.application.apple.menu.about.name=$APP_NAME" \
  --java-options "-Dapple.laf.useScreenMenuBar=true" \
  --java-options "-Dapple.awt.application.appearance=system" \
  --mac-package-name "$APP_NAME" \
  --verbose

echo ""
echo "Successfully created native macOS application bundle:"
echo "Location: out/jpackage/$APP_NAME.app"
echo ""
echo "To run the application with the correct menu name, use:"
echo "open out/jpackage/$APP_NAME.app"
echo ""
echo "Note: Running directly from a JAR on macOS often results in the 'java' menu"
echo "due to how the JVM identifies itself to the system. The native bundle"
echo "includes an Info.plist that correctly identifies the application as '$APP_NAME'."
