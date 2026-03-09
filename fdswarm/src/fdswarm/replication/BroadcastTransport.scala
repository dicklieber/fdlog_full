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

import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging
import fdswarm.util.NodeIdentityManager
import jakarta.inject.{Inject, Singleton}

import java.net.{
  DatagramPacket,
  DatagramSocket,
  InetAddress,
  InetSocketAddress,
  NetworkInterface
}
import java.util.concurrent.LinkedBlockingQueue
import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*

@Singleton
class BroadcastTransport @Inject() (
                                     @Named("fdswarm.UDP.port") port: Int,
                                     val nodeIdentityManager: NodeIdentityManager,
                                   ) extends Transport with LazyLogging:

  logger.debug("Starting BroadcastTransport on port {}", port)

  override val mode: String = "Broadcast"
  val queue = new LinkedBlockingQueue[UDPHeaderData]()
  private val listeners = new java.util.concurrent.CopyOnWriteArrayList[UDPHeaderData => Unit]()

  def addListener(listener: UDPHeaderData => Unit): Unit = listeners.add(listener)
  def removeListener(listener: UDPHeaderData => Unit): Unit = listeners.remove(listener)

  private var socket: DatagramSocket = uninitialized
  private var thread: Thread = uninitialized

  // Start receiver in constructor
  try
    socket = new DatagramSocket(null)
    socket.setReuseAddress(true)
    socket.setBroadcast(true)
    socket.bind(new InetSocketAddress("0.0.0.0", port))

    thread = new Thread(
      () =>
        val buffer = new Array[Byte](65535)

        while !Thread.currentThread().isInterrupted do
          try
            val packet: DatagramPacket = new DatagramPacket(buffer, buffer.length)
            socket.receive(packet)
            val senderAddr = packet.getAddress
            val senderPort = packet.getPort
            logger.trace(
              s"Received UDP packet from $senderAddr:$senderPort, length ${packet.getLength}"
            )

            try
              UDPHeader.parse(packet) match
                case Some(udpHeader) if !isUs(udpHeader.nodeIdentity) =>
                  logger.trace(
                    s"Received UDP packet from $senderAddr:$senderPort: ${udpHeader.service}"
                  )
                  listeners.forEach(_.apply(udpHeader))
                  queue.offer(udpHeader)
                case Some(_) =>
                  logger.trace("Ignoring our own message from {}", senderPort)
                case None =>
                  // Should not happen as UDPHeader.parse returns Some or throws
                  logger.warn("Received empty UDP packet from $senderAddr:$senderPort")

            catch
              case e: IllegalArgumentException =>
                logger.error(
                  s"Received invalid UDP packet from $senderAddr:$senderPort: ${e.getMessage}"
                )

          catch
            case _: InterruptedException =>
              Thread.currentThread().interrupt()

            case e: java.net.SocketException if socket != null && socket.isClosed =>
              Thread.currentThread().interrupt()

            case e: Exception =>
              if socket != null && !socket.isClosed then
                logger.error(
                  s"Error in BroadcastTransport receiver: ${e.getMessage}",
                  e
                )
      ,
      "Broadcast-Receiver"
    )

    thread.setDaemon(true)
    thread.start()

  catch
    case e: Exception =>
      logger.error(
        s"Failed to start BroadcastTransport receiver: ${e.getMessage}",
        e
      )
      stop()

  def send(data: Array[Byte]): Unit =
    send(Service.QSO, data)

  def send(service: Service, data: Array[Byte]): Unit =
    val packetBytes = UDPHeader(service, nodeIdentityManager.portAndInstance, data)
    val broadcastAddr = InetAddress.getByName("255.255.255.255")
    val packet =
      new DatagramPacket(packetBytes, packetBytes.length, broadcastAddr, port)
    socket.send(packet)

  def stop(): Unit =
    logger.debug("Stopping BroadcastTransport")

    if socket != null then
      try
        if !socket.isClosed then socket.close()
      catch
        case e: Exception =>
          logger.debug(s"Error while closing DatagramSocket: ${e.getMessage}")
      finally socket = null

    if thread != null then
      thread.interrupt()
      thread = null
