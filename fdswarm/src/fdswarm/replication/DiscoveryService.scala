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

import java.net.{DatagramPacket, DatagramSocket, InetAddress, InetSocketAddress, NetworkInterface}
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import scala.jdk.CollectionConverters.*

/**
 * Handles node discovery within the fdswarm network using UDP broadcasts.
 *
 * This service provides mechanisms for:
 * 1.  **Responding to Discovery Requests**: Listens on a configured UDP port for `Service.Discovery` messages
 *     and responds with the local node's current [[ContestConfig]] using a `Service.Discovered` message.
 * 2.  **Active Discovery**: Sends a broadcast `Service.Discovery` request and waits for responses from other
 *     nodes to build a list of available contest configurations in the swarm.
 *
 * The service runs a background daemon thread ("DiscoveryService-Receiver") that handles all incoming
 * UDP traffic on the discovery port.
 *
 * Hoever sending discovery brodcast only happens when [[discover()]] is called.
 *
 * @param contestManager provides the local node's contest configuration for outgoing discovery responses
 * @param config application configuration containing discovery port, timeout, and broadcast address
 */
@Singleton
class DiscoveryService @Inject()(
    contestManager: ContestManager,
    config: Config
) extends LazyLogging:

  private val discoveryPort = config.getInt("fdswarm.discovery.Port")
  private val timeoutMs = config.getLong("fdswarm.discovery.timeoutMs")
  private val broadcastAddress = config.getString("fdswarm.broadcastAddress")
  private val ignoreSelf = if config.hasPath("fdswarm.discovery.ignoreSelf") then config.getBoolean("fdswarm.discovery.ignoreSelf") else true
  private var activeQueue:Option[LinkedBlockingQueue[ContestConfig]] = None

  private var socket: Option[DatagramSocket] = None

  private lazy val myAddresses: Set[InetAddress] =
    NetworkInterface.getNetworkInterfaces.asScala
      .flatMap(_.getInetAddresses.asScala)
      .toSet

  private val receiverThread = new Thread(() =>
    logger.info(s"DiscoveryService receiver listening on port $discoveryPort")

    val buffer = new Array[Byte](65535)
    while !Thread.currentThread().isInterrupted do
      try
        socket.foreach { s =>
          val packet = new DatagramPacket(buffer, buffer.length)
          s.receive(packet)
          val sender = packet.getAddress
          if ignoreSelf && myAddresses.contains(sender) then
            logger.debug(s"Ignoring our own message from $sender")
          else
            val receivedData: Array[Byte] = new Array[Byte](packet.getLength)
            System.arraycopy(packet.getData, packet.getOffset, receivedData, 0, packet.getLength)
            UDPHeader.parse(receivedData) match
              case UDPHeaderData(Service.Discovery, _) =>
                val jsonBytes = writeToByteArray(contestManager.config)
                logger.debug("Received Discovery request from {} current ContestCOnfig", sender)
                send(UDPHeader(Service.Discovered, jsonBytes))
              case UDPHeaderData(Service.Discovered, jsonPayload) =>
                activeQueue.foreach { queue =>
                  val contestConfig = read[ContestConfig](jsonPayload)
                  queue.offer(contestConfig)
                }
              case x =>
                logger.warn(s"Received unknown message from $sender: $x")
        }
      catch
        case _: InterruptedException => Thread.currentThread().interrupt()
        case e: Exception =>
          if socket.exists(!_.isClosed) then
            logger.error("Error receiving UDP packet in DiscoveryService", e)
  , "DiscoveryService-Receiver")


  receiverThread.setDaemon(true)

  private def send(array: Array[Byte]): Unit =
    socket.foreach(s =>
      val address = InetAddress.getByName(broadcastAddress)
      val packet = new DatagramPacket(array, array.length, address, discoveryPort)
      s.send(packet)
    )

  def start(): Unit =
    socket = Some {
      val s = new DatagramSocket(null)
      s.setReuseAddress(true)
      s.bind(new InetSocketAddress(discoveryPort))
      s
    }

    receiverThread.start()

  def stop(): Unit =
    logger.info("Stopping DiscoveryService")
    receiverThread.interrupt()
    socket.foreach(_.close())
    socket = None

  def discover(): Seq[ContestConfig] =
    logger.debug(s"Sending Discovery request waiting for $timeoutMs ms")
    activeQueue = Option(new LinkedBlockingQueue[ContestConfig]())
    try
      val bytes = UDPHeader(Service.Discovery)
      send(bytes)

      activeQueue.map { queue =>
        val entry: ContestConfig = queue.poll(timeoutMs, TimeUnit.MILLISECONDS)
        if entry != null then Seq(entry) else Seq.empty
      }.getOrElse(Seq.empty)
    catch
      case _: InterruptedException => Seq.empty
    finally
      activeQueue = None
