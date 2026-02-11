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
import java.net.DatagramSocket
import java.net.InetAddress

class BroadcastSenderTests extends munit.FunSuite {

  test("BroadcastSender can be constructed with default config") {
    val config = ConfigFactory.load()
    val sender = new BroadcastSender(config)
    // No-op, just check construction
    assert(sender != null)
  }

  test("BroadcastSender sends a packet (loopback)") {
    val configStr =
      """
        |fdswarm.broadcastAddress = "127.0.0.1"
        |fdswarm.broadcastPort = 2235
        |""".stripMargin
    val config = ConfigFactory.parseString(configStr)
    val sender = new BroadcastSender(config)

    val receiverSocket = new DatagramSocket(2235, InetAddress.getByName("127.0.0.1"))
    receiverSocket.setSoTimeout(1000)

    try {
      val testMsg = "Hello Broadcast"
      sender.broadcast(testMsg)

      val buffer = new Array[Byte](1024)
      val packet = new java.net.DatagramPacket(buffer, buffer.length)
      receiverSocket.receive(packet)

      val receivedMsg = new String(packet.getData, 0, packet.getLength, "UTF-8")
      assertEquals(receivedMsg, testMsg)
    } finally {
      receiverSocket.close()
    }
  }
}
