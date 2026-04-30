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

import fdswarm.logging.Locus.Replication
import fdswarm.logging.{LazyStructuredLogging, Locus}
import fdswarm.util.{NodeIdentityManager, StatsSource}
import jakarta.inject.{Inject, Singleton}

import java.net.{DatagramPacket, DatagramSocket, InetAddress, InetSocketAddress}
import scala.compiletime.uninitialized

/**
  * Broadcasts UDP packets to all nodes in the network. Receive any UDP packets from all nodes in
  * the network. Consumers read from the shared incoming queue exposed by [[Transport]].
  * @param nodeIdentity doesnt' need this directly but needs to run to get
  *   [[fdswarm.util.NodeIdentityManager.nodeIdentity]]
  */
@Singleton
class BroadcastTransport @Inject() (nodeIdentity: NodeIdentityManager)
    extends Transport
    with StatsSource(Locus.UDP)
    with Runnable
    with LazyStructuredLogging(Replication):

  logger.info("Starting BroadcastTransport")

  override val mode: String = "Broadcast"
  val buffer = new Array[Byte](65535)

  val port = 8090
  val thread = new Thread(this, "Broadcast-Receiver")

  private val sendMeter = addMeter("sendMeter")
  private val receiveMeter = addMeter("receiveMeter")

  private var socket: DatagramSocket = uninitialized
  socket = new DatagramSocket(null)
  socket.setReuseAddress(true)
  socket.setBroadcast(true)
  socket.bind(new InetSocketAddress("0.0.0.0", port))

  thread.setDaemon(true)
  thread.start()

  override def run(): Unit =
    while !Thread.currentThread().isInterrupted do
      val packet: DatagramPacket = new DatagramPacket(buffer, buffer.length)
      logger.trace(s"Waiting for a UDP packet")
      socket.receive(packet)
      receiveMeter.mark()

      val senderAddr = packet.getAddress
      val senderPort = packet.getPort
      logger.trace(s"Received a UDP packet")
      val udpHeaderData = UDPHeader.parse(packet)
      if NodeIdentityManager.isUs(udpHeaderData.nodeIdentity) then
        logger.trace(s"Received UDP packet from $senderAddr:$senderPort: ${udpHeaderData.service}")
      else
        logger.trace(s"Received UDP packet from $senderAddr:$senderPort: ${udpHeaderData.service}")
        incomingQueue.offer(udpHeaderData)

  def send(data: Array[Byte]): Unit =
    send(Service.QSO, data)

  def send(
      service: Service[?],
      data: Array[Byte]
  ): Unit =
    try
      val packetBytes = UDPHeader(service, NodeIdentityManager.nodeIdentity, data)
      val broadcastAddr = InetAddress.getByName("255.255.255.255")
      val packet =
        new DatagramPacket(packetBytes, packetBytes.length, broadcastAddr, port)
      socket.send(packet)
      sendMeter.mark()

    catch
      case e: Exception =>
        logger.error("Send", e)

  def stop(): Unit =
    logger.debug("Stopping BroadcastTransport")

    if socket != null then
      try
        if !socket.isClosed then socket.close()
      catch
        case e: Exception =>
          logger.debug(s"Error while closing DatagramSocket: ${e.getMessage}")
      finally socket = null
