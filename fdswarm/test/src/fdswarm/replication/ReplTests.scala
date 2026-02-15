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

import fdswarm.io.DirectoryProvider
import fdswarm.model.*
import fdswarm.store.QsoStore
import fdswarm.fx.contest.ContestType
import fdswarm.util.Ids

class ReplTests extends munit.FunSuite {

  test("byFdHour returns FdHourDigest with correct count and digest") {
    val tmpDir = os.temp.dir()
    val directoryProvider = new DirectoryProvider {
      override def apply(): os.Path = tmpDir
    }

    val qsoStore = new QsoStore(directoryProvider)
    val nodeStatusReceiverService = new NodeStatusReceiverService(8888, false)
    val repl = new Repl(qsoStore, nodeStatusReceiverService)

    // Use sequential IDs for predictability in test
    Ids.useSeqentialStartingAt(0)

    val qso1 = Qso(
      callSign = Callsign("W1AW"),
      contestClass = "1A",
      section = "CT",
      bandMode = BandMode("20m", "CW"),
      qsoMetadata = QsoMetadata(Station("S1", "Home", Callsign("WA9NNN")), "node1", ContestType.WFD)
    )
    qsoStore.add(qso1)

    val results = repl.byFdHour.toSeq
    assertEquals(results.size, 1)
    val result = results.head
    assertEquals(result.count, 1)
    
    // Calculate expected digest for ID "0"
    val expectedDigest = java.security.MessageDigest.getInstance("SHA-256")
      .digest("0".getBytes("UTF-8"))
      .map("%02x".format(_)).mkString
    
    assertEquals(result.digest, expectedDigest)
    
    Ids.revertToRandom()
  }

  test("byFdHourJsonGzipBase64 returns valid base64-gzipped-json") {
    val tmpDir = os.temp.dir()
    val directoryProvider = new DirectoryProvider {
      override def apply(): os.Path = tmpDir
    }
    val qsoStore = new QsoStore(directoryProvider)
    val nodeStatusReceiverService = new NodeStatusReceiverService(8888, false)
    val repl = new Repl(qsoStore, nodeStatusReceiverService)

    val qso = Qso(
      callSign = Callsign("W1AW"),
      contestClass = "1A",
      section = "CT",
      bandMode = BandMode("20m", "CW"),
      qsoMetadata = QsoMetadata(Station("S1", "Home", Callsign("WA9NNN")), "node1", ContestType.WFD)
    )
    qsoStore.add(qso)

    val encoded = repl.byFdHourJsonGzipBase64
    
    // Decode base64
    val decodedBytes = java.util.Base64.getDecoder.decode(encoded)
    
    // Unzip
    val bais = new java.io.ByteArrayInputStream(decodedBytes)
    val gzis = new java.util.zip.GZIPInputStream(bais)
    val resultJson = new String(gzis.readAllBytes(), "UTF-8")
    
    // Parse JSON
    import upickle.default.*
    val digests = read[Seq[FdHourDigest]](resultJson)
    
    assertEquals(digests.size, 1)
    assertEquals(digests.head.count, 1)
  }

  test("Repl thread processes messages from queue") {
    val tmpDir = os.temp.dir()
    val directoryProvider = new DirectoryProvider {
      override def apply(): os.Path = tmpDir
    }
    val qsoStore = new QsoStore(directoryProvider)
    val nodeStatusReceiverService = new NodeStatusReceiverService(0, false)
    val repl = new Repl(qsoStore, nodeStatusReceiverService)

    repl.start()
    try {
      val message = "Hello Repl"
      nodeStatusReceiverService.queue.offer(message.getBytes("UTF-8"))

      // Since the thread just logs for now, we don't have an easy way to verify it other than it doesn't crash
      // and we can see it taking the message from the queue.
      var timeout = 100 // 1 second
      while (nodeStatusReceiverService.queue.size() > 0 && timeout > 0) {
        Thread.sleep(10)
        timeout -= 1
      }
      assertEquals(nodeStatusReceiverService.queue.size(), 0, "Queue should be empty after Repl thread processes the message")
    } finally {
      repl.stop()
      os.remove.all(tmpDir)
    }
  }

  test("byFdHour groups by FdHour") {
    val tmpDir = os.temp.dir()
    val directoryProvider = new DirectoryProvider {
      override def apply(): os.Path = tmpDir
    }

    val qsoStore = new QsoStore(directoryProvider)
    val nodeStatusReceiverService = new NodeStatusReceiverService(8888, false)
    val repl = new Repl(qsoStore, nodeStatusReceiverService)
    
    val baseInstant = java.time.Instant.parse("2026-02-10T10:00:00Z")
    
    val qso1 = Qso(
      callSign = Callsign("W1AW"),
      contestClass = "1A",
      section = "CT",
      bandMode = BandMode("20m", "CW"),
      qsoMetadata = QsoMetadata(Station("S1", "Home", Callsign("WA9NNN")), "node1", ContestType.WFD),
      stamp = baseInstant
    )
    
    val qso2 = Qso(
      callSign = Callsign("W2AW"),
      contestClass = "1A",
      section = "NY",
      bandMode = BandMode("40m", "SSB"),
      qsoMetadata = QsoMetadata(Station("S1", "Home", Callsign("WA9NNN")), "node1", ContestType.WFD),
      stamp = baseInstant.plusSeconds(3600) // Next hour
    )
    
    qsoStore.add(qso1)
    qsoStore.add(qso2)
    
    val results = repl.byFdHour.toSeq
    assertEquals(results.size, 2)
  }
}
