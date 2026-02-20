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

import fdswarm.TestDirectory
import fdswarm.model.QsoMetadata.testQsoMetadata
import fdswarm.model.{BandMode, Callsign, Qso}
import fdswarm.replication.StatusMessage
import fdswarm.util.HostAndPort
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import munit.FunSuite

import scala.compiletime.uninitialized

class QsoStoreTest extends FunSuite:
  private var testDirectory: TestDirectory = uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    testDirectory = new TestDirectory()

  override def afterEach(context: AfterEach): Unit =
    testDirectory.cleanup()

  test("FdHour initially empty"):
    val registry = new SimpleMeterRegistry()
    val qsoStore = QsoStore(testDirectory, registry)

    assertEquals(qsoStore.digests().isEmpty, true)
    assertEquals(qsoStore.qsoCollection.isEmpty, true)

  test("add 1st QSO"):
    val registry = new SimpleMeterRegistry()
    val qsoStore = QsoStore(testDirectory, registry)

    val qso = Qso(callsign = Callsign("W9NNN"),
      contestClass = "WFD",
      bandMode = BandMode("20m", "CW"),
      section = "IL",
      qsoMetadata = testQsoMetadata
    )
    qsoStore.add(qso)
    assertEquals(qsoStore.digests().size, 1)
    assertEquals(qsoStore.qsoCollection.size, 1)
    val backAgain = qsoStore.get(qso.uuid)
    assertEquals(backAgain.get, qso)

  test("handle corrupt journal line"):
    val registry = new SimpleMeterRegistry()
    val journalFile = testDirectory() / "qsosJournal.json"
    os.write(journalFile, "this is not json\n", createFolders = true)
    
    // This should not throw an exception because of the new error handling
    val qsoStore = QsoStore(testDirectory, registry)
    
    assertEquals(qsoStore.qsoCollection.isEmpty, true)
    assertEquals(qsoStore.digests().isEmpty, true)

  test("qsosForIds should return all qsos for hour if specificQsos is empty"):
    import cats.effect.unsafe.implicits.global
    val registry = new SimpleMeterRegistry()
    val replicationSupport = ReplicationSupport(testDirectory, registry)
    val qso1 = Qso(callsign = Callsign("W9NNN"),
      contestClass = "WFD",
      bandMode = BandMode("20m", "CW"),
      section = "IL",
      qsoMetadata = testQsoMetadata
    )
    val qso2 = Qso(callsign = Callsign("K9OR"),
      contestClass = "WFD",
      bandMode = BandMode("40m", "SSB"),
      section = "IL",
      qsoMetadata = testQsoMetadata
    )
    replicationSupport.add(Seq(qso1, qso2))

    val request = FdHourRequest(qso1.fdHour, Seq.empty)
    val result = replicationSupport.qsosForIds(request).unsafeRunSync()
    assertEquals(result.fdHour, qso1.fdHour)
    assertEquals(result.qsos.toSet, Set(qso1, qso2))

  test("qsosForIds should return only requested qsos"):
    import cats.effect.unsafe.implicits.global
    val registry = new SimpleMeterRegistry()
    val replicationSupport = ReplicationSupport(testDirectory, registry)
    val qso1 = Qso(callsign = Callsign("W9NNN"),
      contestClass = "WFD",
      bandMode = BandMode("20m", "CW"),
      section = "IL",
      qsoMetadata = testQsoMetadata
    )
    val qso2 = Qso(callsign = Callsign("K9OR"),
      contestClass = "WFD",
      bandMode = BandMode("40m", "SSB"),
      section = "IL",
      qsoMetadata = testQsoMetadata
    )
    replicationSupport.add(Seq(qso1, qso2))

    val request = FdHourRequest(qso1.fdHour, Seq(qso1.uuid))
    val result = replicationSupport.qsosForIds(request).unsafeRunSync()
    assertEquals(result.qsos, Seq(qso1))

  test("determineNeeded should return needed FdHourIds"):
    import cats.effect.unsafe.implicits.global
    val registry = new SimpleMeterRegistry()
    val replicationSupport = ReplicationSupport(testDirectory, registry)
    val qso1 = Qso(callsign = Callsign("W9NNN"),
      contestClass = "WFD",
      bandMode = BandMode("20m", "CW"),
      section = "IL",
      qsoMetadata = testQsoMetadata
    )
    replicationSupport.add(qso1)

    // Remote has an extra QSO in the same hour, so digest will differ
    val qso2 = Qso(callsign = Callsign("K9OR"),
      contestClass = "WFD",
      bandMode = BandMode("40m", "SSB"),
      section = "IL",
      qsoMetadata = testQsoMetadata
    )
    // We want to simulate a remote having qso1 and qso2 in the same hour as qso1
    // Actually FdHourDigest is per FdHour.
    val remoteDigest = FdHourDigest(qso1.fdHour, Seq(qso1, qso2))

    val needed = replicationSupport.isFdHourNeeded(remoteDigest)

    assert(needed.isDefined)
    assertEquals(needed.get, qso1.fdHour)
