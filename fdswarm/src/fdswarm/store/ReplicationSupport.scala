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
import fdswarm.StartupInfo
import fdswarm.io.DirectoryProvider
import fdswarm.replication.status.SwarmData
import fdswarm.replication.{LocalNodeStatus, Transport}
import fdswarm.telemetry.Metrics
import fdswarm.util.Ids.Id
import jakarta.inject.{Inject, Provider, Singleton}

/**
 * Adds methods to [[QsoStore]] that are needed for replication.
 * Having them here keeps [[QsoStore]] clean.
 */
@Singleton
class ReplicationSupport @Inject()(directoryProvider: DirectoryProvider,
                                   otelMetrics: Metrics,
                                   transport: Transport,
                                   swarmDataProvider: Provider[SwarmData],
                                   startupInfo: StartupInfo,
                                   filenameStamp: fdswarm.util.FilenameStamp,
                                   localNodeStatus: LocalNodeStatus
                                  )
  extends QsoStore(directoryProvider, transport, swarmDataProvider, startupInfo, filenameStamp, localNodeStatus):
//  /**
//   *
//   * @param fdHourDigest from a remote node
//   * @return FdHours that need to be replicated.
//   */
//  def isFdHourNeeded(fdHourDigest: FdHourDigest): Option[FdHour] =
//    val fdHour = fdHourDigest.fdHour
//    internalDigests.get(fdHour) match
//      case Some(haveDigest) =>
//        Option.when(haveDigest != fdHourDigest){fdHour}
//      case None =>
//        // If we don't have the FdourDIgest]], we need it.
//        Option(fdHour)


//  def idsForHour(fdHour: FdHour): IO[FdHourIds] =
//    IO {
//      val ids = all.filter(_.fdHour == fdHour).map(_.uuid)
//      FdHourIds(fdHour, ids)
//    }

  private def doWeHaveQso(uuid:Id): IO[Boolean] =
    IO(map.contains(uuid))

//  def missingIds(remote: FdHourIds): IO[Seq[Id]] =
//    remote.ids.filterA { id =>
//      doWeHaveQso(id).map(!_)
//    }

  def addQsos(qsos: Seq[fdswarm.model.Qso]): IO[Unit] =
    IO {
      add(qsos)
    }

