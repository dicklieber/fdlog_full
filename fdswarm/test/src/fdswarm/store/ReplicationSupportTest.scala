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
import fdswarm.replication.{Transport, SwarmStatus}
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
    override def stop(): Unit = ()

  private val mockTransport = new MockTransport()
  private val mockNodeIdentityManager = MockNodeIdentityManager()
  private lazy val swarmStatus = SwarmStatus(testDirectory, mockNodeIdentityManager)

  override def beforeEach(context: BeforeEach): Unit =
    testDirectory = new TestDirectory()

  override def afterEach(context: AfterEach): Unit =
    testDirectory.cleanup()

  test("missingIds should return ids present in remote but not in local"):
    import cats.effect.unsafe.implicits.global
    val registry = new SimpleMeterRegistry()
    val replicationSupport = ReplicationSupport(testDirectory, registry, mockTransport, swarmStatus)

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
    val qso3 = Qso(callsign = Callsign("N9RE"),
      contestClass = "WFD",
      bandMode = BandMode("80m", "FT8"),
      section = "IN",
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
    val registry = new SimpleMeterRegistry()
    val replicationSupport = ReplicationSupport(testDirectory, registry, mockTransport, swarmStatus)

    val qso1 = Qso(callsign = Callsign("W9NNN"),
      contestClass = "WFD",
      bandMode = BandMode("20m", "CW"),
      section = "IL",
      qsoMetadata = testQsoMetadata
    )

    replicationSupport.add(qso1)

    val remote = FdHourIds(qso1.fdHour, Seq(qso1.uuid))
    val missing = replicationSupport.missingIds(remote).unsafeRunSync()

    assert(missing.isEmpty)

  test("missingIds should return all remote ids if local has none for that hour"):
    import cats.effect.unsafe.implicits.global
    val registry = new SimpleMeterRegistry()
    val replicationSupport = ReplicationSupport(testDirectory, registry, mockTransport, swarmStatus)

    val qso1 = Qso(callsign = Callsign("W9NNN"),
      contestClass = "WFD",
      bandMode = BandMode("20m", "CW"),
      section = "IL",
      qsoMetadata = testQsoMetadata
    )

    val remote = FdHourIds(qso1.fdHour, Seq(qso1.uuid))
    val missing = replicationSupport.missingIds(remote).unsafeRunSync()

    assertEquals(missing, Seq(qso1.uuid))

  test("idsForHour should return all ids for given fdHour"):
    import cats.effect.unsafe.implicits.global
    val registry = new SimpleMeterRegistry()
    val replicationSupport = ReplicationSupport(testDirectory, registry, mockTransport, swarmStatus)

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

    val result = replicationSupport.idsForHour(qso1.fdHour).unsafeRunSync()
    assertEquals(result.fdHour, qso1.fdHour)
    assertEquals(result.ids.toSet, Set(qso1.uuid, qso2.uuid))

  test("qsosForFdHour should return all qsos for given fdHour"):
    import cats.effect.unsafe.implicits.global
    val registry = new SimpleMeterRegistry()
    val replicationSupport = ReplicationSupport(testDirectory, registry, mockTransport, swarmStatus)

    val qso1 = Qso(callsign = Callsign("W9NNN"),
      contestClass = "WFD",
      bandMode = BandMode("20m", "CW"),
      section = "IL",
      qsoMetadata = testQsoMetadata
    )
    replicationSupport.add(qso1)

    val result = replicationSupport.qsosForFdHour(qso1.fdHour).unsafeRunSync()
    assertEquals(result, Seq(qso1))
