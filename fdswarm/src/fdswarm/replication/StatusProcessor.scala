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

import fdswarm.api.ReplEndpoints
import fdswarm.logging.LazyStructuredLogging
import fdswarm.replication.status.NodeBandOpPane
import fdswarm.store.QsoStore
import jakarta.inject.{Inject, Singleton}
import nl.grons.metrics4.scala.DefaultInstrumented

/** This is the logic that synchronizes the local QSO store with a remote node.
  */
@Singleton
class StatusProcessor @Inject() (
    qsoStore: QsoStore,
    replEndpoints: ReplEndpoints,
    callEndpoint: CallEndpoint,
    nodeBandOpPane: NodeBandOpPane)
    extends LazyStructuredLogging
    with DefaultInstrumented:

  private var processTimer = metrics.timer("fdswarm_process_status_duration")

  /** Process an incoming status message: determine what FdHours are needed and
    * POST them to the remote node.
    *
    * @return
    *   IO completing after the HTTP call finishes
    */
  def processStatus(
      nodeStatus: NodeStatus
    ): Unit =
    throw new NotImplementedError("")
