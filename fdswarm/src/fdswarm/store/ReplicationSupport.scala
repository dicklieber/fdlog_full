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

import cats.effect.IO
import fdswarm.fx.qso.FdHour
import fdswarm.replication.StatusMessage

/**
 * Adds methods to [[QsoStore]] that are needed for replication.
 * Having them here keeps [[QsoStore]] clean.
 */
trait ReplicationSupport:
  self: QsoStore =>

  def determineNeeded(status: StatusMessage): IO[Seq[FdHourIds]] =
    IO {
      val incoming = status.fdDigests
      val cpy: Map[FdHour, FdHourDigest] = internalDigests
      val neededFdHours = incoming.flatMap { remoteFdHourDigest =>
        val remoteFdHour = remoteFdHourDigest.fdHour
        cpy.get(remoteFdHour) match
          // we have one, is it the same?
          case Some(localFdDigest) =>
            Option.when(localFdDigest != remoteFdHourDigest) {
              remoteFdHour
            }
          case None => // we don't have it yet, so we need it.
            Some(remoteFdHour)
      }
      neededFdHours.map(idsForHour)
    }
