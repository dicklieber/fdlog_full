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

package fdswarm.fx.qso

import munit.FunSuite

class FdHourTest extends FunSuite:

  test("FdHour.toString and FdHour.apply(String) roundtrip") {
    val h1 = FdHour(15, 10)
    val s = h1.toString
    assertEquals(s, "15:10")
    val h2 = FdHour(s)
    assertEquals(h1, h2)
  }

  test("FdHour.apply(String) handles different formats") {
    assertEquals(FdHour("5:9"), FdHour(5, 9))
    assertEquals(FdHour("05:09"), FdHour(5, 9))
    assertEquals(FdHour("24:23"), FdHour(24, 23))
  }

  test("FdHour.apply(String) throws exception on invalid input") {
    intercept[IllegalArgumentException] {
      FdHour("invalid")
    }
    intercept[IllegalArgumentException] {
      FdHour("12-34")
    }
    intercept[IllegalArgumentException] {
      FdHour("12:34:56")
    }
  }

  test("FdHour Circe codec roundtrip") {
    import io.circe.syntax.*
    import io.circe.parser.*

    val h1 = FdHour(15, 10)
    val json = h1.asJson
    assertEquals(json.asString, Some("15:10"))

    val h2 = decode[FdHour](json.noSpaces)
    assertEquals(Right(h1), h2)
  }

  test("FdHour uPickle ReadWriter roundtrip") {
    import upickle.default.*

    val h1 = FdHour(15, 10)
    val json = write(h1)
    assertEquals(json, "\"15:10\"")

    val h2 = read[FdHour](json)
    assertEquals(h1, h2)
  }
