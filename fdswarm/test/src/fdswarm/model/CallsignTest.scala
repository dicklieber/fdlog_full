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
import io.circe.parser.*
import io.circe.syntax.*

class CallsignTest extends FunSuite:

  test("JSON"):
    val callSign = Callsign("WA9NNN")
    val sJson = callSign.asJson.noSpaces
    assertEquals(sJson, """"WA9NNN"""")
    val backAgain: Callsign = decode[Callsign](sJson).toTry.get

  test("Starts with"):
    assert(Callsign("WA9NNN").startsWith("WA9"))
    assert(!Callsign("WA9NNN").startsWith("K"))

  test("to string"):
    assertEquals(Callsign("WA9NNN").toString, "WA9NNN")

  test("equals"):
    assertEquals(Callsign("WA9NNN"), Callsign("WA9NNN"))
    assertNotEquals(Callsign("WA9NNN"), Callsign("N9VTB"))
    assert(!Callsign("WA9NNN").equals(42))

  test("isValid validates standard callsigns") {
    assert(Callsign.isValid("K1ABC"))
    assert(Callsign.isValid("WA9NNN"))
    assert(Callsign.isValid("G4XYZ"))
    assert(Callsign.isValid("7J1RL"))
    assert(Callsign.isValid("N0ABC"))
  }

  test("isValid rejects invalid callsigns") {
    assert(!Callsign.isValid("")) // Empty
    assert(!Callsign.isValid("K")) // Too short
    assert(!Callsign.isValid("K1ABCDEFG HIJKL")) // Contains space
    assert(!Callsign.isValid("ABCDEFG")) // No digit
    assert(!Callsign.isValid("K1ABC/")) // Trailing slash
    assert(!Callsign.isValid("/K1ABC")) // Leading slash
    assert(!Callsign.isValid("VE3/K1ABC")) // Suffix too long (>4)
  }

  test("isValid respects regex-defined length limits") {
    assert(!Callsign.isValid("ABC0ABCDE")) // 9 chars - too long for main part

    // With suffix: (?=.{3,12}$) restricts total length to 12
    assert(Callsign.isValid("ABC0ABCD/P")) // 10 chars
    assert(Callsign.isValid("ABC0ABCD/123")) // 12 chars - max
    assert(!Callsign.isValid("ABC0ABCD/1234")) // 13 chars - too long

    assert(Callsign.isValid("K1A")) // 3 chars - min
  }

  test("Ordering") {
    val c1 = Callsign("A1A")
    val c2 = Callsign("B1A")
    val c3 = Callsign("A2A")
    val c4 = Callsign("A1A/B")
    
    val list = List(c2, c3, c1, c4)
    val sorted = list.sorted
    
    // Order: number, then prefix, then suffix
    // c1: prefix A, number 1, suffix ""
    // c2: prefix B, number 1, suffix ""
    // c3: prefix A, number 2, suffix ""
    // c4: prefix A, number 1, suffix "B"
    
    // Sorted:
    // 1. number 1: c1 (prefix A, no suffix), c4 (prefix A, suffix B), c2 (prefix B, no suffix)
    //    c1 vs c4: number same, prefix same, suffix "" < "B" -> c1, c4
    //    c1/c4 vs c2: number same, prefix A < B -> c1, c4, c2
    // 2. number 2: c3
    
    assertEquals(sorted, List(c1, c4, c2, c3))
  }
