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
import fdswarm.fx.discovery.{
  ContestDiscovery,
  DiscoveryWire,
  NodeContestStation
}
import fdswarm.model.Callsign
import fdswarm.replication.{Service, Transport, UDPHeaderData}
import fdswarm.util.JavaTimeCirce.given
import fdswarm.util.{MockNodeIdentityManager, NodeIdentity, NodeIdentityManager}
import io.circe.syntax.*
import munit.FunSuite

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
    private val mockResponseQueue = new LinkedBlockingQueue[UDPHeaderData]()
    var lastSentService: Option[Service] = None

    override def startQueue(service: Service): Queue =
      if (service == Service.DiscResponse) then 
        mockResponseQueue
      else
        new LinkedBlockingQueue[UDPHeaderData]()

    override def stopQueue(service: Service): Unit = ()
    override def send(data: Array[Byte]): Unit = ()
    override def send(service: Service, data: Array[Byte]): Unit =
      lastSentService = Some(service)
      if (service == Service.DiscReq) {
        val responses = List(
          (NodeIdentity("10.0.0.1", 8081, "node1", "123"), Callsign("W1AW")),
          (NodeIdentity("10.0.0.2", 8081, "node2", "456"), Callsign("K1ABC")),
          (NodeIdentity("10.0.0.3", 8081, "node3", "789"), Callsign("N1XYZ"))
        )
        responses.foreach { case (nodeId, callsign) =>
          val config = DiscoveryWire(
            ContestConfig(
              ContestType.WFD,
              callsign,
              2,
              "I",
              "CT"
            ),
            fdswarm.model.StationConfig()
          )
          val payload = config.asJson.noSpaces.getBytes("UTF-8")
          val header = UDPHeaderData(Service.DiscResponse, nodeId, payload)
          mockResponseQueue.offer(header)
        }
      }

    override def sentCount: Long = if lastSentService.isDefined then 1 else 0
    override def stop(): Unit = ()

  test("discoverContest should send request and collect responses"):
    val mockTransport = new MockTransport()
    val discovery = new ContestDiscovery(mockTransport, 1) // 1 second timeout for test
    import scala.collection.mutable.ListBuffer
    val results = ListBuffer[(NodeIdentity, DiscoveryWire)]()
    discovery.discoverContest(ncs => results += ((ncs.nodeIdentity, ncs.contestStation)))
    assertEquals(mockTransport.lastSentService, Some(Service.DiscReq))
    assertEquals(results.size, 3)
    assertEquals(results.map(_._1.hostIp).toSet, Set("10.0.0.1", "10.0.0.2", "10.0.0.3"))
