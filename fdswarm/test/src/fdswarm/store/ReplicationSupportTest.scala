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
import fdswarm.fx.bandmodes.SelectedBandModeStore
import fdswarm.StationManager
import fdswarm.TestDirectory
import fdswarm.model.QsoMetadata.testQsoMetadata
import fdswarm.model.{BandMode, Callsign, Exchange, FdClass, Qso}
import fdswarm.replication.Transport
import fdswarm.replication.status.SwarmStatus
import fdswarm.util.{NodeIdentityManager, MockNodeIdentityManager}
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import munit.FunSuite

import scala.compiletime.uninitialized

class ReplicationSupportTest extends FunSuite:
  private var testDirectory: TestDirectory = uninitialized

  class MockTransport extends Transport:
    override val nodeIdentityManager: NodeIdentityManager = MockNodeIdentityManager()
    override val mode: String = "Mock"
    override val queue = new java.util.concurrent.LinkedBlockingQueue[fdswarm.replication.UDPHeaderData]()
    override def addListener(listener: fdswarm.replication.UDPHeaderData => Unit): Unit = ()
    override def removeListener(listener: fdswarm.replication.UDPHeaderData => Unit): Unit = ()
    override def send(data: Array[Byte]): Unit = ()
    override def send(service: fdswarm.replication.Service, data: Array[Byte]): Unit = ()
    override def sentCount: Long = 0
    override def stop(): Unit = ()

  private val mockTransport = new MockTransport()

  private var mockNodeIdentityManager: fdswarm.util.MockNodeIdentityManager = uninitialized
  private var stationManager: StationManager = uninitialized
  private var selectedBandModeStore: SelectedBandModeStore = uninitialized
  private var swarmStatus: fdswarm.replication.status.SwarmStatus = uninitialized
  private var contestCatalog: fdswarm.fx.contest.ContestCatalog = uninitialized
  private var sections: fdswarm.fx.sections.Sections = uninitialized
  private var filenameStamp: fdswarm.util.FilenameStamp = uninitialized
  private var contestManager: fdswarm.fx.contest.ContestManager = uninitialized
  private var qsoStore: QsoStore = uninitialized
  private var registry: SimpleMeterRegistry = uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    testDirectory = new TestDirectory()
    registry = new SimpleMeterRegistry()
    mockNodeIdentityManager = fdswarm.util.MockNodeIdentityManager(port = 8080)
    stationManager = new StationManager(testDirectory)
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
    selectedBandModeStore = new SelectedBandModeStore(testDirectory, bandModeBuilder)
    swarmStatus = fdswarm.replication.status.SwarmStatus(testDirectory, mockNodeIdentityManager, stationManager, selectedBandModeStore, null)
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
    qsoStore = new QsoStore(testDirectory, registry, mockTransport, swarmStatus, filenameStamp)
    val discovery = new fdswarm.fx.contest.ContestDiscovery(mockTransport, 1)
    contestManager = new fdswarm.fx.contest.ContestManager(testDirectory, contestCatalog, sections, qsoStore, filenameStamp, mockTransport, discovery, 7)

  override def afterEach(context: AfterEach): Unit =
    testDirectory.cleanup()

  test("missingIds should return ids present in remote but not in local"):
    import cats.effect.unsafe.implicits.global
    val replicationSupport = ReplicationSupport(testDirectory, registry, mockTransport, swarmStatus, filenameStamp)

    val qso1 = Qso(callsign = Callsign("W9NNN"),
      exchange = Exchange(FdClass("1A"), "IL"),
      bandMode = BandMode("20m", "CW"),
      qsoMetadata = testQsoMetadata
    )
    val qso2 = Qso(callsign = Callsign("K9OR"),
      exchange = Exchange(FdClass("1A"), "IL"),
      bandMode = BandMode("40m", "SSB"),
      qsoMetadata = testQsoMetadata
    )
    val qso3 = Qso(callsign = Callsign("N9RE"),
      exchange = Exchange(FdClass("1A"), "IN"),
      bandMode = BandMode("80m", "FT8"),
      qsoMetadata = testQsoMetadata
    )

    // Local has qso1
    replicationSupport.add(qso1)

    // Remote has qso1, qso2, and qso3 (all in the same FdHour)
    // assuming they are in the same FdHour for this test
    val fdHour = qso1.fdHour
    val remote = FdHourIds(fdHour, Seq(qso1.uuid, qso2.uuid, qso3.uuid))

    val missing = replicationSupport.missingIds(remote).unsafeRunSync()

    // missing should contain qso2 and qso3
    assertEquals(missing.toSet, Set(qso2.uuid, qso3.uuid))

  test("missingIds should return empty if all remote ids are present locally"):
    import cats.effect.unsafe.implicits.global
    val replicationSupport = ReplicationSupport(testDirectory, registry, mockTransport, swarmStatus, filenameStamp)

    val qso1 = Qso(callsign = Callsign("W9NNN"),
      exchange = Exchange(FdClass("1A"), "IL"),
      bandMode = BandMode("20m", "CW"),
      qsoMetadata = testQsoMetadata
    )

    replicationSupport.add(qso1)

    val remote = FdHourIds(qso1.fdHour, Seq(qso1.uuid))
    val missing = replicationSupport.missingIds(remote).unsafeRunSync()

    assert(missing.isEmpty)

  test("missingIds should return all remote ids if local has none for that hour"):
    import cats.effect.unsafe.implicits.global
    val replicationSupport = ReplicationSupport(testDirectory, registry, mockTransport, swarmStatus, filenameStamp)

    val qso1 = Qso(callsign = Callsign("W9NNN"),
      exchange = Exchange(FdClass("1A"), "IL"),
      bandMode = BandMode("20m", "CW"),
      qsoMetadata = testQsoMetadata
    )

    val remote = FdHourIds(qso1.fdHour, Seq(qso1.uuid))
    val missing = replicationSupport.missingIds(remote).unsafeRunSync()

    assertEquals(missing, Seq(qso1.uuid))

  test("idsForHour should return all ids for given fdHour"):
    import cats.effect.unsafe.implicits.global
    val replicationSupport = ReplicationSupport(testDirectory, registry, mockTransport, swarmStatus, filenameStamp)

    val qso1 = Qso(callsign = Callsign("W9NNN"),
      exchange = Exchange(FdClass("1A"), "IL"),
      bandMode = BandMode("20m", "CW"),
      qsoMetadata = testQsoMetadata
    )
    val qso2 = Qso(callsign = Callsign("K9OR"),
      exchange = Exchange(FdClass("1A"), "IL"),
      bandMode = BandMode("40m", "SSB"),
      qsoMetadata = testQsoMetadata
    )
    replicationSupport.add(Seq(qso1, qso2))

    val result = replicationSupport.idsForHour(qso1.fdHour).unsafeRunSync()
    assertEquals(result.fdHour, qso1.fdHour)
    assertEquals(result.ids.toSet, Set(qso1.uuid, qso2.uuid))

  test("qsosForFdHour should return all qsos for given fdHour"):
    import cats.effect.unsafe.implicits.global
    val replicationSupport = ReplicationSupport(testDirectory, registry, mockTransport, swarmStatus, filenameStamp)

    val qso1 = Qso(callsign = Callsign("W9NNN"),
      exchange = Exchange(FdClass("1A"), "IL"),
      bandMode = BandMode("20m", "CW"),
      qsoMetadata = testQsoMetadata
    )
    replicationSupport.add(qso1)

    val result = replicationSupport.qsosForFdHour(qso1.fdHour).unsafeRunSync()
    assertEquals(result, Seq(qso1))
