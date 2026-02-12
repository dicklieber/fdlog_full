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

import fdswarm.model.{FdHour, Qso}
import fdswarm.store.QsoStore
import jakarta.inject.Inject
import upickle.default.*
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPOutputStream
import scala.collection.immutable

class Repl @Inject()(qsoStore: QsoStore):

  def byFdHour: Seq[FdHourDigest] =

    val hourToQsos: Map[FdHour, Seq[Qso]] = qsoStore.all.groupBy(_.fdHour)
    val r: Iterable[FdHourDigest] = for
      (fdHour, qsos) <- hourToQsos
    yield
      val sortedIds = qsos.map(_.uuid).sorted.mkString
      val digest = java.security.MessageDigest.getInstance("MD5")
        .digest(sortedIds.getBytes("UTF-8"))
        .map("%02x".format(_)).mkString
      FdHourDigest(fdHour, qsos.size, digest)
    r.toSeq

  def byFdHourJsonGzip: Array[Byte] =
    val json = write(byFdHour)
    val baos = new ByteArrayOutputStream()
    val gzos = new GZIPOutputStream(baos)
    try
      gzos.write(json.getBytes("UTF-8"))
    finally
      gzos.close()
    baos.toByteArray

  def byFdHourJsonGzipBase64: String =
    Base64.getEncoder.encodeToString(byFdHourJsonGzip)
    

case class FdHourDigest(fdHour: FdHour, count: Int, digest: String) derives ReadWriter