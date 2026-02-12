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

package fdswarm.util

import munit.FunSuite
import upickle.default.*
import java.time.LocalDateTime
import JavaTimePickle.given

class JavaTimePickleTest extends FunSuite:
  test("LocalDateTime ReadWriter"):
    val ldt = LocalDateTime.of(2026, 2, 4, 7, 42, 0)
    val json = write(ldt)
    assertEquals(json, """"2026-02-04T07:42:00"""")
    val back = read[LocalDateTime](json)
    assertEquals(back, ldt)

  test("ZonedDateTime ReadWriter"):
    import java.time.ZonedDateTime
    import java.time.ZoneOffset
    val zdt = ZonedDateTime.of(2026, 2, 4, 7, 42, 0, 0, ZoneOffset.UTC)
    val json = write(zdt)
    assertEquals(json, """"2026-02-04T07:42:00Z"""")
    val back = read[ZonedDateTime](json)
    assertEquals(back, zdt)

  test("URL ReadWriter"):
    import java.net.URL
    val url = URL("http://127.0.0.1:8080")
    val json = write(url)
    assertEquals(json, """"http://127.0.0.1:8080"""")
    val back = read[URL](json)
    assertEquals(back, url)
