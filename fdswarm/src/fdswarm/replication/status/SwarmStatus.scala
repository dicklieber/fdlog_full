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

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.qso.FdHour
import fdswarm.replication.*
import fdswarm.store.FdHourDigest
import fdswarm.util.{JavaTimeCirce, NodeIdentity, NodeIdentityManager}
import io.circe.*
import jakarta.inject.*
import jakarta.inject.Provider
import javafx.collections.ListChangeListener
import scalafx.beans.property.ObjectProperty

import java.time.Instant
import scala.collection.concurrent.TrieMap

/**
 * Hold 
 */
@Singleton
class SwarmStatus @Inject() (
                              nodeIdentityManager: NodeIdentityManager,
                              localNodeStatus: LocalNodeStatus,
                              swarmStatusPaneProvider: Provider[SwarmStatusPane]
                            ) extends SwarmStatusApi with LazyLogging:
  val nodeMap: TrieMap[NodeIdentity, NodeStatus] = new TrieMap[NodeIdentity, NodeStatus]

  private def swarmStatusPane: SwarmStatusPane = swarmStatusPaneProvider.get()

  /**
   * 
   * @param nodeStatus as received from a node
   */
  def put(nodeStatus: NodeStatus): Unit =
    nodeMap.put(nodeStatus.nodeIdentity, nodeStatus)
    val pane = swarmStatusPane
    if pane != null then
      pane.update(nodeMap.values.toSeq)

  localNodeStatus.updates.forEach(ns => put(ns))
  localNodeStatus.updates.addListener((change: ListChangeListener.Change[? <: NodeStatus]) =>
    while change.next() do
      if change.wasAdded() then
        change.getAddedSubList.forEach(ns => put(ns))
  )

  def updateLocalDigests(digests: Seq[FdHourDigest]): Unit =
    localNodeStatus.updateDigests(digests)

  def refresh(): Unit =
    val pane = swarmStatusPane
    if pane != null then
      pane.update(nodeMap.values.toSeq)

  def ourNodeIdentity: NodeIdentity = nodeIdentityManager.ourNodeIdentity
  def clear(): Unit =
    val localStatus = nodeMap.get(ourNodeIdentity)
    nodeMap.clear()
    localStatus.foreach(status => nodeMap.put(status.nodeIdentity, status))
    val pane = swarmStatusPane
    if pane != null then
      pane.update(nodeMap.values.toSeq)
    logger.debug("Cleared swarm status data, retaining local node.")

  def remove(nodeIdentity: NodeIdentity): Unit =
    if nodeIdentity != ourNodeIdentity then
      nodeMap.remove(nodeIdentity)
      val pane = swarmStatusPane
      if pane != null then
        pane.update(nodeMap.values.toSeq)
      logger.debug(s"Removed node status for $nodeIdentity")

case class LHData(fdHourDigest: FdHourDigest, lastSeen: Instant = Instant.EPOCH)
object LHData:
  import JavaTimeCirce.given
  given Codec[LHData] = Codec.AsObject.derived

case class FdHourNodeCellDTO(fdHour: FdHour, lhData: LHData)
    derives Codec.AsObject

case class FdHourNodeCell(nideIdentity: NodeIdentity, fdHour: FdHour):
  val lhData: ObjectProperty[LHData] =
    ObjectProperty[LHData](LHData(FdHourDigest.empty(fdHour)))

trait SwarmStatusApi:
  def clear(): Unit
  def refresh(): Unit
  def remove(nodeIdentity: NodeIdentity): Unit
