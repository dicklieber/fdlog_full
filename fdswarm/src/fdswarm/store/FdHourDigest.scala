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

package fdswarm.store

import fdswarm.model.{FdHour, Qso}
import upickle.default.*

/**\
 *
 *
 * @param fdHour for when.
 * @param count number of QSOs.
 * @param digest based on the [[Id]]s of the QSOs.
 */
case class FdHourDigest(fdHour: FdHour, count: Int, digest: String) derives ReadWriter

object FdHourDigest:
  def apply(fdHour: FdHour, qsos: Seq[Qso]): FdHourDigest =
    val sortedIds = qsos.map(_.uuid).sorted.mkString
    val digest = java.security.MessageDigest.getInstance("MD5")
      .digest(sortedIds.getBytes("UTF-8"))
      .map("%02x".format(_)).mkString
    FdHourDigest(fdHour, qsos.size, digest)
