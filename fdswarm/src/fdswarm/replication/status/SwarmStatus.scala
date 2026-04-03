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

package fdswarm.replication.status

import fdswarm.StationConfigManager
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.bandmodes.SelectedBandModeManager
import fdswarm.fx.qso.FdHour
import fdswarm.io.DirectoryProvider
import fdswarm.model.BandModeOperator
import fdswarm.replication.*
import fdswarm.store.FdHourDigest
import fdswarm.util.{JavaTimeCirce, NodeIdentity, NodeIdentityManager}
import io.circe.*
import io.circe.parser.decode
import io.circe.syntax.*
import jakarta.inject.*
import jakarta.inject.Provider
import scalafx.application.Platform
import scalafx.beans.property.ObjectProperty

import java.time.Instant
import scala.collection.concurrent.TrieMap

import fdswarm.replication.ReceivedNodeStatus

/**
 * Hold 
 */
@Singleton
class SwarmStatus @Inject() (
                              directoryProvider: DirectoryProvider,
                              nodeIdentityManager: NodeIdentityManager,
                              stationManager: StationConfigManager,
                              selectedBandModeStore: SelectedBandModeManager,
                              swarmStatusPaneProvider: Provider[SwarmStatusPane],
                              contestConfigManagerProvider: Provider[fdswarm.fx.contest.ContestConfigManager]
                            ) extends SwarmStatusApi with LazyLogging:
  val nodeMap: TrieMap[NodeIdentity, ReceivedNodeStatus] = new TrieMap[NodeIdentity, ReceivedNodeStatus]
  private val statusFile = directoryProvider() / "swarmStatus.json"

  private def contestConfigManager: fdswarm.fx.contest.ContestConfigManager = contestConfigManagerProvider.get()
  private def swarmStatusPane: SwarmStatusPane = swarmStatusPaneProvider.get()

  /**
   * 
   * @param receivedNodeStatus as received from a node
   */
  def put(receivedNodeStatus: ReceivedNodeStatus): Unit =
    nodeMap.put(receivedNodeStatus.nodeIdentity, receivedNodeStatus)
    val pane = swarmStatusPane
    if pane != null then
      pane.update(nodeMap.values.toSeq)
    save()

  // Load state on startup
  try
    if os.exists(statusFile) then
      val json = os.read(statusFile)
      decode[Seq[ReceivedNodeStatus]](json) match
        case Right(statuses) =>
          statuses.foreach { status =>
            nodeMap.put(status.nodeIdentity, status)
          }
        case Left(error) =>
          logger.error(s"Error decoding swarm status: $error")
  catch
    case e: Exception =>
      logger.error(s"Error loading swarm status: ${e.getMessage}")

  def updateLocalDigests(digests: Seq[FdHourDigest]): Unit =
    val nodeIdentity = ourNodeIdentity
    val operator = stationManager.station.operator
    val bandMode = selectedBandModeStore.selected.value
    contestConfigManager.contestConfigOption match
      case Some(config) =>
        val statusMessage = StatusMessage(
          fdDigests = digests,
          bandNodeOperator = BandModeOperator(operator, bandMode),
          contestConfig = config)
        val receivedNodeStatus = ReceivedNodeStatus(statusMessage, nodeIdentity)
        put(receivedNodeStatus)
      case None =>
        logger.debug("Skipping updateLocalDigests as contestConfig is not yet initialized")

  def refresh(): Unit =
    val pane = swarmStatusPane
    if pane != null then
      pane.update(nodeMap.values.toSeq)

  def ourNodeIdentity: NodeIdentity = nodeIdentityManager.ourNodeIdentity
  def clear(): Unit =
    val localStatus = nodeMap.get(ourNodeIdentity)
    nodeMap.clear()
    localStatus.foreach(status => nodeMap.put(status.nodeIdentity, status))
    save()
    val pane = swarmStatusPane
    if pane != null then
      pane.update(nodeMap.values.toSeq)
    logger.debug("Cleared swarm status data, retaining local node.")

  def remove(nodeIdentity: NodeIdentity): Unit =
    if nodeIdentity != ourNodeIdentity then
      nodeMap.remove(nodeIdentity)
      save()
      val pane = swarmStatusPane
      if pane != null then
        pane.update(nodeMap.values.toSeq)
      logger.debug(s"Removed node status for $nodeIdentity")

  private def save(): Unit =
    try
      val json = nodeMap.values.asJson.noSpaces
      os.write.over(statusFile, json, createFolders = true)
      logger.trace(s"Saved swarm status to $statusFile")
    catch
      case e: Exception =>
        logger.error(s"Error saving swarm status: ${e.getMessage}")

case class LHData(fdHourDigest: FdHourDigest, lastSeen: Instant = Instant.EPOCH)
object LHData:
  import JavaTimeCirce.given
  given Codec[LHData] = Codec.AsObject.derived

case class FdHourNodeCellDTO(fdHour: FdHour, lhData: LHData)
    derives Codec.AsObject
case class NodeDetailsDTO(cells: Seq[FdHourNodeCellDTO]) derives Codec.AsObject

case class FdHourNodeCell(nideIdentity: NodeIdentity, fdHour: FdHour):
  val lhData: ObjectProperty[LHData] =
    ObjectProperty[LHData](LHData(FdHourDigest.empty(fdHour)))

trait SwarmStatusApi:
  def clear(): Unit
  def refresh(): Unit
  def remove(nodeIdentity: NodeIdentity): Unit