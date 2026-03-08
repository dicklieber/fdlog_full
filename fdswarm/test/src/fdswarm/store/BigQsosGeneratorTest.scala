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
import fdswarm.replication.{MulticastTransport, SwarmStatus}
import fdswarm.util.{MetricsDebug, MockNodeIdentityManager, NodeIdentityManager}
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import munit.FunSuite

import java.util
import scala.compiletime.uninitialized

/**
 * Add a lot of QSOs to the store checking for correct collections.
 */
class BigQsosGeneratorTest extends FunSuite with LazyLogging:
  private var testDirectory: TestDirectory = uninitialized

  class MockMulticastTransport extends MulticastTransport(8900, "239.192.0.88", MockNodeIdentityManager(port = 8080)):
    override def send(service: fdswarm.replication.Service, data: Array[Byte]): Unit = ()
    override def stop(): Unit = ()

  private val mockTransport = new MockMulticastTransport()

  override def beforeEach(context: BeforeEach): Unit =
    testDirectory = new TestDirectory()

  override def afterEach(context: AfterEach): Unit =
    testDirectory.cleanup()

  /**
    * Mock BandModeBuilder that accepts any band/mode without consulting catalogs.
    */
  class AllowAllBandModeBuilder
      extends BandModeBuilder(
        null.asInstanceOf[BandCatalog],
        null.asInstanceOf[ModeCatalog]
      ):
    override def apply(band: BandMode.Band, mode: BandMode.Mode): BandMode =
      new BandMode(band.toLowerCase, mode.toUpperCase)

  test("generate 100 QSOs using BigQsosGenerator with permissive BandModeBuilder"):
    val registry = new SimpleMeterRegistry()
    val mockNodeIdentityManager = MockNodeIdentityManager(port = 8080)
    val swarmStatus = SwarmStatus(testDirectory, mockNodeIdentityManager)
    val qsoStore = QsoStore(testDirectory, registry, mockTransport, swarmStatus)

    val bandModeBuilder = new AllowAllBandModeBuilder
    val generator = new BigQsosGenerator(qsoStore, bandModeBuilder, mockNodeIdentityManager)

    // Generate 100 QSOs at 20 per hour cadence with prefix "K"
    generator.qsos(howMany = 10000, howManyPerHour = 400, prefix = "K")

    assertEquals(qsoStore.qsoCollection.size, 10000)
    // Ensure digests are built (at least one hour should have entries)
    val digests = qsoStore.digests()
    assertEquals(digests.size, 26)
    
    // Check qsosJournal.json (wc equivalent)
    val journalFile = testDirectory() / "qsosJournal.json"
    assert(os.exists(journalFile), "Journal file should exist")
    val bytes = os.read.bytes(journalFile)
    val content = new String(bytes, "UTF-8")
    val lineCount = content.count(_ == '\n')
    val wordCount = content.split("\\s+").count(_.nonEmpty)
    val byteCount = bytes.length
    
    assertEquals(lineCount, 10000, "Should have 10000 lines (one per QSO)")
    assert(byteCount > 10000 * 50, s"Byte count ($byteCount) seems too small for 10000 QSOs")
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
    assert(msCalculatingFdHourDigests < 100.0,
      s"Timer should have recorded less than 100ms expection around 50ms on fast MacBookPro, but got $msCalculatingFdHourDigests")