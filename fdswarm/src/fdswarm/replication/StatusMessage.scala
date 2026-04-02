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

import fdswarm.fx.contest.ContestConfig
import fdswarm.model.BandModeOperator
import fdswarm.store.FdHourDigest
import fdswarm.util.Ids.Id
import fdswarm.util.{CirceGzip, Ids, NodeIdentity}
import io.circe.Codec

import java.time.Instant

case class StatusMessage(fdDigests: Seq[FdHourDigest],
                         bandNodeOperator: BandModeOperator,
                         id: Id = Ids.generateId(),
                         contestConfig: ContestConfig) derives Codec.AsObject:
  def toPacket: Array[Byte] = CirceGzip.encode(this)

  override def toString: String =
    s"StatusMessage( $bandNodeOperator fdDigests:${fdDigests.size} gzipSize: ${toPacket.length} bytes.)"

object StatusMessage:
  def apply(gzipped: Array[Byte]): StatusMessage =
    CirceGzip.decode[StatusMessage](gzipped).toTry.get
   
class NodeStatus(val lastStatusMessage:StatusMessage, val statusReceievdCount:Int, val lastStatusReceived:Instant)