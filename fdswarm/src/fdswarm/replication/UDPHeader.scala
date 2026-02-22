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

import scala.util.Try

case class UDPHeaderData(service: Service, payload: Array[Byte])

/**
 * UDP Header format:
 * FDSWARM|SERVICE|VERSION|\n
 * JSON_PAYLOAD
 */
object UDPHeader:
  private val headerRegx = s"""^FDSWARM\\|(${Service.values.map(_.toString).mkString("|")})\\|(\\d+)\\|$$""".r

  /**
   * Creates a UDP packet from a Header.
   *
   * @param service what this data is about.
   * @param payload contents of the packet.
   * @return suitable for sending over UDP.
   */
  def apply(service: Service, payload: Array[Byte] = Array.emptyByteArray): Array[Byte] =
    val header = s"FDSWARM|$service|${BuildInfo.dataVersion}|\n"
    val headerBytes: Array[Byte] = header.getBytes("UTF-8")
    val result: Array[Byte] = headerBytes ++ payload
    result

  /**
   * Parses a UDP packet into a UDPHeaderData.
   *
   * @param packet received.
   * @return
   */
  @throws[IllegalArgumentException]("if packet is invalid")
  def parse(packet: Array[Byte]): UDPHeaderData =
    Try {
      val newlineIndex = packet.indexOf('\n'.toByte)
      if (newlineIndex == -1) throw new IllegalArgumentException("Invalid packet: no newline found")

      val headerStr = new String(packet.take(newlineIndex), "UTF-8")
      val payloadBytes = packet.drop(newlineIndex + 1)

      headerStr match
        case headerRegx(sService, dataVersion) =>
          if dataVersion != BuildInfo.dataVersion then
            throw new IllegalArgumentException(s"Data version mismatch: expected ${BuildInfo.dataVersion}, got $dataVersion")

          val service = Service.valueOf(sService)

          val payload = payloadBytes
          UDPHeaderData(service, payload)
        case _ =>
          throw new IllegalArgumentException(s"Invalid header format: $headerStr")
    }.get

enum Service:
  case Status, QSO
  