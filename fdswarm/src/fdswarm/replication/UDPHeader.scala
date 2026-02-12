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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.GZIPInputStream
import scala.util.Try

case class UDPHeaderData(service: Service, jsonPayload: String)

object UDPHeader:
  private val headerRegx = s"""^FDSWARM\\|(${Service.values.map(_.toString).mkString("|")})\\|(\\d+)\\|$$""".r

  def apply(service: Service, payload: Array[Byte] = Array.emptyByteArray): Array[Byte] =
    val header = s"FDSWARM|$service|${BuildInfo.dataVersion}|\n"
    val headerBytes = header.getBytes("UTF-8")
    val result = new Array[Byte](headerBytes.length + payload.length)
    System.arraycopy(headerBytes, 0, result, 0, headerBytes.length)
    System.arraycopy(payload, 0, result, headerBytes.length, payload.length)
    result

  /**
   * Parses a UDP packet into a UDPHeaderData.
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

          val jsonString = if isGzip(payloadBytes) then
            ungzip(payloadBytes)
          else
            new String(payloadBytes, "UTF-8")

          UDPHeaderData(service, jsonString)
        case _ =>
          throw new IllegalArgumentException(s"Invalid header format: $headerStr")
    }.get

  private def isGzip(data: Array[Byte]): Boolean =
    data.length >= 2 &&
      (data(0) & 0xFF) == (GZIPInputStream.GZIP_MAGIC & 0xFF) &&
      (data(1) & 0xFF) == (GZIPInputStream.GZIP_MAGIC >> 8 & 0xFF)

  private def ungzip(data: Array[Byte]): String =
    val bais = new ByteArrayInputStream(data)
    val gzis = new GZIPInputStream(bais)
    val baos = new ByteArrayOutputStream()
    val buffer = new Array[Byte](1024)
    var len = gzis.read(buffer)
    while len > 0 do
      baos.write(buffer, 0, len)
      len = gzis.read(buffer)
    new String(baos.toByteArray, "UTF-8")

enum Service:
  case Discovery, Discovered, Status
  