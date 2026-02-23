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
import fdswarm.fx.contest.*
import fdswarm.fx.qso.FdHour
import fdswarm.fx.sections.*
import fdswarm.fx.bands.*
import fdswarm.store.*
import fdswarm.replication.*
import fdswarm.util.HostAndPort
import fdswarm.ContestDates
import java.time.*
import java.net.URL

class CirceSupportTest extends FunSuite:

  test("HamBand round trip"):
    val band = HamBand("40m", 7000000L, 7300000L, BandClass.HF, Set(ItuRegion.R2))
    val json = band.asJson.noSpaces
    val decoded = decode[HamBand](json).toOption.get
    assertEquals(decoded, band)

  test("ContestConfig round trip"):
    val now = ZonedDateTime.now(ZoneOffset.UTC)
    val config = ContestConfig(ContestType.WFD, now, now.plusDays(2))
    val json = config.asJson.noSpaces
    val decoded = decode[ContestConfig](json).toOption.get
    // ZonedDateTime might lose some precision or change format slightly, but should be equivalent
    assertEquals(decoded.contest, config.contest)
    assertEquals(decoded.contest.name, "Winter Field Day")
    assert(decoded.start.isEqual(config.start))

  test("ContestType name field"):
    assertEquals(ContestType.WFD.name, "Winter Field Day")
    assertEquals(ContestType.ARRL.name, "ARRL Field Day")

  test("Exchange round trip"):
    val exchange = Exchange(FdClass(2, 'A'), "IL")
    val json = exchange.asJson.noSpaces
    val decoded = decode[Exchange](json).toOption.get
    assertEquals(decoded, exchange)

  test("StatusMessage round trip"):
    val hostAndPort = HostAndPort("localhost", 8080)
    val fdHour = FdHour(15, 23)
    val digest = FdHourDigest(fdHour, 10, "some-digest")
    val status = StatusMessage(hostAndPort, Seq(digest))
    val json = status.asJson.noSpaces
    val decoded = decode[StatusMessage](json).toOption.get
    assertEquals(decoded, status)

  test("Node round trip"):
    val now = ZonedDateTime.now(ZoneOffset.UTC)
    val config = ContestConfig(ContestType.ARRL, now, now.plusDays(2))
    val node = Node(new URL("http://localhost:8080"), config, Callsign("WA9NNN"))
    val json = node.asJson.noSpaces
    val decoded = decode[Node](json).toOption.get
    assertEquals(decoded.url.toString, node.url.toString)
    assertEquals(decoded.ourStation, node.ourStation)

  test("FdHourIds round trip"):
    val fdHour = FdHour(15, 23)
    val ids = FdHourIds(fdHour, Seq("id1", "id2"))
    val json = ids.asJson.noSpaces
    val decoded = decode[FdHourIds](json).toOption.get
    assertEquals(decoded, ids)
