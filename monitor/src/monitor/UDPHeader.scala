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

import com.codahale.metrics.SharedMetricRegistries
import fdswarm.util.NodeIdentity
import io.circe.Decoder
import org.slf4j.LoggerFactory

import java.net.DatagramPacket
import java.nio.charset.StandardCharsets
import scala.util.Try


//  def decode(
//    udpHeaderData: UDPHeaderData
//  ): T =
//    udpHeaderData.decodePayload[T](
//      using payloadDecoder
//    )

final case class NoPayload()

object NoPayload:
  given Decoder[NoPayload] = Decoder.const(
    NoPayload()
  )



/**
 *
 * Node identity used in the cluster.
 * e.g. "fdswarm|Status|8078-s123232131)|0\n"
 *
 * | Field | Type | Description |
 * |------------|--------|------------------------------------------|
 * | `fdswarm | String | the fdswarm app, helps in WireShark |
 * | `service | String | from [[fdswarm.replication.Service]]                   |
 * | `port | String | TCP Port and instnaceID
 * | \n | end of line | marks end of header.  |
 *
 *
 */
object UDPHeader:
  private val logger = LoggerFactory.getLogger(getClass)
  private val metricRegistry = SharedMetricRegistries.getOrCreate(
    "default"
  )
  private val outgoingPayloadSizeBytes = metricRegistry.histogram(
    "udp_outgoing_payload_size_bytes"
  )
  private val outgoingPacketSizeBytes = metricRegistry.histogram(
    "udp_outgoing_packet_size_bytes"
  )
  private val incomingPayloadSizeBytes = metricRegistry.histogram(
    "udp_incoming_payload_size_bytes"
  )
  private val incomingPacketSizeBytes = metricRegistry.histogram(
    "udp_incoming_packet_size_bytes"
  )
  private val headerRegx =
    """^FDSWARM\|([^|]+)\|([^|]+)\|(\d+)\|$""".r

//  val address: InetAddress = packet.getAddress

  /**
   * Parses a UDP packet into a [[UDPHeaderData]].
   *
   * @param packet received.
   * @return Option[UDPHeaderData] None if the packet is from the local instance.
   */
  @throws[IllegalArgumentException]("if packet is invalid")
  def parse(packet: DatagramPacket): NodeInfo =
      val data: Array[Byte] = packet.getData.take(packet.getLength)
      incomingPacketSizeBytes.update(
        data.length.toLong
      )
      val newlineIndex = data.indexOf('\n'.toByte)
      if (newlineIndex == -1) throw new IllegalArgumentException("Invalid packet: no newline found")

      val headerBytes = data.take(newlineIndex)
      val headerStr = new String(headerBytes, "UTF-8")
      val payloadBytes = data.drop(newlineIndex + 1) // after newline
      incomingPayloadSizeBytes.update(
        payloadBytes.length.toLong
      )

      headerStr match
        case headerRegx(sService, udpPiece, sDataVersion) =>
          val address = packet.getAddress
          val nodeIdentity = NodeIdentity.fromUdpHeader(address, udpPiece)
          NodeInfo(sService, data, nodeIdentity)
//          val nodeIdentity= NodeIdentity.fromUdpHeader(address, udpPiece)
//          UDPHeaderData(Service.valueOf(sService), nodeIdentity, payloadBytes)
        case _ =>
          throw new IllegalArgumentException(s"Invalid header format: $headerStr")
  
