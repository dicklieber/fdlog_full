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
