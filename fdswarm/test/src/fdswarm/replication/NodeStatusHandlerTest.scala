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

import java.net.InetAddress
import java.util.concurrent.LinkedBlockingQueue

class NodeStatusHandlerTest extends FunSuite:

  class MockMulticastTransport extends MulticastTransport(8900, "239.192.0.88"):
    override val queue = new LinkedBlockingQueue[Array[Byte]]()
    // Avoid starting the actual receiver thread
    // The constructor calls the real one, but we can't easily stop it without side effects
    // but we just need the queue

  test("NodeStatusHandler ignores its own status messages"):
    val registry = new SimpleMeterRegistry()
    class TestReplicationSupport extends ReplicationSupport(new DirectoryProvider { override def apply(): os.Path = os.temp.dir() }, registry):
      var digestsCalled = false
      override def digests(): Seq[FdHourDigest] = {
        digestsCalled = true
        super.digests()
      }
    val replicationSupport = new TestReplicationSupport
    
    val transport = new MockMulticastTransport
    
    val myHostAndPort = HostAndPort("192.168.1.100", 8080)
    val hostAndPortProvider = new HostAndPortProvider(8080) {
      override val http = myHostAndPort
    }
    
    val remoteEndpointCaller = new RemoteEndpointCaller
    val neededRequester = new StatusProcessor(replicationSupport, remoteEndpointCaller)
    val handler = new NodeStatusHandler(replicationSupport, neededRequester, transport, hostAndPortProvider, new SwarmStatus)
    
    // Create a status message from "myself"
    val myStatus = StatusMessage(myHostAndPort, Seq.empty)
    val packet = UDPHeader(Service.Status, myStatus.toPacket)
    
    transport.queue.put(packet)
    
    handler.start()
    
    // Wait a bit for processing
    Thread.sleep(200)
    
    // Verify digests was NEVER called because it was ignored
    assert(!replicationSupport.digestsCalled, "digests should NOT have been called for our own message")
    
    handler.stop()
    transport.stop()

  test("NodeStatusHandler processes status messages from other nodes"):
    val registry = new SimpleMeterRegistry()
    class TestReplicationSupport extends ReplicationSupport(new DirectoryProvider { override def apply(): os.Path = os.temp.dir() }, registry):
      var determineNeededCalled = false
      override def handleStatusMessage(status: StatusMessage): cats.effect.IO[Seq[fdswarm.store.FdHourIds]] = {
        determineNeededCalled = true
        super.handleStatusMessage(status)
      }
    val replicationSupport = new TestReplicationSupport
    
    val transport = new MockMulticastTransport
    
    val myHostAndPort = HostAndPort("192.168.1.100", 8080)
    val otherHostAndPort = HostAndPort("192.168.1.101", 8080)
    
    val hostAndPortProvider = new HostAndPortProvider(8080) {
      override val http = myHostAndPort
    }
    
    val remoteEndpointCaller = new RemoteEndpointCaller
    val neededRequester = new StatusProcessor(replicationSupport, remoteEndpointCaller)
    val handler = new NodeStatusHandler(replicationSupport, neededRequester, transport, hostAndPortProvider, new SwarmStatus)
    
    // Create a status message from "someone else" with one FdHour
    val fdHour = FdHour(15, 10)
    val digest = FdHourDigest(fdHour, 5, "some-digest")
    val otherStatus = StatusMessage(otherHostAndPort, Seq(digest))
    val packet = UDPHeader(Service.Status, otherStatus.toPacket)
    
    transport.queue.put(packet)
    
    handler.start()
    
    // Wait a bit for processing
    Thread.sleep(500)
    
    // Verify determineNeeded WAS called (via NeededRequester)
    assert(replicationSupport.determineNeededCalled, "determineNeeded SHOULD have been called for other node's message")
    
    handler.stop()
    transport.stop()
