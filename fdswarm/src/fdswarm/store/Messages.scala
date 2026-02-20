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

import fdswarm.fx.qso.FdHour
import fdswarm.model.Qso
import fdswarm.util.Ids.Id
import io.circe.Codec

/**
 * What gets broacasted to all nodes.
 *
 * @param fdHour for when.
 * @param count number of QSOs.
 * @param digest based on the [[Id]]s of the QSOs.
 */
case class FdHourDigest(fdHour: FdHour, count: Int, digest: String ) extends Ordered[FdHourDigest] derives  Codec.AsObject:
  override def compare(that: FdHourDigest): Int = this.fdHour.compare(that.fdHour)

object FdHourDigest:
  def empty(fdHour: FdHour): FdHourDigest =
    apply(fdHour, Seq.empty)
    
  def apply(fdHour: FdHour, qsos: Seq[Qso]): FdHourDigest =
    val sortedIds = qsos.map(_.uuid).sorted.mkString
    val digest = java.security.MessageDigest.getInstance("MD5")
      .digest(sortedIds.getBytes("UTF-8"))
      .map("%02x".format(_)).mkString
    FdHourDigest(fdHour, qsos.size, digest)

/**
 * Can be sent to an FdSwarm node to get some or all of the QSOs for a given hour.
 * @param fdHour for when.
 * @param specificQsos what we need. If [[Seq.empty]], all QSOs for the given hour are returned.
 */
case class FdHourRequest(fdHour: FdHour, specificQsos: Seq[Id] = Seq.empty) derives  Codec.AsObject

/**
 * @param fdHour for when
 * @param ids QSOs for the given hour, the the node has or that a node needs.
 */
case class FdHourIds(fdHour: FdHour, ids:Seq[Id]) derives  Codec.AsObject

case class FdHourQsos(fdHour: FdHour, qsos:Seq[Qso]) derives  Codec.AsObject