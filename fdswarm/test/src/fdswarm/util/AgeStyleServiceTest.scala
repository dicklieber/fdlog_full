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

import com.typesafe.config.ConfigFactory
import munit.FunSuite
import java.time.Instant

class AgeStyleServiceTest extends FunSuite:

  test("AgeStyleService should load and calculate styles"):
    val configStr =
      """
        |fdswarm {
        |  ageStyles {
        |    testStyle {
        |      thresholds = [
        |        { duration = 10.0, style = "fresh" }
        |        { duration = 60.0, style = "recent" }
        |        { duration = 120.0, style = "stale" }
        |      ]
        |    }
        |  }
        |}
      """.stripMargin
    val config = ConfigFactory.parseString(configStr)
    val service = new AgeStyleService(config)

    val now = Instant.now()
    
    // 5 seconds ago -> fresh
    assertEquals(service.calc("testStyle", now.minusSeconds(5)).style, "fresh")
    
    // 30 seconds ago -> recent
    assertEquals(service.calc("testStyle", now.minusSeconds(30)).style, "recent")
    
    // 2 minutes ago -> stale
    assertEquals(service.calc("testStyle", now.minusSeconds(120)).style, "stale")

  test("AgeStyleService should throw exception for unknown style"):
    val config = ConfigFactory.parseString("fdswarm { ageStyles {} }")
    val service = new AgeStyleService(config)
    intercept[IllegalArgumentException] {
      service.calc("unknown", Instant.now())
    }
