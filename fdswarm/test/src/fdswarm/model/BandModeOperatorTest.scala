/*
 * Copyright (c) 2026. Dick Lieber, WA9NNN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package fdswarm.model

import io.circe.parser.decode
import io.circe.syntax.*
import munit.FunSuite

import java.time.Instant

class BandModeOperatorTest extends FunSuite:

  test("BandNodeOperator Circe round trip"):
    val operator = Callsign("WA9NNN")
    val bandMode = BandMode("40M", "CW")
    val stamp = Instant.parse("2026-03-16T15:00:00Z")
    val bandNodeOperator = BandModeOperator(operator, bandMode, stamp)
    
    val json = bandNodeOperator.asJson.noSpaces
    val decoded = decode[BandModeOperator](json).getOrElse(fail("failed to decode"))
    assertEquals(decoded, bandNodeOperator)
    assertEquals(decoded.operator, operator)
    assertEquals(decoded.bandMode, bandMode)
    assertEquals(decoded.stamp, stamp)

  test("BandNodeOperator decoding with invalid format should fail"):
    val json = "\"WA9NNN\""
    val result = decode[BandModeOperator](json)
    assert(result.isLeft)
