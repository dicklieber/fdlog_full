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
import fdswarm.io.DirectoryProvider
import fdswarm.replication.StatusMessage
import fdswarm.util.Ids.Id
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.{Inject, Singleton}

/**
 * Adds methods to [[QsoStore]] that are needed for replication.
 * Having them here keeps [[QsoStore]] clean.
 */
@Singleton
class ReplicationSupport @Inject()(directoryProvider: DirectoryProvider, registry: MeterRegistry) extends QsoStore(directoryProvider, registry):

  def handleStatusMessage(status: StatusMessage): IO[Seq[FdHourIds]] =
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

    import cats.implicits.*
    neededFdHours.traverse(idsForHour)

  def idsForHour(fdHour: FdHour): IO[FdHourIds] =
    IO {
      val ids = all.filter(_.fdHour == fdHour).map(_.uuid)
      FdHourIds(fdHour, ids)
    }

  def missingIds(remote: FdHourIds): IO[Seq[Id]] =
    idsForHour(remote.fdHour).map { local =>
      val localIds = local.ids.toSet
      remote.ids.filterNot(localIds.contains)
    }

  def addQsos(qsos: Seq[fdswarm.model.Qso]): IO[Unit] =
    IO {
      add(qsos)
    }

  def qsosForFdHour(fdHour: FdHour): IO[Seq[fdswarm.model.Qso]] =
    IO {
      all.filter(_.fdHour == fdHour)
    }

  def qsosForIds(request: FdHourRequest): IO[FdHourQsos] =
    qsosForFdHour(request.fdHour).map { qsos =>
      val filteredQsos = if (request.specificQsos.isEmpty) {
        qsos
      } else {
        val ids = request.specificQsos.toSet
        qsos.filter(qso => ids.contains(qso.uuid))
      }
      FdHourQsos(request.fdHour, filteredQsos)
    }
