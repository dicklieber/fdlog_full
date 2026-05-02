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
import fdswarm.api.{AllQsos, ReplEndpoints}
import fdswarm.logging.LazyStructuredLogging
import fdswarm.logging.Locus.{Replication, TCP}
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
    with StatsSource(TCP):

  private val processTimer = addTimer("process")
  private val httpRequestCounter = addCounter("http-request")
  private val allQsosHttpRequestMeter = addMeter("allQsos.httpRequest")
  private val allQsosFetchAndApplyTimer = addTimer("allQsos.fetchAndApply")
  private val allQsosResponseBytesHistogram = addHistogram("allQsos.responseBytes")
  private val allQsosResponseQsoCountHistogram = addHistogram("allQsos.responseQsoCount")
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
        val allQsosContext = allQsosFetchAndApplyTimer.time()
        try
          allQsosHttpRequestMeter.mark()
          val (_, contentEncoding, _, responseBytes) =
            callEndpoint(
              ReplEndpoints.allQsosDef,
              Some("gzip")
            ).unsafeRunSync()
          val remoteAllQsos: AllQsos =
            ReplEndpoints.decodeAllQsos(
              contentEncoding = contentEncoding,
              bytes = responseBytes
            )
          httpRequestCounter.inc()
          allQsosResponseBytesHistogram.update(
            responseBytes.length
          )
          allQsosResponseQsoCountHistogram.update(remoteAllQsos.qsos.size)
          qsoStore.addReplicated(
            remoteAllQsos.qsos
          )
        finally allQsosContext.stop()
    finally processTimerContext.stop()
