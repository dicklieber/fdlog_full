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

import fdswarm.fx.bands.{BandCatalog, BandModeBuilder, ModeCatalog}
import fdswarm.fx.bandmodes.SelectedBandModeManager
import fdswarm.StationManager
import fdswarm.TestDirectory
import fdswarm.model.QsoMetadata.testQsoMetadata
import fdswarm.model.{BandMode, Callsign, Exchange, Qso}
import fdswarm.replication.status.SwarmStatus
import fdswarm.replication.{Service, StatusMessage, Transport}
import fdswarm.util.{MockNodeIdentityManager, NodeIdentity, NodeIdentityManager}
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import munit.FunSuite

import scala.compiletime.uninitialized
import fdswarm.MockStartupInfo
import fdswarm.fx.discovery.ContestDiscovery

class QsoStoreTest extends FunSuite:
  private var testDirectory: TestDirectory = uninitialized

  class MockTransport extends Transport:
    override val nodeIdentityManager: NodeIdentityManager = MockNodeIdentityManager()
    override val mode: String = "Mock"
    override val queue = new java.util.concurrent.LinkedBlockingQueue[fdswarm.replication.UDPHeaderData]()
    override def addListener(listener: fdswarm.replication.UDPHeaderData => Unit): Unit = ()
    override def removeListener(listener: fdswarm.replication.UDPHeaderData => Unit): Unit = ()
    var sentData: Seq[(Service, Array[Byte])] = Seq.empty
    override def send(data: Array[Byte]): Unit = ()
    override def send(service: Service, data: Array[Byte]): Unit =
      sentData = sentData :+ (service, data)
    override def sentCount: Long = sentData.size
    override def stop(): Unit = ()

  private val mockTransport = new MockTransport()
  private var mockNodeIdentityManager: MockNodeIdentityManager = uninitialized
  private var swarmStatus: SwarmStatus = uninitialized
  private var stationManager: StationManager = uninitialized
  private var selectedBandModeStore: SelectedBandModeManager = uninitialized
  private var contestCatalog: fdswarm.fx.contest.ContestCatalog = uninitialized
  private var sections: fdswarm.fx.sections.Sections = uninitialized
  private var filenameStamp: fdswarm.util.FilenameStamp = uninitialized
  private var contestManager: fdswarm.fx.contest.ContestManager = uninitialized
  private var qsoStore: QsoStore = uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    testDirectory = new TestDirectory()
    mockNodeIdentityManager = MockNodeIdentityManager()
    stationManager = new StationManager(testDirectory, MockStartupInfo)
    val config = com.typesafe.config.ConfigFactory.parseString(
      """
        |fdswarm {
        |  hamBands = [
        |    { bandName = "20m", startFrequencyHz = 14000000, endFrequencyHz = 14350000, bandClass = "HF", regions = ["ALL"] }
        |  ]
        |  modes = ["CW", "PH", "DIGI"]
        |}
        |""".stripMargin)
    val bandCatalog = new BandCatalog(config)
    val modeCatalog = new ModeCatalog(config)
    val bandModeBuilder = new BandModeBuilder(bandCatalog, modeCatalog)
    selectedBandModeStore = new SelectedBandModeManager(testDirectory, bandModeBuilder, MockStartupInfo)
    swarmStatus = SwarmStatus(testDirectory, mockNodeIdentityManager, stationManager, selectedBandModeStore, null)
    contestCatalog = {
      val config = com.typesafe.config.ConfigFactory.parseString(
        """
          |fdswarm.contests = []
          |fdswarm.sections = []
          |""".stripMargin)
      new fdswarm.fx.contest.ContestCatalog(config)
    }
    sections = new fdswarm.fx.sections.Sections(new fdswarm.fx.sections.SectionsProvider(com.typesafe.config.ConfigFactory.parseString(
      """
        |fdswarm.sections = []
        |""".stripMargin)))
    filenameStamp = new fdswarm.util.FilenameStamp(new jakarta.inject.Provider[fdswarm.fx.contest.ContestManager] {
      override def get(): fdswarm.fx.contest.ContestManager = contestManager
    })
    qsoStore = new QsoStore(testDirectory, new SimpleMeterRegistry(), mockTransport, swarmStatus, MockStartupInfo, filenameStamp)
    val discovery = new ContestDiscovery(mockTransport, 1)
    contestManager = new fdswarm.fx.contest.ContestManager(testDirectory, contestCatalog, sections, qsoStore, filenameStamp, mockTransport, discovery, 7)

  override def afterEach(context: AfterEach): Unit =
    testDirectory.cleanup()

  test("FdHour initially empty"):
    assertEquals(qsoStore.digests().isEmpty, true)
    assertEquals(qsoStore.qsoCollection.isEmpty, true)

  test("add 1st QSO"):
    val qso = Qso(callsign = Callsign("W9NNN"),
      exchange = Exchange(fdswarm.model.FdClass("1A"), "IL"),
      bandMode = BandMode("20m", "CW"),
      qsoMetadata = testQsoMetadata
    )
    qsoStore.add(qso)
    assertEquals(qsoStore.digests().size, 1)
    assertEquals(qsoStore.qsoCollection.size, 1)
    val backAgain = qsoStore.get(qso.uuid)
    assertEquals(backAgain.get, qso)

  test("handle corrupt journal line"):
    val journalFile = testDirectory() / "qsosJournal.json"
    os.write(journalFile, "this is not json\n", createFolders = true)
    
    // This should not throw an exception because of the new error handling
    // Re-instantiating with corrupt file
    val qs = new QsoStore(testDirectory, new SimpleMeterRegistry(), mockTransport, swarmStatus, MockStartupInfo, filenameStamp)
    
    assertEquals(qs.qsoCollection.isEmpty, true)
    assertEquals(qs.digests().isEmpty, true)

  test("qsosForIds should return all qsos for hour if specificQsos is empty"):
    import cats.effect.unsafe.implicits.global
    val replicationSupport = ReplicationSupport(testDirectory, new SimpleMeterRegistry(), mockTransport, swarmStatus, MockStartupInfo, filenameStamp)
    val qso1 = Qso(callsign = Callsign("W9NNN"),
      exchange = Exchange(fdswarm.model.FdClass("1A"), "IL"),
      bandMode = BandMode("20m", "CW"),
      qsoMetadata = testQsoMetadata
    )
    val qso2 = Qso(callsign = Callsign("K9OR"),
      exchange = Exchange(fdswarm.model.FdClass("1A"), "IL"),
      bandMode = BandMode("40m", "SSB"),
      qsoMetadata = testQsoMetadata
    )
    replicationSupport.add(Seq(qso1, qso2))

    val request = FdHourRequest(qso1.fdHour, Seq.empty)
    val result = replicationSupport.qsosForIds(request).unsafeRunSync()
    assertEquals(result.fdHour, qso1.fdHour)
    assertEquals(result.qsos.toSet, Set(qso1, qso2))

  test("qsosForIds should return only requested qsos"):
    import cats.effect.unsafe.implicits.global
    val replicationSupport = ReplicationSupport(testDirectory, new SimpleMeterRegistry(), mockTransport, swarmStatus, MockStartupInfo, filenameStamp)
    val qso1 = Qso(callsign = Callsign("W9NNN"),
      exchange = Exchange(fdswarm.model.FdClass("1A"), "IL"),
      bandMode = BandMode("20m", "CW"),
      qsoMetadata = testQsoMetadata
    )
    val qso2 = Qso(callsign = Callsign("K9OR"),
      exchange = Exchange(fdswarm.model.FdClass("1A"), "IL"),
      bandMode = BandMode("40m", "SSB"),
      qsoMetadata = testQsoMetadata
    )
    replicationSupport.add(Seq(qso1, qso2))

    val request = FdHourRequest(qso1.fdHour, Seq(qso1.uuid))
    val result = replicationSupport.qsosForIds(request).unsafeRunSync()
    assertEquals(result.qsos, Seq(qso1))

  test("determineNeeded should return needed FdHourIds"):
    import cats.effect.unsafe.implicits.global
    val replicationSupport = ReplicationSupport(testDirectory, new SimpleMeterRegistry(), mockTransport, swarmStatus, MockStartupInfo, filenameStamp)
    val qso1 = Qso(callsign = Callsign("W9NNN"),
      exchange = Exchange("1A", "IL"),
      bandMode = BandMode("20m", "CW"),
      qsoMetadata = testQsoMetadata
    )
    replicationSupport.add(qso1)

    // Remote has an extra QSO in the same hour, so digest will differ
    val qso2 = Qso(callsign = Callsign("K9OR"),
      exchange = Exchange("1A", "IL"),
      bandMode = BandMode("40m", "SSB"),
      qsoMetadata = testQsoMetadata
    )
    // We want to simulate a remote having qso1 and qso2 in the same hour as qso1
    // Actually FdHourDigest is per FdHour.
    val remoteDigest = FdHourDigest(qso1.fdHour, Seq(qso1, qso2))

    val needed = replicationSupport.isFdHourNeeded(remoteDigest)

    assert(needed.isDefined)
    assertEquals(needed.get, qso1.fdHour)

  test("removeAll should clear all state and delete journal file"):
    val qso = Qso(callsign = Callsign("W9NNN"),
      exchange = Exchange(fdswarm.model.FdClass("1A"), "IL"),
      bandMode = BandMode("20m", "CW"),
      qsoMetadata = testQsoMetadata
    )
    qsoStore.add(qso)
    assertEquals(qsoStore.qsoCollection.size, 1)
    assertEquals(qsoStore.digests().size, 1)
    val journalFile = testDirectory() / "qsosJournal.json"
    assert(os.exists(journalFile))

    qsoStore.archiveAndClear()

    assertEquals(qsoStore.qsoCollection.size, 0)
    assertEquals(qsoStore.digests().size, 0)
    assertEquals(qsoStore.all.size, 0)
    assert(!os.exists(journalFile))

  test("potentialDups should limit results and return total count"):
    val bandMode = BandMode("20m", "CW")
    
    val qsos = (1 to 100).map { i =>
      Qso(callsign = Callsign(s"W9NN$i"),
        exchange = Exchange(fdswarm.model.FdClass("1A"), "IL"),
        bandMode = bandMode,
        qsoMetadata = testQsoMetadata
      )
    }
    qsoStore.add(qsos)
    
    val dupInfo = qsoStore.potentialDups("W9NN", bandMode)
    assertEquals(dupInfo.totalDups, 100)
    assertEquals(dupInfo.firstNDups.size, 70)
