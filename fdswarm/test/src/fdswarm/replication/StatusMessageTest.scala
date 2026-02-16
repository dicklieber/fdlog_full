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

import fdswarm.store.FdHourDigest
import fdswarm.util.HostAndPort
import fdswarm.fx.qso.FdHour
import upickle.default.*
import munit.FunSuite
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

class StatusMessageTest extends FunSuite:
  test("toPacket should serialize to JSON and gzip") {
    val hp = HostAndPort("localhost", 8080)
    val digests = Seq(FdHourDigest(FdHour(15, 12), 10, "digest-abc"))
    val sm = StatusMessage(hp, digests)
    
    val packet = sm.toPacket
    
    // Decompress
    val bais = new ByteArrayInputStream(packet)
    val gzis = new GZIPInputStream(bais)
    val json = new String(gzis.readAllBytes(), "UTF-8")
    
    // Deserialize
    val readSm = read[StatusMessage](json)
    
    assertEquals(readSm, sm)
    assertEquals(readSm.hostAndPort, hp)
    assertEquals(readSm.fdDigests.size, 1)
    assertEquals(readSm.fdDigests.head.count, 10)
  }

  test("fromPacket should deserialize from gzipped packet") {
    val hp = HostAndPort("localhost", 8080)
    val digests = Seq(FdHourDigest(FdHour(15, 12), 10, "digest-abc"))
    val sm = StatusMessage(hp, digests)
    
    val packet = sm.toPacket
    val readSm = StatusMessage.apply(packet)
    
    assertEquals(readSm, sm)
  }

  test("fromPacket should throw if not a gzip") {
    intercept[java.io.IOException] {
      StatusMessage.apply("not a gzip".getBytes("UTF-8"))
    }
  }
