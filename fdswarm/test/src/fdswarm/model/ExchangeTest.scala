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
import sttp.tapir.Schema

class ExchangeTest extends FunSuite:

  test("Exchange toString should format correctly"):
    assertEquals(Exchange(FdClass(1, 'A'), "IL").toString, "1A IL")
    assertEquals(Exchange(FdClass(10, 'F'), "WI").toString, "10F WI")
    assertEquals(Exchange(FdClass(2, 'H'), "EPA").toString, "2H EPA")

  test("Exchange apply(String) should parse correctly"):
    assertEquals(Exchange("1A IL"), Exchange(FdClass(1, 'A'), "IL"))
    assertEquals(Exchange("10F WI"), Exchange(FdClass(10, 'F'), "WI"))
    assertEquals(Exchange("2H EPA"), Exchange(FdClass(2, 'H'), "EPA"))

  test("Exchange apply(String) should throw IllegalArgumentException for invalid input"):
    intercept[IllegalArgumentException] {
      Exchange("1AIL")
    }
    intercept[IllegalArgumentException] {
      Exchange("1A  IL") // only single space is supported by Parse regex
    }
    intercept[IllegalArgumentException] {
      Exchange("1A")
    }
    intercept[IllegalArgumentException] {
      Exchange("")
    }

  test("Exchange Circe round trip"):
    val exchange = Exchange(FdClass(2, 'B'), "IL")
    val json = exchange.asJson.noSpaces
    assertEquals(json, "\"2B IL\"")
    val decoded = decode[Exchange](json).getOrElse(fail("failed to decode"))
    assertEquals(decoded, exchange)

  test("Exchange Tapir schema should be a string"):
    val schema = summon[Schema[Exchange]]
    assertEquals(schema, Schema.string)

  test("Exchange default constructor should work"):
    val exchange = Exchange()
    assertEquals(exchange.fdClass, FdClass(1, 'I'))
    assertEquals(exchange.sectionCode, "IL")
    assertEquals(exchange.toString, "1I IL")
