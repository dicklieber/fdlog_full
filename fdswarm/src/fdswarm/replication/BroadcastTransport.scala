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

import fdswarm.logging.LazyStructuredLogging
import fdswarm.util.NodeIdentityManager
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.{Inject, Singleton}

import java.net.{DatagramPacket, DatagramSocket, InetAddress, InetSocketAddress}
import scala.compiletime.uninitialized

/**
 * Broadcasts UDP packets to all nodes in the network.
 * Receive any UDP packets from all nodes in the network.
 * Consumers read from the shared incoming queue exposed by [[Transport]].
 * @param nodeIdentityManager who we are.
 * @param meterRegistry for metrics,
 */
@Singleton
class BroadcastTransport @Inject() (
                                     val nodeIdentityManager: NodeIdentityManager,
                                     meterRegistry: MeterRegistry
                                   ) extends Transport with Runnable with LazyStructuredLogging:

  logger.info("Starting BroadcastTransport")

  override val mode: String = "Broadcast"

  private val sendCounter = meterRegistry.counter("fdswarm_sent_packets_total", "mode", mode)
  private var lastPacketBytes: Int = 0
  meterRegistry.gauge("fdswarm_sent_packet_bytes", this, (bt: BroadcastTransport) => bt.lastPacketBytes.toDouble)
  val buffer = new Array[Byte](65535)

  private var socket: DatagramSocket = uninitialized
  val port = 8090
  socket = new DatagramSocket(null)
  socket.setReuseAddress(true)
  socket.setBroadcast(true)
  socket.bind(new InetSocketAddress("0.0.0.0", port))

  val thread = new Thread(this, "Broadcast-Receiver")

  thread.setDaemon(true)
  thread.start()

  private val sentCounter = new java.util.concurrent.atomic.LongAdder()
  override def sentCount: Long = sentCounter.sum()

  override def run(): Unit =
    while !Thread.currentThread().isInterrupted do
      val packet: DatagramPacket = new DatagramPacket(buffer, buffer.length)
      logger.trace(s"Waiting for a UDP packet")
      socket.receive(packet)
      val senderAddr = packet.getAddress
      val senderPort = packet.getPort
      logger.trace(s"Received a UDP packet")
      val udpHeaderData = UDPHeader.parse(packet)
      if isUs(udpHeaderData.nodeIdentity) then
        logger.trace(s"Received UDP packet from $senderAddr:$senderPort: ${udpHeaderData.service}")
      else
        logger.trace(s"Received UDP packet from $senderAddr:$senderPort: ${udpHeaderData.service}")
        incomingQueue.offer(udpHeaderData)

  def send(data: Array[Byte]): Unit =
    send(Service.QSO, data)

  def send(service: Service, data: Array[Byte]): Unit =
    try
      val packetBytes = UDPHeader(service, nodeIdentityManager.ourNodeIdentity, data)
      lastPacketBytes = packetBytes.length
      val broadcastAddr = InetAddress.getByName("255.255.255.255")
      val packet =
        new DatagramPacket(packetBytes, packetBytes.length, broadcastAddr, port)
      socket.send(packet)
      sentCounter.increment()
      sendCounter.increment()
    catch
      case e: Exception  =>
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
