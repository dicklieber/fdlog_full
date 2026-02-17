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

import fdswarm.store.FdHourDigest
import fdswarm.util.HostAndPort
import upickle.default.*

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

case class StatusMessage(hostAndPort: HostAndPort, fdDigests:Seq[FdHourDigest]) derives ReadWriter:
  def toPacket: Array[Byte] =
    val json = write(this)
    val baos = new ByteArrayOutputStream()
    val gzos = new GZIPOutputStream(baos)
    try
      gzos.write(json.getBytes("UTF-8"))
    finally
      gzos.close()
    baos.toByteArray

  override def toString: String =
    s"StatusMessage(hostAndPort: $hostAndPort, fdDigests:${fdDigests.size} gzipSize: ${toPacket.length} bytes.)"

object StatusMessage:
  def apply(gzipped: Array[Byte]): StatusMessage =
    val bais = new ByteArrayInputStream(gzipped)
    val gzis = new GZIPInputStream(bais)
    try
      val json = new String(gzis.readAllBytes(), "UTF-8")
      read[StatusMessage](json)
    finally
      gzis.close()
   
