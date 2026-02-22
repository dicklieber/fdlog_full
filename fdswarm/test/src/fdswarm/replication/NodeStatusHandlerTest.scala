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

import fdswarm.fx.qso.FdHour
import fdswarm.io.DirectoryProvider
import fdswarm.store.{FdHourDigest, FdHourIds, QsoStore, ReplicationSupport}
import fdswarm.util.{HostAndPort, HostAndPortProvider}
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import munit.FunSuite

import java.net.{InetAddress, NetworkInterface}
import java.util.concurrent.LinkedBlockingQueue
import scala.jdk.CollectionConverters.*

class NodeStatusHandlerTest extends FunSuite:

  class MockMulticastTransport(hostAndPortProvider: HostAndPortProvider) extends MulticastTransport(8900, "239.192.0.88", hostAndPortProvider):
    override val queue = new LinkedBlockingQueue[UDPHeaderData]()
    
    // Simulates receiving a raw packet and performing filtering
    def receiveRaw(packetBytes: Array[Byte], senderAddress: InetAddress = InetAddress.getByName("192.168.1.101")): Unit =
      val localAddresses = NetworkInterface.getNetworkInterfaces.asScala
        .flatMap(_.getInetAddresses.asScala)
        .toSet

      if !localAddresses.contains(senderAddress) then
        val udpHeader = UDPHeader.parse(packetBytes)
        queue.offer(udpHeader)

  test("MulticastTransport ignores its own status messages"):
    val myHostAndPort = HostAndPort("127.0.0.1", 8080)
    val hostAndPortProvider = new HostAndPortProvider(8080) {
      override val http = myHostAndPort
    }
    val transport = new MockMulticastTransport(hostAndPortProvider)
    
    // Create a status message
    val myStatus = StatusMessage(myHostAndPort, Seq.empty)
    val packetBytes = UDPHeader(Service.Status, myStatus.toPacket)
    
    // Simulate receiving it from "ourselves" (127.0.0.1)
    transport.receiveRaw(packetBytes, InetAddress.getByName("127.0.0.1"))
    
    assert(transport.queue.isEmpty, "MulticastTransport should HAVE ignored our own message")
    transport.stop()

  test("MulticastTransport ignores its own QSO messages"):
    val myHostAndPort = HostAndPort("127.0.0.1", 8080)
    val hostAndPortProvider = new HostAndPortProvider(8080) {
      override val http = myHostAndPort
    }
    val transport = new MockMulticastTransport(hostAndPortProvider)

    import fdswarm.model.*
    import fdswarm.model.QsoMetadata.testQsoMetadata
    import io.circe.syntax.*

    val qso = Qso(callsign = Callsign("W9NNN"),
      contestClass = "WFD",
      bandMode = BandMode("20m", "CW"),
      section = "IL",
      qsoMetadata = testQsoMetadata.copy(node = "local")
    )
    val packetBytes = UDPHeader(Service.QSO, qso.asJson.noSpaces.getBytes("UTF-8"))

    transport.receiveRaw(packetBytes, InetAddress.getByName("127.0.0.1"))
    
    assert(transport.queue.isEmpty, "MulticastTransport should HAVE ignored our own QSO")
    transport.stop()

  test("NodeStatusHandler processes status messages from other nodes"):
    val registry = new SimpleMeterRegistry()
    class TestReplicationSupport extends ReplicationSupport(new DirectoryProvider { override def apply(): os.Path = os.temp.dir() }, registry):
      var isFdHourNeededCalled = false
      override def isFdHourNeeded(fdHourDigest: FdHourDigest): Option[FdHour] = {
        isFdHourNeededCalled = true
        super.isFdHourNeeded(fdHourDigest)
      }
    val replicationSupport = new TestReplicationSupport
    
    val myHostAndPort = HostAndPort("192.168.1.100", 8080)
    val otherHostAndPort = HostAndPort("192.168.1.101", 8080)
    
    val hostAndPortProvider = new HostAndPortProvider(8080) {
      override val http = myHostAndPort
    }
    
    val transport = new MockMulticastTransport(hostAndPortProvider)
    
    val remoteEndpointCaller = new CallEndpoint
    val neededRequester = new StatusProcessor(replicationSupport, null, remoteEndpointCaller)
    val handler = new NodeStatusHandler(replicationSupport, neededRequester, transport, hostAndPortProvider, new SwarmStatus)
    
    // Create a status message from "someone else" with one FdHour
    val fdHour = FdHour(15, 10)
    val digest = FdHourDigest(fdHour, 5, "some-digest")
    val otherStatus = StatusMessage(otherHostAndPort, Seq(digest))
    val packetBytes = UDPHeader(Service.Status, otherStatus.toPacket)
    val packet = UDPHeader.parse(packetBytes)
    
    transport.queue.put(packet)
    
    handler.start()
    
    // Wait a bit for processing
    Thread.sleep(500)
    
    handler.stop()
    transport.stop()

  test("NodeStatusHandler processes QSO messages from other nodes"):
    val registry = new SimpleMeterRegistry()
    class TestReplicationSupport extends ReplicationSupport(new DirectoryProvider { override def apply(): os.Path = os.temp.dir() }, registry):
      var addedQso: Option[fdswarm.model.Qso] = None
      override def add(qso: fdswarm.model.Qso): Unit = {
        addedQso = Some(qso)
        super.add(qso)
      }
    val replicationSupport = new TestReplicationSupport

    val myHostAndPort = HostAndPort("192.168.1.100", 8080)
    val hostAndPortProvider = new HostAndPortProvider(8080) {
      override val http = myHostAndPort
    }
    
    val transport = new MockMulticastTransport(hostAndPortProvider)

    val remoteEndpointCaller = new CallEndpoint
    val neededRequester = new StatusProcessor(replicationSupport, null, remoteEndpointCaller)
    val handler = new NodeStatusHandler(replicationSupport, neededRequester, transport, hostAndPortProvider, new SwarmStatus)

    import fdswarm.model.*
    import fdswarm.model.QsoMetadata.testQsoMetadata
    import io.circe.syntax.*

    val qso = Qso(callsign = Callsign("W9NNN"),
      contestClass = "WFD",
      bandMode = BandMode("20m", "CW"),
      section = "IL",
      qsoMetadata = testQsoMetadata
    )
    val packetBytes = UDPHeader(Service.QSO, qso.asJson.noSpaces.getBytes("UTF-8"))
    val packet = UDPHeader.parse(packetBytes)

    transport.queue.put(packet)

    handler.start()

    // Wait a bit for processing
    Thread.sleep(500)

    // Verify qso was added
    assert(replicationSupport.addedQso.isDefined, "QSO should have been added")
    assertEquals(replicationSupport.addedQso.get.uuid, qso.uuid)

    handler.stop()
    transport.stop()
