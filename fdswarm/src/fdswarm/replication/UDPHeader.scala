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
import fdswarm.fx.contest.ContestConfig
import fdswarm.model.Qso
import fdswarm.util.NodeIdentity
import fdswarm.util.Gzip
import io.circe.Decoder
import com.codahale.metrics.SharedMetricRegistries
import org.slf4j.LoggerFactory

import java.net.{DatagramPacket, InetAddress}
import java.nio.charset.StandardCharsets
import scala.util.Try

enum Service[T]:
  type Payload = T
  case Status extends Service[StatusMessage]
  case SendStatus extends Service[Unit]
  case QSO extends Service[Qso]
  case SyncContest extends Service[ContestConfig]
  case RestartContest extends Service[ContestConfig]

  def decode(
    udpHeaderData: UDPHeaderData
  ): T =
    this match
      case Service.Status =>
        udpHeaderData.decodePayload[StatusMessage]
      case Service.SendStatus =>
        ()
      case Service.QSO =>
        udpHeaderData.decodePayload[Qso]
      case Service.SyncContest =>
        udpHeaderData.decodePayload[ContestConfig]
      case Service.RestartContest =>
        udpHeaderData.decodePayload[ContestConfig]

case class UDPHeaderData(
  service: Service[?],
  nodeIdentity: NodeIdentity,
  payload: Array[Byte]
):

  def decodePayload[T](
    using Decoder[T]
  ): T =
    val decodedPayload = maybeDecompressGzip(payload)
    val jsonString = new String(decodedPayload, StandardCharsets.UTF_8)
    io.circe.parser.parse(jsonString) match
      case Left(error) => throw new RuntimeException(s"Failed to parse JSON: ${error.getMessage}", error)
      case Right(json) => json.as[T] match
        case Left(error) => throw new RuntimeException(s"Failed to decode JSON to T: ${error.getMessage}", error)
        case Right(value) => value

  def decodeFor[T](
    expectedService: Service[T]
  ): T =
    require(
      service == expectedService,
      s"Expected service $expectedService but got $service"
    )
    expectedService.decode(
      this
    )

  def decodeByService: service.Payload =
    service.decode(
      this
    )

  private def maybeDecompressGzip(
                                  input: Array[Byte]
                                ): Array[Byte] =
    if isGzip(input) then
      Try(
        Gzip.decompress(input)
      ).getOrElse(
        throw new RuntimeException(
          "Failed to decompress GZIP payload"
        )
      )
    else
      input

  private def isGzip(
                      input: Array[Byte]
                    ): Boolean =
    input.length >= 2 &&
      input(0) == 0x1f.toByte &&
      input(1) == 0x8b.toByte

/**
 *
 * Node identity used in the cluster.
 * e.g. "fdswarm|Status|8078-s123232131)|0\n"
 *
 * | Field      | Type   | Description                              |
 * |------------|--------|------------------------------------------|
 * | `fdswarm`     | String | the fdswarm app, helps in WireShark                   |
 * | `service`     | String | from [[fdswarm.replication.Service]]                   |
 * | `port`     | String    | TCP Port and instnaceID
 * | \n | end of line   | marks end of header.  |
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
  private val serviceNames: String = Service.values.map(_.toString).mkString("|")
  private val headerRegx =
    """^FDSWARM\|([^|]+)\|([^|]+)\|(\d+)\|$""".r
  /**
   * Constructs a byte array representing a UDP header along with an optional payload.
   * The header adheres to the format: `FDSWARM|SERVICE|NODE|DATAVERSION|.
   * e.g. "fdswarm|Status|10.10.10.10:8078(s123232131)|0"
   *
   * @param service      the service type, represented as an instance of the `Service` enum (e.g., Status or QSO).
   * @param nodeIdentity for the udpPiece.
   * @param payload      optional byte array representing additional data to append after the header, defaulting to an empty array.
   * @return a byte array combining the formatted header and the optional payload.
   */
  def apply(
    service: Service[?],
    nodeIdentity: NodeIdentity,
    payload: Array[Byte] = Array.emptyByteArray
  ): Array[Byte] =
    val header = s"FDSWARM|$service|${nodeIdentity.udpHeaderPiece}|${BuildInfo.dataVersion}|\n"
    val headerBytes: Array[Byte] = header.getBytes("UTF-8")
    val result: Array[Byte] = headerBytes ++ payload
    outgoingPayloadSizeBytes.update(
      payload.length.toLong
    )
    outgoingPacketSizeBytes.update(
      result.length.toLong
    )
    result

//  val address: InetAddress = packet.getAddress

  /**
   * Parses a UDP packet into a [[UDPHeaderData]].
   *
   * @param packet received.
   * @return Option[UDPHeaderData] None if the packet is from the local instance.
   */
  @throws[IllegalArgumentException]("if packet is invalid")
  def parse(packet: DatagramPacket): UDPHeaderData =
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
          if sDataVersion != BuildInfo.dataVersion then
            throw new IllegalArgumentException(s"Data version mismatch: expected ${BuildInfo.dataVersion}, got $sDataVersion")
          val address = packet.getAddress
          val nodeIdentity= NodeIdentity.fromUdpHeader(address, udpPiece)
          UDPHeaderData(Service.valueOf(sService), nodeIdentity, payloadBytes)
        case _ =>
          throw new IllegalArgumentException(s"Invalid header format: $headerStr")
  
