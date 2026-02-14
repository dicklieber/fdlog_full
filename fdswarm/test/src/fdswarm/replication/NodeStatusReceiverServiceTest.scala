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

import com.organization.BuildInfo
import fdswarm.io.DirectoryProvider
import fdswarm.store.QsoStore
import munit.FunSuite
import java.net.{DatagramPacket, DatagramSocket, InetAddress}
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

class NodeStatusReceiverServiceTest extends FunSuite:

  test("NodeStatusReceiverService receives and queues status updates"):
    // Find an available port
    val s1 = new DatagramSocket(0)
    val statusPort = s1.getLocalPort
    s1.close()

    val receiver = new NodeStatusReceiverService(
      statusPort = statusPort,
      ignoreSelf = false
    )
    val queue = receiver.queue

    receiver.start()

    try
      // Simulate a sender
      val senderSocket = new DatagramSocket()
      val jsonPayload = "{\"test\": \"data\"}"
      val gzipBytes = {
        val baos = new java.io.ByteArrayOutputStream()
        val gzos = new java.util.zip.GZIPOutputStream(baos)
        gzos.write(jsonPayload.getBytes("UTF-8"))
        gzos.close()
        baos.toByteArray
      }
      
      val packetBytes = UDPHeader(Service.Status, gzipBytes)
      val address = InetAddress.getByName("127.0.0.1")
      val packet = new DatagramPacket(packetBytes, packetBytes.length, address, statusPort)
      
      senderSocket.send(packet)
      
      val polled = queue.poll(5, TimeUnit.SECONDS)
      assert(polled != null, "Should have received a message in the queue")
      assertEquals(new String(polled, "UTF-8"), jsonPayload)
      
      senderSocket.close()
    finally
      receiver.stop()

  test("NodeStatusReceiverService ignores non-status messages"):
    val s1 = new DatagramSocket(0)
    val statusPort = s1.getLocalPort
    s1.close()

    val receiver = new NodeStatusReceiverService(
      statusPort = statusPort,
      ignoreSelf = false
    )
    val queue = receiver.queue

    receiver.start()

    try
      val senderSocket = new DatagramSocket()
      val packetBytes = UDPHeader(Service.Discovery) // Wrong service
      val address = InetAddress.getByName("127.0.0.1")
      val packet = new DatagramPacket(packetBytes, packetBytes.length, address, statusPort)
      
      senderSocket.send(packet)
      
      val polled = queue.poll(1, TimeUnit.SECONDS)
      assert(polled == null, "Should NOT have received a message in the queue")
      
      senderSocket.close()
    finally
      receiver.stop()
