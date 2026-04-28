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

package monitor

import com.google.inject.{Inject, Singleton}
import com.typesafe.config.Config
import fdswarm.logging.LazyStructuredLogging

import java.net.*
import java.nio.channels.DatagramChannel
import java.util.concurrent.LinkedBlockingQueue
import scala.compiletime.uninitialized
import scala.util.control.NonFatal

@Singleton
final class UdpPacketListener @Inject() (config: Config)
    extends Runnable
    with LazyStructuredLogging:

  val incomingQueue: LinkedBlockingQueue[NodeInfo] =
    new LinkedBlockingQueue[NodeInfo]()
  private val port : Int = 8090

  @volatile
  private var stopped = false
  private var socket: DatagramSocket = uninitialized

  private val thread = new Thread(this, "Monitor-UDP-Listener")

  socket = openReusableSocket()

  thread.setDaemon(true)
  thread.start()

  logger.info(s"Monitor UDP listener started on port $port")

  override def run(): Unit =
    while !stopped && !Thread.currentThread().isInterrupted do
      val buffer = new Array[Byte](UdpPacketListener.MaxPacketSize)
      val datagram = new DatagramPacket(buffer, buffer.length)
      try
        logger.info(s"Waiting for UDP packet on port $port")
        socket.receive(datagram)
        val data = datagram.getData.slice(
          datagram.getOffset,
          datagram.getOffset + datagram.getLength
        )
        logger.info(s"Received UDP packet on port $port: ${data.mkString(",")}")
        val packet = UDPHeader.parse(datagram)
        incomingQueue.offer(
          packet

        )
      catch
        case _: SocketException if stopped =>
          Thread.currentThread().interrupt()
        case e: SocketException =>
          stopped = true
          logger.error(s"UDP listener socket closed unexpectedly on port $port", e)
        case _: InterruptedException =>
          Thread.currentThread().interrupt()
        case NonFatal(e) =>
          logger.error(s"Error receiving UDP packet on port $port", e)

  def stop(): Unit =
    stopped = true
    if socket != null && !socket.isClosed then socket.close()
    thread.interrupt()

  private def openReusableSocket(): DatagramSocket =
    val channel = DatagramChannel.open()
    channel.setOption(StandardSocketOptions.SO_REUSEADDR, true)
    enableReusePort(channel)

    val datagramSocket = channel.socket()
    datagramSocket.setReuseAddress(true)
    datagramSocket.bind(new InetSocketAddress("0.0.0.0", port))
    datagramSocket

  private def enableReusePort(channel: DatagramChannel): Unit =
    try channel.setOption(StandardSocketOptions.SO_REUSEPORT, true)
    catch
      case _: UnsupportedOperationException =>
        logger.debug("SO_REUSEPORT is not available for the monitor UDP socket")
      case _: IllegalArgumentException =>
        logger.debug("SO_REUSEPORT is not available for the monitor UDP socket")

object UdpPacketListener:
  private val MaxPacketSize = 65535

