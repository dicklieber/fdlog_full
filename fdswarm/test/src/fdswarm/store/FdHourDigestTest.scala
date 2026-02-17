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

import fdswarm.model.*
import fdswarm.fx.contest.ContestType
import fdswarm.fx.qso.FdHour
import fdswarm.util.Ids
import munit.FunSuite

class FdHourDigestTest extends FunSuite :

  test("FdHourDigest.apply calculates correct digest from QSOs") :
    val fdHour = FdHour(15, 10) // Feb 15, 10:00
    
    
    val station = Station("S1", "Home", Callsign("WA9NNN"))
    val metadata = QsoMetadata(station, "node1", ContestType.WFD)
    
    val qso1 = Qso(
      callsign = Callsign("W1AW"),
      contestClass = "1A",
      section = "CT",
      bandMode = BandMode("20m", "CW"),
      qsoMetadata = metadata,
      uuid = "id-1"
    )
    
    val qso2 = Qso(
      callsign = Callsign("W2AW"),
      contestClass = "2A",
      section = "NY",
      bandMode = BandMode("40m", "SSB"),
      qsoMetadata = metadata,
      uuid = "id-2"
    )

    val digest = FdHourDigest(fdHour, Seq(qso1, qso2))
    
    assertEquals(digest.fdHour, fdHour)
    assertEquals(digest.count, 2)
    
    // Expected digest: MD5 of "id-1id-2" (sorted IDs)
    val expectedInput = "id-1id-2"
    val expectedDigest = java.security.MessageDigest.getInstance("MD5")
      .digest(expectedInput.getBytes("UTF-8"))
      .map("%02x".format(_)).mkString
      
    assertEquals(digest.digest, expectedDigest)
    
    Ids.revertToRandom()


  test("FdHourDigest.apply handles empty QSOs") :
    val fdHour = FdHour(15, 11)
    val digest = FdHourDigest(fdHour, Seq.empty)
    
    assertEquals(digest.count, 0)
    
    // Expected digest for empty string
    val expectedDigest = java.security.MessageDigest.getInstance("MD5")
      .digest("".getBytes("UTF-8"))
      .map("%02x".format(_)).mkString
      
    assertEquals(digest.digest, expectedDigest)

  test("FdHourDigest.apply is order-independent for input QSOs") {
    val fdHour = FdHour(15, 12)
    val station = Station("S1", "Home", Callsign("WA9NNN"))
    val metadata = QsoMetadata(station, "node1", ContestType.WFD)
    
    val qso1 = Qso(Callsign("C1"), "1A", "CT", BandMode("20m", "CW"), metadata, uuid = "a")
    val qso2 = Qso(Callsign("C2"), "1A", "CT", BandMode("20m", "CW"), metadata, uuid = "b")
    
    val digest1 = FdHourDigest(fdHour, Seq(qso1, qso2))
    val digest2 = FdHourDigest(fdHour, Seq(qso2, qso1))
    
    assertEquals(digest1.digest, digest2.digest)
  }
