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
import io.circe.syntax.*
import io.circe.parser.decode
import fdswarm.fx.contest.ContestType

import java.time.Instant
import fdswarm.fx.qso.FdHour
import fdswarm.util.NodeIdentity

class QsoCirceTest extends FunSuite:

  test("Qso round trip via Circe"):
    val station = Station(operator = Callsign("WA9NNN"), rig = "FT-891", antenna = "End Fed")
    val qsoMetadata = QsoMetadata(station = station, node = NodeIdentity(), contest = ContestType.WFD)
    val bandMode = BandMode("40M", "CW")
    val qso = Qso(
      callsign = Callsign("K1ABC"),
      exchange = Exchange(FdClass("1A"), "CT"),
      bandMode = bandMode,
      qsoMetadata = qsoMetadata,
      stamp = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
      uuid = "unique-id-123"
    )

    // Encode to JSON
    val json = qso.asJson.noSpaces
    
    // Simple check that it is NOT a long number (no quotes) or a formatted date string
    // Base64 of a Long (8 bytes) will be 11 characters.
    // Let's just print it for debugging and check it doesn't look like a number or ISO string
    // println(s"[DEBUG_LOG] JSON: $json")
    val stampPart = "\"stamp\":\""
    assert(json.contains(stampPart), s"JSON should contain $stampPart - actually: $json")
    
    // Decode back to Qso
    val decoded = decode[Qso](json)
    
    assert(decoded.isRight, s"Failed to decode: ${decoded.left.map(_.getMessage)}")
    assertEquals(decoded.toOption.get, qso)

  test("FdHour Circe round trip"):
    val fdHour = FdHour(15, 23)
    val json = fdHour.asJson.noSpaces
    val decoded = decode[FdHour](json).getOrElse(fail("failed to decode"))
    assertEquals(decoded, fdHour)

  test("Callsign Circe round trip"):
    val callsign = Callsign("wa9nnn")
    val json = callsign.asJson.noSpaces
    assertEquals(json, "\"WA9NNN\"")
    val decoded = decode[Callsign](json).getOrElse(fail("failed to decode"))
    assertEquals(decoded, Callsign("WA9NNN"))

  test("BandMode Circe round trip"):
    val bandMode = BandMode("20M", "PH")
    val json = bandMode.asJson.noSpaces
    val decoded = decode[BandMode](json).getOrElse(fail("failed to decode"))
    assertEquals(decoded, bandMode)

  test("QsoMetadata Circe round trip"):
    val station = Station(operator = Callsign("N9VTB"), rig = "IC-7300", antenna = "Dipole")
    val metadata = QsoMetadata(station = station, node = NodeIdentity(), contest = ContestType.ARRL)
    val json = metadata.asJson.noSpaces
    val decoded = decode[QsoMetadata](json).getOrElse(fail("failed to decode"))
    assertEquals(decoded, metadata)
