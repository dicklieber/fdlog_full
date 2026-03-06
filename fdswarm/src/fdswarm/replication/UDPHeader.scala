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
import fdswarm.util.Ids.Id
import fdswarm.util.{Ids, NodeIdentity, PortAndInstance}
import org.slf4j.LoggerFactory

import java.net.DatagramPacket
import scala.util.Try

case class UDPHeaderData(service: Service, nodeIdentity: NodeIdentity, payload: Array[Byte])

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
  private val serviceNames: String = Service.values.map(_.toString).mkString("|")
  private val headerRegx =
    """^FDSWARM\|([^|]+)\|([^|]+)\|(\d+)\|$""".r
  /**
   * Constructs a byte array representing a UDP header along with an optional payload.
   * The header adheres to the format: `FDSWARM|SERVICE|NODE|DATAVERSION|.
   * e.g. "fdswarm|Status|10.10.10.10:8078(s123232131)|0"
   *
   * @param service      the service type, represented as an instance of the `Service` enum (e.g., Status or QSO).
   * @param portAndInstance just the port and instanceID ultimately will get the host from the [[DatagramPacket]].
   * @param payload      optional byte array representing additional data to append after the header, defaulting to an empty array.
   * @return a byte array combining the formatted header and the optional payload.
   */
  def apply(service: Service, portAndInstance: PortAndInstance, payload: Array[Byte] = Array.emptyByteArray): Array[Byte] =
    val header = s"FDSWARM|$service|$portAndInstance|${BuildInfo.dataVersion}|\n"
    val headerBytes: Array[Byte] = header.getBytes("UTF-8")
    val result: Array[Byte] = headerBytes ++ payload
    result

  /**
   * Parses a UDP packet into a UDPHeaderData.
   *
   * @param packet received.
   * @return Option[UDPHeaderData] None if the packet is from the local instance.
   */
  @throws[IllegalArgumentException]("if packet is invalid")
  def parse(packet: DatagramPacket): Option[UDPHeaderData] =
    Try {
      val data: Array[Byte] = packet.getData.take(packet.getLength)
      val newlineIndex = data.indexOf('\n'.toByte)
      if (newlineIndex == -1) throw new IllegalArgumentException("Invalid packet: no newline found")

      val headerBytes = data.take(newlineIndex)
      val headerStr = new String(headerBytes, "UTF-8")
      val payloadBytes = data.drop(newlineIndex + 1) // after newline

      headerStr match
        case headerRegx(sService, sPortAndInstance, sDataVersion) =>
          if sDataVersion != BuildInfo.dataVersion then
            throw new IllegalArgumentException(s"Data version mismatch: expected ${BuildInfo.dataVersion}, got $sDataVersion")
          val hostAddress = packet.getAddress.getHostAddress
          val portAndInstance: PortAndInstance = PortAndInstance.fromString(sPortAndInstance)
          val nodeIdentity= NodeIdentity(hostAddress, portAndInstance.port, portAndInstance.instanceId)
          
          Option.when(nodeIdentity.notUs) {
            UDPHeaderData(Service.valueOf(sService), nodeIdentity, payloadBytes)
          }
        case _ =>
          throw new IllegalArgumentException(s"Invalid header format: $headerStr")
    }.get

enum Service:
  case Status, QSO, DiscReq, DiscResponse
  