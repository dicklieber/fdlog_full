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

import munit.FunSuite
import io.circe.syntax.*
import io.circe.parser.decode
import sttp.tapir.Schema

class FdClassTest extends FunSuite:

  test("FdClass toString should format correctly"):
    assertEquals(FdClass(1, 'A').toString, "1A")
    assertEquals(FdClass(10, 'F').toString, "10F")
    assertEquals(FdClass(2, 'H').toString, "2H")

  test("FdClass apply(String) should parse correctly"):
    assertEquals(FdClass("1A"), FdClass(1, 'A'))
    assertEquals(FdClass("10F"), FdClass(10, 'F'))
    assertEquals(FdClass("2H"), FdClass(2, 'H'))

  test("FdClass apply(String) should throw IllegalArgumentException for invalid input"):
    intercept[IllegalArgumentException] {
      FdClass("A1")
    }
    intercept[IllegalArgumentException] {
      FdClass("1")
    }
    intercept[IllegalArgumentException] {
      FdClass("AA")
    }
    intercept[IllegalArgumentException] {
      FdClass("")
    }

  test("FdClass apply(Int, StationClass) should work correctly"):
    val sc = StationClass('A', "Club or group portable")
    assertEquals(FdClass(3, sc), FdClass(3, 'A'))

  test("FdClass Circe round trip"):
    val fdClass = FdClass(2, 'B')
    val json = fdClass.asJson.noSpaces
    assertEquals(json, "\"2B\"")
    val decoded = decode[FdClass](json).getOrElse(fail("failed to decode"))
    assertEquals(decoded, fdClass)

  test("FdClass Tapir schema should be a string"):
    val schema = summon[Schema[FdClass]]
    assertEquals(schema, Schema.string)

  test("Conversion from String to FdClass should work"):
    val fdClass: FdClass = "3C"
    assertEquals(fdClass, FdClass(3, 'C'))
