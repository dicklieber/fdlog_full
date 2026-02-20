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

import cats.effect.IO
import cats.syntax.all.*
import com.typesafe.scalalogging.LazyLogging
import fdswarm.api.ReplEndpoints
import fdswarm.fx.qso.FdHour
import fdswarm.store.{FdHourRequest, ReplicationSupport}
import jakarta.inject.{Inject, Singleton}

/**
 * This is the logic that synchronizes the local QSO store with a remote node.
 *
 */
@Singleton
class StatusProcessor @Inject()(qsoStore: ReplicationSupport,
                                remoteEndpointCaller: RemoteEndpointCaller) extends LazyLogging:


  /**
   * Process an incoming status message: determine what FdHours are needed
   * and POST them to the remote node.
   *
   * @param status incoming status from a remote node
   * @return IO completing after the HTTP call finishes
   */
  def processStatus(status: StatusMessage): IO[Unit] =
    for
      neededIds <- qsoStore.handleStatusMessage(status) // IO[Seq[FdHourIds]]
      neededHours: Seq[FdHour] = neededIds.map(_.fdHour) // Seq[FdHour]
      remoteFdHourIds <- neededHours.toList.traverse: fdHour =>
        remoteEndpointCaller.callRemoteEndpoint(status.hostAndPort, ReplEndpoints.qsoIdsByHourPostDef, fdHour)
      _ <- remoteFdHourIds.traverse: remoteIds =>
        for
          missing <- qsoStore.missingIds(remoteIds)
          _ <- if missing.nonEmpty then
            val request = FdHourRequest(remoteIds.fdHour, missing)
            remoteEndpointCaller.callRemoteEndpoint(status.hostAndPort, ReplEndpoints.qsosForIdsDef, request).flatMap: response =>
              qsoStore.addQsos(response.qsos)
          else IO.unit
        yield ()
    yield ()
