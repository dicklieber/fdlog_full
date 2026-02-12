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

import com.typesafe.config.ConfigFactory
import java.net.{DatagramPacket, DatagramSocket, InetAddress}

class DiscoveryServiceTests extends munit.FunSuite {

  test("DiscoveryService sends fdswarm|discover and collects responses") {
    val port = 2236
    val configStr =
      s"""
        |fdswarm.broadcastAddress = "127.0.0.1"
        |fdswarm.broadcastPort = $port
        |fdswarm.discoveryTimeoutMs = 500
        |""".stripMargin
    val config = ConfigFactory.parseString(configStr)
    val service = new DiscoveryService(config)

    // A mock node that listens for discovery and responds
    val mockNodeThread = new Thread(() => {
      val socket = new DatagramSocket(port, InetAddress.getByName("127.0.0.1"))
      try {
        socket.setSoTimeout(1000)
        val buffer = new Array[Byte](1024)
        val packet = new DatagramPacket(buffer, buffer.length)
        
        // Wait for discovery request
        socket.receive(packet)
        val msg = new String(packet.getData, 0, packet.getLength, "UTF-8")
        if (msg == "fdswarm|discover") {
          // Respond back to the sender
          val response = "fdswarm|response|node1"
          val responseBytes = response.getBytes("UTF-8")
          val responsePacket = new DatagramPacket(
            responseBytes,
            responseBytes.length,
            packet.getAddress,
            packet.getPort
          )
          socket.send(responsePacket)
        }
      } catch {
        case _: Exception => // ignore
      } finally {
        socket.close()
      }
    })
    mockNodeThread.start()

    // Small delay to ensure mock node is listening
    Thread.sleep(100)

    val responses = service.discover()
    
    assertEquals(responses.size, 1)
    assertEquals(responses.head, "fdswarm|response|node1")
    
    mockNodeThread.join()
  }

  test("DiscoveryService handles no responses") {
    val port = 2237
    val configStr =
      s"""
        |fdswarm.broadcastAddress = "127.0.0.1"
        |fdswarm.broadcastPort = $port
        |fdswarm.discoveryTimeoutMs = 200
        |""".stripMargin
    val config = ConfigFactory.parseString(configStr)
    val service = new DiscoveryService(config)

    val responses = service.discover()
    assert(responses.isEmpty)
  }
}
