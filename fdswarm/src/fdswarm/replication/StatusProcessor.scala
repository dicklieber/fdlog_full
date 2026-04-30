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

import cats.effect.unsafe.implicits.global
import fdswarm.api.ReplEndpoints
import fdswarm.logging.LazyStructuredLogging
import fdswarm.logging.Locus.Replication
import fdswarm.replication.status.NodeBandOpPane
import fdswarm.store.QsoStore
import fdswarm.util.StatsSource
import jakarta.inject.{Inject, Singleton}

/** This is the logic that synchronizes the local QSO store with a remote node.
  */
@Singleton
class StatusProcessor @Inject() (
                                  qsoStore: QsoStore,
                                  localNodeStatus: LocalNodeStatus,
                                  replEndpoints: ReplEndpoints,
                                  callEndpoint: CallEndpoint,
                                  nodeBandOpPane: NodeBandOpPane,
                                  nodeStatusDispatcher: NodeStatusDispatcher)
    extends LazyStructuredLogging
    with StatsSource(Replication):

  private val processTimer = addTimer("process")
  nodeStatusDispatcher.addListener(
    service = Service.Status,
    singleListener = false
  )(
    (nodeIdentity, statusMessage) =>
      processStatus(
        NodeStatus(
          statusMessage = statusMessage,
          nodeIdentity = nodeIdentity,
          isLocal = false
        )
      )
  )

  /**
   * If the remote node has a different hash count than the local node, then fetch all the QSOs from the remote node and add them to the local QSO store.
   * Note [[QsoStore.add]] handles duplicates.
    *
    * @return
    *   IO completing after the HTTP call finishes
    */
  private def processStatus(nodeStatus: NodeStatus): Unit =
    val processTimerContext = processTimer.time()
    try
      val remoteHashCount = nodeStatus.statusMessage.hashCount
      val localHashCount = localNodeStatus.statusMessage.hashCount

      if remoteHashCount != localHashCount then
        val qsoCountDiff = remoteHashCount.qsoCount - localHashCount.qsoCount
        logger.info(
          s"HashCount mismatch for ${nodeStatus.nodeIdentity}: local=$localHashCount remote=$remoteHashCount qsoCountDiff=$qsoCountDiff"
        )
        given fdswarm.util.NodeIdentity = nodeStatus.nodeIdentity
        val remoteAllQsos =
          callEndpoint(
            ReplEndpoints.allQsosDef,
            ()
          ).unsafeRunSync()
        qsoStore.add(
          remoteAllQsos.qsos
        )
    finally processTimerContext.stop()
