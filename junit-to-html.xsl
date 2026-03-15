<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright (c) 2026. Dick Lieber, WA9NNN
  ~
  ~ This program is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ This program is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with this program.  If not, see <http://www.gnu.org/licenses/>.
  ~
  -->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="html" indent="yes" encoding="UTF-8"/>

    <xsl:template match="/">
        <html>
            <head>
                <title>Test Report</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 2rem; }
                    h1, h2 { margin-bottom: 0.5rem; }
                    table { border-collapse: collapse; width: 100%; margin-top: 1rem; }
                    th, td { border: 1px solid #ccc; padding: 0.5rem; text-align: left; }
                    th { background: #f5f5f5; }
                    .pass { color: green; font-weight: bold; }
                    .fail { color: red; font-weight: bold; }
                    .skip { color: #aa7700; font-weight: bold; }
                    pre { white-space: pre-wrap; margin: 0; }
                </style>
            </head>
            <body>
                <h1>Test Report</h1>

                <xsl:for-each select="testsuites | testsuite">
                    <xsl:choose>
                        <xsl:when test="self::testsuites">
                            <h2>Summary</h2>
                            <p>
                                Tests: <xsl:value-of select="@tests"/>
                                |
                                Failures: <xsl:value-of select="@failures"/>
                                |
                                Errors: <xsl:value-of select="@errors"/>
                                |
                                Skipped: <xsl:value-of select="@skipped"/>
                                |
                                Time: <xsl:value-of select="@time"/>s
                            </p>

                            <xsl:for-each select="testsuite">
                                <h2>
                                    Suite:
                                    <xsl:value-of select="@name"/>
                                </h2>
                                <table>
                                    <tr>
                                        <th>Test Case</th>
                                        <th>Class</th>
                                        <th>Time</th>
                                        <th>Status</th>
                                        <th>Details</th>
                                    </tr>
                                    <xsl:apply-templates select="testcase"/>
                                </table>
                            </xsl:for-each>
                        </xsl:when>

                        <xsl:otherwise>
                            <h2>Suite: <xsl:value-of select="@name"/></h2>
                            <p>
                                Tests: <xsl:value-of select="@tests"/>
                                |
                                Failures: <xsl:value-of select="@failures"/>
                                |
                                Errors: <xsl:value-of select="@errors"/>
                                |
                                Skipped: <xsl:value-of select="@skipped"/>
                                |
                                Time: <xsl:value-of select="@time"/>s
                            </p>

                            <table>
                                <tr>
                                    <th>Test Case</th>
                                    <th>Class</th>
                                    <th>Time</th>
                                    <th>Status</th>
                                    <th>Details</th>
                                </tr>
                                <xsl:apply-templates select="testcase"/>
                            </table>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>

            </body>
        </html>
    </xsl:template>

    <xsl:template match="testcase">
        <tr>
            <td><xsl:value-of select="@name"/></td>
            <td><xsl:value-of select="@classname"/></td>
            <td><xsl:value-of select="@time"/></td>
            <td>
                <xsl:choose>
                    <xsl:when test="failure or error">
                        <span class="fail">FAIL</span>
                    </xsl:when>
                    <xsl:when test="skipped">
                        <span class="skip">SKIPPED</span>
                    </xsl:when>
                    <xsl:otherwise>
                        <span class="pass">PASS</span>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <td>
                <xsl:if test="failure">
                    <pre><xsl:value-of select="failure"/></pre>
                </xsl:if>
                <xsl:if test="error">
                    <pre><xsl:value-of select="error"/></pre>
                </xsl:if>
                <xsl:if test="skipped">
                    <pre><xsl:value-of select="skipped"/></pre>
                </xsl:if>
            </td>
        </tr>
    </xsl:template>

</xsl:stylesheet>