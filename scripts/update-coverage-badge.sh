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

# Extract statement-rate from Scoverage XML report
XML_REPORT="out/fdswarm/scoverage/xmlReport.dest/scoverage.xml"

if [[ ! -f "$XML_REPORT" ]]; then
  echo "Error: Scoverage XML report not found at $XML_REPORT"
  exit 1
fi

# Use grep and sed to extract the statement-rate from the first <scoverage> tag
COVERAGE=$(grep -oE 'statement-rate="[^"]+"' "$XML_REPORT" | head -1 | cut -d'"' -f2)

if [[ -z "$COVERAGE" ]]; then
  echo "Error: Could not extract statement-rate from $XML_REPORT"
  exit 1
fi

# Format coverage to 1 decimal place if it's not an integer
COVERAGE_DISPLAY=$(printf "%.1f" "$COVERAGE")
# If it's something like 18.9, the badge will show 18.9%

echo "Extracted coverage: $COVERAGE_DISPLAY%"

# Determine color based on coverage
COLOR="red"
if (( $(awk -v n="$COVERAGE" 'BEGIN{print (n > 80)}' 2>/dev/null) )); then
  COLOR="brightgreen"
elif (( $(awk -v n="$COVERAGE" 'BEGIN{print (n > 60)}' 2>/dev/null) )); then
  COLOR="green"
elif (( $(awk -v n="$COVERAGE" 'BEGIN{print (n > 40)}' 2>/dev/null) )); then
  COLOR="yellowgreen"
elif (( $(awk -v n="$COVERAGE" 'BEGIN{print (n > 20)}' 2>/dev/null) )); then
  COLOR="yellow"
fi

# Update README.md
# Old badge: ![Coverage](https://img.shields.io/badge/coverage-85%25-brightgreen)
# New badge pattern: ![Coverage](https://img.shields.io/badge/coverage-VALUE%25-COLOR)

# We use | as a delimiter for sed because the URL contains /
# Escaping the % with %25 for the shields.io URL
NEW_BADGE="![Coverage](https://img.shields.io/badge/coverage-${COVERAGE_DISPLAY}%25-${COLOR})"
sed -i.bak -E "s|!\[Coverage\]\(https://img.shields.io/badge/coverage-[0-9.]+%25-[a-z]+\)|${NEW_BADGE}|" README.md

echo "Updated README.md with new coverage badge: $NEW_BADGE"
rm README.md.bak
