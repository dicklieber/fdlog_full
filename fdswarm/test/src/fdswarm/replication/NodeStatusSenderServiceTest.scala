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
import com.typesafe.config.ConfigFactory
import fdswarm.io.DirectoryProvider
import fdswarm.store.QsoStore
import munit.FunSuite
import java.net.{DatagramPacket, DatagramSocket}
import java.util.concurrent.{CountDownLatch, TimeUnit}

class NodeStatusSenderServiceTest extends FunSuite:

  test("NodeStatus broadcasts periodically"):
    // Find two available ports
    val s1 = new DatagramSocket(0)
    val statusPort = s1.getLocalPort
    s1.close()

    val config = ConfigFactory.parseString(
      s"""
         |statusPort = $statusPort
         |broadcastAddress = "127.0.0.1"
         |broadcastPeriodSec = 1
         |""".stripMargin
    )

    val tmpDir = os.temp.dir()
    val directoryProvider = new DirectoryProvider {
      override def apply(): os.Path = tmpDir
    }

    val qsoStore = new QsoStore(directoryProvider)
    val repl = new Repl(qsoStore)

    val nodeStatus = new NodeStatusSenderService(
      repl,
      statusPort,
      "127.0.0.1",
      1
    )

    val receiverSocket = new DatagramSocket(statusPort)
    receiverSocket.setSoTimeout(3000)
    
    val latch = new CountDownLatch(1)
    var receivedData: Option[Array[Byte]] = None

    val receiverThread = new Thread(() =>
      try
        val buffer = new Array[Byte](65535)
        val packet = new DatagramPacket(buffer, buffer.length)
        receiverSocket.receive(packet)
        receivedData = Some(packet.getData.slice(0, packet.getLength))
        latch.countDown()
      catch
        case e: Exception => e.printStackTrace()
    )
    receiverThread.start()

    nodeStatus.start()

    try
      val success = latch.await(5, TimeUnit.SECONDS)
      assert(success, "Should have received a broadcast within 5 seconds")
      assert(receivedData.isDefined, "Received data should be defined")
      val data = receivedData.get
      
      val expectedHeader = s"FDSWARM|Status|${BuildInfo.dataVersion}|\n".getBytes("UTF-8")
      
      assert(data.length > expectedHeader.length, "Data too short to contain header and GZIP")
      
      val actualHeader = data.slice(0, expectedHeader.length)
      assert(actualHeader.sameElements(expectedHeader), "Header mismatch")
      
      // Check if it's GZIP data (magic number 0x1f, 0x8b) after the header
      val gzipData = data.slice(expectedHeader.length, data.length)
      assert(gzipData.length >= 2, "GZIP data too short")
      assertEquals(gzipData(0), 0x1f.toByte, "GZIP magic byte 1 mismatch")
      assertEquals(gzipData(1), 0x8b.toByte, "GZIP magic byte 2 mismatch")
    finally
      nodeStatus.stop()
      receiverSocket.close()
      os.remove.all(tmpDir)
