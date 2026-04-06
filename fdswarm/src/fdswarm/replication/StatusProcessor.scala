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
import fdswarm.replication.status.NodeBandOpPane
import fdswarm.store.{FdHourIds, FdHourRequest, ReplicationSupport}
import fdswarm.util.NodeIdentity
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.{Inject, Singleton}

import java.util.concurrent.TimeUnit

/**
 * This is the logic that synchronizes the local QSO store with a remote node.
 *
 */
@Singleton
class StatusProcessor @Inject()(qsoStore: ReplicationSupport,
                                replEndpoints: ReplEndpoints,
                                callEndpoint: CallEndpoint,
                                meterRegistry: MeterRegistry,
                                nodeBandOpPane: NodeBandOpPane) extends LazyLogging:

  private val timer = meterRegistry.timer("fdswarm_process_status_duration")

  /**
   * Process an incoming status message: determine what FdHours are needed
   * and POST them to the remote node.
   *
   * @return IO completing after the HTTP call finishes
   */
  def processStatus(nodeStatus: NodeStatus): IO[Unit] =
    val needed = nodeStatus.statusMessage.fdDigests.flatMap(qsoStore.isFdHourNeeded)
    IO(nodeBandOpPane.refreshIfDue()) >>
      (if needed.nonEmpty then
         // Record at least one timing sample to indicate processing occurred
         IO(timer.record(1L, TimeUnit.NANOSECONDS)) >>
           processStatusInternal(nodeStatus, needed).handleError(_ => IO.unit)
       else
         IO.unit)

  private def processStatusInternal(nodeStatus: NodeStatus, needed: Seq[FdHour]): IO[Unit] =
    given NodeIdentity = nodeStatus.nodeIdentity
    needed.traverse_ { fdHour =>
      for
        // 1. Ask remote for all IDs in this hour
        remoteIds <- callEndpoint(ReplEndpoints.qsoIdsByHourGetDef, fdHour)
        _ <- IO(logger.debug(s"Remote has ${remoteIds.size} IDs for $fdHour"))

        // 2. Check locally which ones we don't have
        missingIds <- qsoStore.missingIds(FdHourIds(fdHour, remoteIds))
        _ <- IO(logger.debug(s"Local is missing ${missingIds.size} IDs for $fdHour"))

        // 3. If any are missing, fetch the actual QSOs
        _ <- if missingIds.nonEmpty then
          logger.debug("fdHour: {} Missing: ({}) {}", fdHour, missingIds.size, missingIds.mkString(","))
          for
            remoteQsos <- callEndpoint(ReplEndpoints.qsosForIdsDef, FdHourRequest(fdHour, missingIds))
            _ <- IO(logger.debug(s"Fetched ${remoteQsos.size} remote QSOs for $fdHour"))
            _ <- qsoStore.addQsos(remoteQsos)
          yield ()
        else IO.unit

      yield ()
    }
