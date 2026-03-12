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

class CallsignEqualityTest extends FunSuite:
  test("Callsign instances with same value should be equal"):
    val c1 = Callsign("W9NNN")
    val c2 = Callsign("w9nnn")
    assertEquals(c1, c2)
    assertEquals(c1.hashCode(), c2.hashCode())

  test("Callsign instances with different values should not be equal"):
    val c1 = Callsign("W9NNN")
    val c2 = Callsign("K9OR")
    assertNotEquals(c1, c2)
    assertNotEquals(c1.hashCode(), c2.hashCode())

  test("Callsign should work as a key in a Map"):
    val c1 = Callsign("W9NNN")
    val c2 = Callsign("w9nnn")
    val m = Map(c1 -> 1)
    assertEquals(m.get(c2), Some(1))

  test("groupBy should work correctly with Callsign"):
    case class Data(op: Callsign, value: Int)
    val list = List(
      Data(Callsign("W9NNN"), 10),
      Data(Callsign("w9nnn"), 20),
      Data(Callsign("K9OR"), 30)
    )
    val grouped = list.groupBy(_.op)
    assertEquals(grouped.size, 2)
    assertEquals(grouped(Callsign("W9NNN")).size, 2)
    assertEquals(grouped(Callsign("K9OR")).size, 1)
