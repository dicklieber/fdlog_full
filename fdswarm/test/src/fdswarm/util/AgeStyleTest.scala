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
import java.time.{Duration, Instant}

class AgeStyleTest extends FunSuite:

  test("AgeStyle should return correct style based on age"):
    val ageStyle = new AgeStyle(
      (Duration.ofSeconds(10), "fresh"),
      (Duration.ofSeconds(60), "recent")
    )(
      "stale"
    )

    val now = Instant.now()

    // 5 seconds ago -> fresh
    assertEquals(ageStyle.calc(now.minusSeconds(5), now).style, "fresh")

    // 10 seconds ago -> fresh (inclusive boundary)
    assertEquals(ageStyle.calc(now.minusSeconds(10), now).style, "fresh")

    // 15 seconds ago -> recent
    assertEquals(ageStyle.calc(now.minusSeconds(15), now).style, "recent")

    // 60 seconds ago -> recent
    assertEquals(ageStyle.calc(now.minusSeconds(60), now).style, "recent")

    // 65 seconds ago -> stale
    assertEquals(ageStyle.calc(now.minusSeconds(65), now).style, "stale")
    assertEquals(ageStyle.calc(now.minusSeconds(65), now).needsPurge, false)

  test("AgeStyle should handle unsorted thresholds"):
    val ageStyle = new AgeStyle(
      (Duration.ofSeconds(60), "recent"),
      (Duration.ofSeconds(10), "fresh")
    )(
      "stale"
    )

    val now = Instant.now()
    assertEquals(ageStyle.calc(now.minusSeconds(5), now).style, "fresh")
    assertEquals(ageStyle.calc(now.minusSeconds(15), now).style, "recent")
    assertEquals(ageStyle.calc(now.minusSeconds(65), now).style, "stale")

  test("AgeStyle should set needsPurge when purgeAfter is reached"):
    val ageStyle = new AgeStyle(
      (Duration.ofSeconds(10), "fresh")
    )(
      olderStyle = "stale",
      purgeAfter = Some(Duration.ofSeconds(30))
    )
    val now = Instant.now()
    assertEquals(ageStyle.calc(now.minusSeconds(29), now).needsPurge, false)
    assertEquals(ageStyle.calc(now.minusSeconds(30), now).needsPurge, true)
