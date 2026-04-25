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

package fdswarm.store

import fdswarm.model.{BandMode, Callsign}
import munit.FunSuite

class QsoStoreTest extends FunSuite:

  test("sameBandMode matches band and mode case-insensitively"):
    val left = BandMode("20M SSB")
    val right = BandMode("20m SSB")
    assert(QsoStore.sameBandMode(left, right))

  test("sameBandMode does not match different mode"):
    val left = BandMode("20M SSB")
    val right = BandMode("20m CW")
    assert(!QsoStore.sameBandMode(left, right))

  test("potentialDupCallsigns matches prefix case-insensitively across all callsigns"):
    val callsigns = Seq(Callsign("wa9zzz"), Callsign("WA9ABC"), Callsign("K1XYZ"))
    val result = QsoStore.potentialDupCallsigns(callsigns, "wa9")
    assertEquals(result.map(_.value), Seq("WA9ABC", "WA9ZZZ"))

  test("potentialDupCallsigns returns distinct callsigns"):
    val callsigns = Seq(Callsign("WA9ZZZ"), Callsign("wa9zzz"))
    val result = QsoStore.potentialDupCallsigns(callsigns, "WA9")
    assertEquals(result.map(_.value), Seq("WA9ZZZ"))
