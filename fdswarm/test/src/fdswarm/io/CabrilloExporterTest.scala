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

package fdswarm.io

import fdswarm.fx.contest.ContestType
import fdswarm.model.*
import java.time.Instant
import munit.FunSuite

class CabrilloExporterTest extends FunSuite:

  test("exportQsos creates a valid Cabrillo format"):
    val station = Station(rig = "1A", antenna = "Wire", operator = Callsign("W1AW"))
    val qsoMetadata = QsoMetadata(station = station, node = "local", contest = ContestType.WFD)
    
    val qso1 = Qso(
      callsign = Callsign("K1ABC"),
      contestClass = "1O",
      section = "CT",
      bandMode = BandMode("20m CW"), // Using the string apply for BandMode
      qsoMetadata = qsoMetadata,
      stamp = Instant.parse("2026-02-25T14:00:00Z")
    )

    val qsos = Seq(qso1)
    val result = CabrilloExporter.exportQsos(qsos, station, ContestType.WFD)
    
    assert(result.contains("START-OF-LOG: 3.0"))
    assert(result.contains("CALLSIGN: W1AW"))
    assert(result.contains("CONTEST: WFD"))
    // QSO: freq mo date       time mycall       myclass mysect hiscall      hisclass hissect
    // QSO: 14035 CW 2026-02-25 1400 W1AW         1A  XX  K1ABC        1O  CT 
    assert(result.contains("QSO: 14035 CW 2026-02-25 1400 W1AW         1A  XX  K1ABC        1O  CT "))
    assert(result.contains("END-OF-LOG:"))

  test("mapContest maps correctly"):
    val station = Station(operator = Callsign("W1AW"))
    val resultWfd = CabrilloExporter.exportQsos(Seq.empty, station, ContestType.WFD)
    assert(resultWfd.contains("CONTEST: WFD"))
    
    val resultArrl = CabrilloExporter.exportQsos(Seq.empty, station, ContestType.ARRL)
    assert(resultArrl.contains("CONTEST: ARRL-FIELD-DAY"))
