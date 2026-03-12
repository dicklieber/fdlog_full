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

package fdswarm.fx.contest

import fdswarm.TestDirectory
import fdswarm.model.Callsign
import fdswarm.replication.{Service, Transport, UDPHeaderData}
import fdswarm.util.{MockNodeIdentityManager, NodeIdentity, NodeIdentityManager}
import io.circe.syntax.*
import munit.FunSuite
import fdswarm.util.JavaTimeCirce.given

import java.time.*
import scala.compiletime.uninitialized

class ContestDefinitionDiscoveryTest extends FunSuite:
  private var testDirectory: TestDirectory = uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    testDirectory = new TestDirectory()

  override def afterEach(context: AfterEach): Unit =
    testDirectory.cleanup()

  class MockTransport extends Transport:
    override val nodeIdentityManager: NodeIdentityManager = MockNodeIdentityManager()
    override val mode: String = "Mock"
    override val queue = new java.util.concurrent.LinkedBlockingQueue[UDPHeaderData]()
    var lastSentService: Option[Service] = None
    override def send(data: Array[Byte]): Unit = ()
    override def send(service: Service, data: Array[Byte]): Unit =
      lastSentService = Some(service)
      if (service == Service.DiscReq) {
        // Simulate a response from another node
        val otherNode = NodeIdentity("10.0.0.1", 8081, "other-instance")
        val config = ContestConfig(
          ContestType.WFD,
          ZonedDateTime.now(ZoneOffset.UTC),
          ZonedDateTime.now(ZoneOffset.UTC).plusDays(1),
          Callsign("W1AW"),
          2,
          "I",
          "CT"
        )
        val payload = config.asJson.noSpaces.getBytes("UTF-8")
        val header = UDPHeaderData(Service.DiscResponse, otherNode, payload)
        // In our real MulticastTransport, we added listeners.
        // We need to trigger them here.
        triggerListeners(header)
      }

    private val testListeners = new java.util.concurrent.CopyOnWriteArrayList[UDPHeaderData => Unit]()
    override def addListener(l: UDPHeaderData => Unit): Unit = testListeners.add(l)
    override def removeListener(l: UDPHeaderData => Unit): Unit = testListeners.remove(l)
    def triggerListeners(h: UDPHeaderData): Unit = testListeners.forEach(_.apply(h))
    override def sentCount: Long = if lastSentService.isDefined then 1 else 0
    override def stop(): Unit = ()

  test("discoverContest should send request and collect responses"):
    val mockTransport = new MockTransport()
    val discovery = new ContestDiscovery(mockTransport, 1) // 1 second timeout for test
    
    val results = discovery.discoverContest()
    
    assertEquals(mockTransport.lastSentService, Some(Service.DiscReq))
    assertEquals(results.size, 1)
    val (node, config) = results.head
    assertEquals(node.host, "10.0.0.1")
    assertEquals(config.ourCallsign, Callsign("W1AW"))
