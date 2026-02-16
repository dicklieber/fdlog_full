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
import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging
import java.net.{DatagramPacket, DatagramSocket, InetAddress, NetworkInterface}
import java.util.concurrent.LinkedBlockingQueue
import scala.jdk.CollectionConverters.*

/**
 * Receives node status broadcasts from other nodes in the swarm.
 *
 * This service listens on the configured `statusPort` for UDP packets.
 * It parses incoming packets using [[UDPHeader]] and, if valid [[Service.Status]] messages,
 * offers the payload (decompressed JSON string converted to bytes) to the provided queue.
 *
 * @param statusPort UDP port to listen for status broadcasts
 * @param ignoreSelf whether to ignore broadcasts originating from this node
 * @param queue the destination queue for received status payloads
 */
@Singleton
class NodeStatusReceiver @Inject()(
    @Named("fdswarm.statusPort") statusPort: Int
) extends LazyLogging:

  val queue = new LinkedBlockingQueue[Array[Byte]]()

  private var socket: Option[DatagramSocket] = None
  private var thread: Option[Thread] = None

  private lazy val myAddresses: Set[InetAddress] =
    NetworkInterface.getNetworkInterfaces.asScala
      .flatMap(_.getInetAddresses.asScala)
      .toSet

  def start(): Unit =
    logger.info(s"Starting NodeStatus receiver on port $statusPort")
    val s = new DatagramSocket(statusPort)
    socket = Some(s)

    val t = new Thread(() =>
      val buffer = new Array[Byte](65535)
      while !Thread.currentThread().isInterrupted do
        try
          val packet = new DatagramPacket(buffer, buffer.length)
          s.receive(packet)
          val sender = packet.getAddress
          
          if myAddresses.contains(sender) then
            logger.debug(s"Ignoring our own status message from $sender")
          else
            val receivedData = new Array[Byte](packet.getLength)
            System.arraycopy(packet.getData, packet.getOffset, receivedData, 0, packet.getLength)
            
            try
              val headerData = UDPHeader.parse(receivedData)
              if headerData.service == Service.Status then
                val statusMessage = StatusMessage(headerData.payload)
                // For now, we still queue the raw payload bytes if Repl expects them, 
                // but let's check Repl.scala to see what it expects in the queue.
                queue.offer(headerData.payload)
                logger.trace(s"Received and queued status from $sender: ${headerData.payload.length} bytes")
              else
                logger.debug(s"Received non-status message on status port from $sender: ${headerData.service}")
            catch
              case e: IllegalArgumentException =>
                logger.debug(s"Failed to parse UDP packet from $sender: ${e.getMessage}")
        catch
          case _: InterruptedException => Thread.currentThread().interrupt()
          case e: Exception =>
            if !s.isClosed then
              logger.error("Error receiving node status", e)
    , "NodeStatus-Receiver")
    t.setDaemon(true)
    t.start()
    thread = Some(t)

  def stop(): Unit =
    logger.info("Stopping NodeStatus receiver")
    thread.foreach(_.interrupt())
    socket.foreach(_.close())
    thread = None
    socket = None
