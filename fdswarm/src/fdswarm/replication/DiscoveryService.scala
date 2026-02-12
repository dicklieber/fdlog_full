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

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import jakarta.inject.Inject

import java.net.{DatagramPacket, DatagramSocket, InetAddress}
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable.ListBuffer

class DiscoveryService @Inject() (config: Config) extends LazyLogging:

  private val broadcastPort = if config.hasPath("fdswarm.broadcastPort") then config.getInt("fdswarm.broadcastPort") else 2234
  private val broadcastAddress = if config.hasPath("fdswarm.broadcastAddress") then config.getString("fdswarm.broadcastAddress") else "255.255.255.255"
  private val discoveryTimeoutMs = if config.hasPath("fdswarm.discoveryTimeoutMs") then config.getInt("fdswarm.discoveryTimeoutMs") else 2000

  def discover(): Seq[String] =
    val socket = new DatagramSocket()
    val responses = ListBuffer.empty[String]
    try
      socket.setBroadcast(true)
      socket.setSoTimeout(discoveryTimeoutMs)
      
      val discoverMsg = "fdswarm|discover"
      val bytes = discoverMsg.getBytes("UTF-8")
      val packet = new DatagramPacket(
        bytes,
        bytes.length,
        InetAddress.getByName(broadcastAddress),
        broadcastPort
      )
      
      socket.send(packet)
      logger.debug(s"Sent discovery broadcast to $broadcastAddress:$broadcastPort")
      
      val receiveBuffer = new Array[Byte](1024)
      val receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length)
      
      val startTime = System.currentTimeMillis()
      var remainingTime = discoveryTimeoutMs
      
      while remainingTime > 0 do
        try
          socket.receive(receivePacket)
          val response = new String(receivePacket.getData, 0, receivePacket.getLength, "UTF-8")
          logger.debug(s"Received discovery response from ${receivePacket.getAddress}: $response")
          responses += response
        catch
          case _: java.net.SocketTimeoutException =>
            remainingTime = 0 // Break loop on timeout
          case e: Exception =>
            logger.error("Error receiving discovery response", e)
            remainingTime = 0

        if remainingTime > 0 then
          remainingTime = (discoveryTimeoutMs - (System.currentTimeMillis() - startTime)).toInt
          if remainingTime > 0 then
            socket.setSoTimeout(remainingTime)

    catch
      case e: Exception =>
        logger.error(s"Failed during discovery on $broadcastAddress:$broadcastPort", e)
    finally
      socket.close()
      
    responses.toSeq
