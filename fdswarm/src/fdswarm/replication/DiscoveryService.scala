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

import com.google.inject.{Inject, Singleton}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.contest.{ContestConfig, ContestManager}
import upickle.default.*

import java.net.{DatagramPacket, DatagramSocket, InetAddress, NetworkInterface}
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import scala.jdk.CollectionConverters.*

@Singleton
class DiscoveryService @Inject()(
    contestManager: ContestManager,
    config: Config
) extends LazyLogging:

  private val discoveryPort = config.getInt("fdswarm.discovery.Port")
  private val timeoutMs = config.getLong("fdswarm.discovery.timeoutMs")
  private val broadcastAddress = config.getString("fdswarm.broadcastAddress")

  private var socket: Option[DatagramSocket] = None
  private var activeDiscovery: Option[LinkedBlockingQueue[ContestConfig]] = None

  private lazy val myAddresses: Set[InetAddress] =
    NetworkInterface.getNetworkInterfaces.asScala
      .flatMap(_.getInetAddresses.asScala)
      .toSet

  private val receiverThread = new Thread(() => {
    logger.info(s"DiscoveryService receiver listening on port $discoveryPort")
    val buffer = new Array[Byte](65535)
    while !Thread.currentThread().isInterrupted do
      try
        socket.foreach { s =>
          val packet = new DatagramPacket(buffer, buffer.length)
          s.receive(packet)
          val sender = packet.getAddress
          if myAddresses.contains(sender) then
            logger.debug(s"Ignoring our own message from $sender")
          else
            val data = new Array[Byte](packet.getLength)
            System.arraycopy(packet.getData, packet.getOffset, data, 0, packet.getLength)
            val message = new String(data, "UTF-8")
            if message == "FDSWARM|DISCOVER" then
              logger.info(s"Received FDSWARM|DISCOVER from $sender, responding with ContestConfig")
              val response = write(contestManager.config)
              send(response.getBytes("UTF-8"))
            else
              activeDiscovery.foreach { queue =>
                try
                  val config = read[ContestConfig](message)
                  queue.offer(config)
                catch
                  case e: Exception =>
                    logger.warn(s"Received unknown message from $sender: ${message.take(100)}")
              }
        }
      catch
        case _: InterruptedException => Thread.currentThread().interrupt()
        case e: Exception =>
          if socket.exists(!_.isClosed) then
            logger.error("Error receiving UDP packet in DiscoveryService", e)
  }, "DiscoveryService-Receiver")

  receiverThread.setDaemon(true)

  private def send(array: Array[Byte]): Unit =
    socket.foreach(s =>
      val address = InetAddress.getByName(broadcastAddress)
      val packet = new DatagramPacket(array, array.length, address, discoveryPort)
      s.send(packet)
    )

  def start(): Unit =
    socket = Some(new DatagramSocket(discoveryPort))
    receiverThread.start()

  def stop(): Unit =
    logger.info("Stopping DiscoveryService")
    receiverThread.interrupt()
    socket.foreach(_.close())
    socket = None

  def discover(): Seq[ContestConfig] =
    logger.debug(s"Sending FDSWARM|DISCOVER waiting for $timeoutMs ms")
    val queue = new LinkedBlockingQueue[ContestConfig]()
    activeDiscovery = Some(queue)
    try
      send("FDSWARM|DISCOVER".getBytes("UTF-8"))

      val entry = queue.poll(timeoutMs, TimeUnit.MILLISECONDS)
      val results = if entry != null then Seq(entry) else Seq.empty
      logger.debug("Discovered {} nodes", results.size)
      results
    finally
      activeDiscovery = None
