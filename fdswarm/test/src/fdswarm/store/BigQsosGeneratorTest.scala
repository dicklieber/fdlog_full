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

import com.typesafe.scalalogging.LazyLogging
import fdswarm.TestDirectory
import fdswarm.fx.bands.{BandCatalog, BandModeBuilder, ModeCatalog}
import fdswarm.model.BandMode
import fdswarm.replication.Transport
import fdswarm.replication.status.SwarmStatus
import fdswarm.util.{MetricsDebug, MockNodeIdentityManager, NodeIdentityManager}
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import munit.FunSuite

import java.time.Instant
import java.util
import scala.compiletime.uninitialized

/**
 * Add a lot of QSOs to the store checking for correct collections.
 */
class BigQsosGeneratorTest extends FunSuite with LazyLogging:
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

  private var mockNodeIdentityManager: MockNodeIdentityManager = uninitialized
  private var swarmStatus: SwarmStatus = uninitialized
  private var contestCatalog: fdswarm.fx.contest.ContestCatalog = uninitialized
  private var sections: fdswarm.fx.sections.Sections = uninitialized
  private var filenameStamp: fdswarm.util.FilenameStamp = uninitialized
  private var contestManager: fdswarm.fx.contest.ContestManager = uninitialized
  private var qsoStore: QsoStore = uninitialized
  private var registry: SimpleMeterRegistry = uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    testDirectory = new TestDirectory()
    registry = new SimpleMeterRegistry()
    mockNodeIdentityManager = MockNodeIdentityManager(port = 8080)
    swarmStatus = SwarmStatus(testDirectory, mockNodeIdentityManager, null)
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

  /**
    * Mock BandModeBuilder that accepts any band/mode without consulting catalogs.
    */
  class AllowAllBandModeBuilder(bandCatalog: BandCatalog, modeCatalog: ModeCatalog)
      extends BandModeBuilder(bandCatalog, modeCatalog):
    override def apply(band: BandMode.Band, mode: BandMode.Mode): BandMode =
      new BandMode(band.toLowerCase, mode.toUpperCase)

  test("generate 100 QSOs using BigQsosGenerator with permissive BandModeBuilder"):
    import com.typesafe.config.ConfigFactory
    val config = ConfigFactory.parseString(
      """
        |fdswarm {
        |  hamBands = [
        |    { bandName = "20m", startFrequencyHz = 14000000, endFrequencyHz = 14350000, bandClass = "HF", regions = ["ALL"] }
        |  ]
        |  modes = ["CW", "PH", "DIGI"]
        |}
        |""".stripMargin)
    val mockBandCatalog = new BandCatalog(config)
    val mockModeCatalog = new ModeCatalog(config)
    val bandModeBuilder = new AllowAllBandModeBuilder(mockBandCatalog, mockModeCatalog)
    val generator = new BigQsosGenerator(qsoStore, bandModeBuilder, mockNodeIdentityManager, mockBandCatalog, mockModeCatalog)

    // Set 'now' to a fixed point at the beginning of an hour to ensure we only span 5 hours.
    // 2026-03-14T05:59:59Z would also work, but let's be safe.
    // We use a date in the middle of the month (15th) to avoid any month-boundary issues in CI.
    val now = Instant.parse("2026-03-15T05:59:59Z")
    // Generate 100 QSOs at 20 per hour cadence with prefix "K"
    generator.qsos(howMany = 100, howManyPerHour = 20, prefix = "K", now = now)

    assertEquals(qsoStore.qsoCollection.size, 100)
    // Ensure digests are built (at least one hour should have entries)
    val digests = qsoStore.digests()
    assertEquals(digests.size, 5)
    
    // Check qsosJournal.json (wc equivalent)
    val journalFile = testDirectory() / "qsosJournal.json"
    assert(os.exists(journalFile), "Journal file should exist")
    val bytes = os.read.bytes(journalFile)
    val content = new String(bytes, "UTF-8")
    val lineCount = content.count(_ == '\n')
    val wordCount = content.split("\\s+").count(_.nonEmpty)
    val byteCount = bytes.length
    
    assertEquals(lineCount, 100, "Should have 100 lines (one per QSO)")
    assert(byteCount > 100 * 50, s"Byte count ($byteCount) seems too small for 100 QSOs")
    logger.info(s"Journal wc: lines=$lineCount, words=$wordCount, bytes=$byteCount")

    // Verify metrics
    val counter = registry.find("fdlog.build.hour.digests.count").counter()
    assert(counter != null, "Counter fdlog.build.hour.digests.count should exist")
    // It should have been called twice in this test:
    // 1. When initializing QsoStore (if journalFile exists - but it doesn't here)
    // 2. When qsoStore.add(batchOfQsos) is called by generator.qsos
    assertEquals(counter.count(), 1.0)

    val sMetrics = MetricsDebug.dumpMetrics(registry)
    println(sMetrics)
    val meters: util.List[Meter] = registry.getMeters

    val timer = registry.find("fdlog.build.hour.digests").timer()
    assert(timer != null, "Timer fdlog.build.hour.digests should exist")
    assertEquals(timer.count(), 1L)
    val msCalculatingFdHourDigests: Double = timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)
    assert(msCalculatingFdHourDigests > 0, "Timer should have recorded some time")
    assert(msCalculatingFdHourDigests < 200.0,
      s"Timer should have recorded less than 100ms expection around 50ms on fast MacBookPro, but got $msCalculatingFdHourDigests")