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
import fdswarm.store.{FdHourDigest, QsoStore}
import fdswarm.util.{HostAndPort, HostAndPortProvider}
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
    class TestQsoStore extends QsoStore(new DirectoryProvider { override def apply(): os.Path = os.temp.dir() }):
      var neededQsosCalled = false
      override def neededQsos(incoming: Seq[FdHourDigest]): Seq[FdHour] = {
        neededQsosCalled = true
        Seq.empty
      }
    val qsoStore = new TestQsoStore
    
    val transport = new MockMulticastTransport
    
    val myHostAndPort = HostAndPort("192.168.1.100", 8080)
    val hostAndPortProvider = new HostAndPortProvider(8080) {
      override val http = myHostAndPort
    }
    
    val handler = new NodeStatusHandler(qsoStore, transport, hostAndPortProvider, new SwarmStatus)
    
    // Create a status message from "myself"
    val myStatus = StatusMessage(myHostAndPort, Seq.empty)
    val packet = UDPHeader(Service.Status, myStatus.toPacket)
    
    transport.queue.put(packet)
    
    handler.start()
    
    // Wait a bit for processing
    Thread.sleep(200)
    
    // Verify neededQsos was NEVER called because it was ignored
    assert(!qsoStore.neededQsosCalled, "neededQsos should NOT have been called for our own message")
    
    handler.stop()
    transport.stop()

  test("NodeStatusHandler processes status messages from other nodes"):
    class TestQsoStore extends QsoStore(new DirectoryProvider { override def apply(): os.Path = os.temp.dir() }):
      var neededQsosCalled = false
      override def neededQsos(incoming: Seq[FdHourDigest]): Seq[FdHour] = {
        neededQsosCalled = true
        Seq.empty
      }
    val qsoStore = new TestQsoStore
    
    val transport = new MockMulticastTransport
    
    val myHostAndPort = HostAndPort("192.168.1.100", 8080)
    val otherHostAndPort = HostAndPort("192.168.1.101", 8080)
    
    val hostAndPortProvider = new HostAndPortProvider(8080) {
      override val http = myHostAndPort
    }
    
    val handler = new NodeStatusHandler(qsoStore, transport, hostAndPortProvider, new SwarmStatus)
    
    // Create a status message from "someone else"
    val otherStatus = StatusMessage(otherHostAndPort, Seq.empty)
    val packet = UDPHeader(Service.Status, otherStatus.toPacket)
    
    transport.queue.put(packet)
    
    handler.start()
    
    // Wait a bit for processing
    Thread.sleep(200)
    
    // Verify neededQsos WAS called
    assert(qsoStore.neededQsosCalled, "neededQsos SHOULD have been called for other node's message")
    
    handler.stop()
    transport.stop()
