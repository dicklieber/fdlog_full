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

import fdswarm.fx.contest.{ContestConfig, ContestType}
import fdswarm.model.{BandMode, BandModeOperator, Callsign}
import io.circe
import io.circe.parser.decode
import munit.FunSuite

import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.zip.GZIPInputStream
import scala.util.Try

class StatusMessageTest extends FunSuite:
  private val dummyBno = BandModeOperator(Callsign("WA9NNN"), BandMode("40M", "CW"), Instant.parse("2026-03-16T20:11:04Z"))
  private val dummyContestConfig = ContestConfig(ContestType.ARRL,
    Callsign("WA9NNN"),
    1,
    "A",
    "IL",
    stamp = Instant.parse("2026-03-16T20:11:04Z"))


  test("gzip round trip") {
//    val hp = NodeIdentity("localhost", 8080, name =)
    val sm = StatusMessage(hashCount = HashCount(), bandNodeOperator = dummyBno, contestConfig = dummyContestConfig,)
    
    val packet = sm.toPacket

    val backAgain = StatusMessage(packet)

    assertEquals(backAgain, sm)
  }

  test("fromPacket should deserialize from gzipped packet") {
    val sm = StatusMessage(hashCount = HashCount(), bandNodeOperator = dummyBno, contestConfig = dummyContestConfig,)
    
    val packet = sm.toPacket
    val readSm = StatusMessage.apply(packet)
    
    assertEquals(readSm, sm)
  }

  test("fromPacket should throw if not a gzip") {
    intercept[java.io.IOException] {
      StatusMessage.apply("not a gzip".getBytes("UTF-8"))
    }
  }
