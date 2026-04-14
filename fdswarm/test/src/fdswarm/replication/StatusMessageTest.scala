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

package fdswarm.replication

import fdswarm.model.{BandMode, BandModeOperator, Callsign}
import fdswarm.store.FdHourDigest
import fdswarm.util.NodeIdentity
import fdswarm.fx.contest.{ContestConfig, ContestType}
import fdswarm.fx.qso.FdHour
import io.circe
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import munit.FunSuite

import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.zip.GZIPInputStream

class StatusMessageTest extends FunSuite:
  private val dummyBno = BandModeOperator(Callsign("WA9NNN"), BandMode("40M", "CW"), Instant.parse("2026-03-16T20:11:04Z"))
  private val dummyContestConfig = ContestConfig(ContestType.ARRL,
    Callsign("WA9NNN"),
    1,
    "A",
    "IL",
    stamp = Instant.parse("2026-03-16T20:11:04Z"))

  test("JSON roundtrip"):
    val digests = Seq(FdHourDigest(FdHour(15, 12), 10, "digest-abc"))
    val sm = StatusMessage(
      hashCount = HashCount(),
      hash = digests,
      bandNodeOperator = dummyBno,
      contestConfig = dummyContestConfig
    )
    val fdigestsJson = sm.hash.head.asJson.spaces2
    assertEquals(fdigestsJson, """{
                                 |  "fdHour" : "15:12",
                                 |  "count" : 10,
                                 |  "digest" : "digest-abc"
                                 |}""".stripMargin)
    val sJson = sm.asJson.spaces2
    println(

    )
//    assertEquals(sJson, """""".stripMargin)
    val decoded = decode[StatusMessage](sJson).getOrElse(fail("failed to decode"))
    assertEquals(decoded, sm)
    assertEquals(decoded.hash.size, 1)
    assertEquals(decoded.hash.head.count, 10)

  test("toPacket should serialize to JSON and gzip") {
//    val hp = NodeIdentity("localhost", 8080, name =)
    val digests = Seq(FdHourDigest(FdHour(15, 12), 10, "digest-abc"))
    val sm = StatusMessage(
      hashCount = HashCount(),
      hash = digests,
      bandNodeOperator = dummyBno,
      contestConfig = dummyContestConfig
    )
    
    val packet = sm.toPacket
    
    // Decompress
    val bais = new ByteArrayInputStream(packet)
    val gzis = new GZIPInputStream(bais)
    val json = new String(gzis.readAllBytes(), "UTF-8")

    val value: Either[circe.Error, StatusMessage] = decode[StatusMessage](json)
    // Deserialize
    val readSm = value.toTry.get
    
    assertEquals(readSm, sm)
    assertEquals(readSm.hash.size, 1)
    assertEquals(readSm.hash.head.count, 10)
  }

  test("fromPacket should deserialize from gzipped packet") {
    val digests = Seq(FdHourDigest(FdHour(15, 12), 10, "digest-abc"))
    val sm = StatusMessage(
      hashCount = HashCount(),
      hash = digests,
      bandNodeOperator = dummyBno,
      contestConfig = dummyContestConfig
    )
    
    val packet = sm.toPacket
    val readSm = StatusMessage.apply(packet)
    
    assertEquals(readSm, sm)
  }

  test("fromPacket should throw if not a gzip") {
    intercept[java.io.IOException] {
      StatusMessage.apply("not a gzip".getBytes("UTF-8"))
    }
  }
