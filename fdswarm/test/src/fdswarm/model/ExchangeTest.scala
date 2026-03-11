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

class ExchangeTest extends FunSuite:

  test("Exchange toString should format correctly"):
    assertEquals(Exchange(FdClass(1, 'A'), "IL").toString, "1A IL")
    assertEquals(Exchange(FdClass(2, 'H'), "CT").toString, "2H CT")

  test("Exchange apply(String) should parse correctly"):
    assertEquals(Exchange("1A IL"), Exchange(FdClass(1, 'A'), "IL"))
    assertEquals(Exchange("2H CT"), Exchange(FdClass(2, 'H'), "CT"))

  test("Exchange apply(String) should throw IllegalArgumentException for invalid input"):
    intercept[IllegalArgumentException] {
      Exchange("1AIL")
    }
    intercept[IllegalArgumentException] {
      Exchange("1A")
    }
    intercept[IllegalArgumentException] {
      Exchange(" IL")
    }
    intercept[IllegalArgumentException] {
      Exchange("")
    }

  test("Exchange Circe round trip"):
    val exchange = Exchange(FdClass(1, 'A'), "IL")
    val json = exchange.asJson.noSpaces
    assertEquals(json, "\"1A IL\"")
    val decoded = decode[Exchange](json).getOrElse(fail("failed to decode"))
    assertEquals(decoded, exchange)

  test("Exchange Tapir schema should be a string"):
    val schema = summon[Schema[Exchange]]
    assertEquals(schema, Schema.string)
