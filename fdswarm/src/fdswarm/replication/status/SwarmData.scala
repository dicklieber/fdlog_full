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
import fdswarm.fx.GridBuilder
import fdswarm.fx.qso.FdHour
import fdswarm.replication.LocalNodeStatus
import fdswarm.replication.NodeStatus
import fdswarm.store.FdHourDigest
import fdswarm.util.NodeIdentity
import fdswarm.util.NodeIdentityManager
import jakarta.inject.{Inject, Provider, Singleton}
import javafx.beans.value.ChangeListener
import scalafx.application.Platform
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.Parent
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.StackPane

import javafx.collections.ListChangeListener
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import scala.collection.concurrent.TrieMap

enum FdHours(val fdHour: FdHour):
  case Value(override val fdHour: FdHour) extends FdHours(fdHour)

  def label: String = s"FdHour:${fdHour.display}"

object FdHours:
  def apply(fdHour: FdHour): FdHours = Value(fdHour)

@Singleton
class SwarmData @Inject() (
                            nodeIdentityManager: NodeIdentityManager,
                            localNodeStatus: LocalNodeStatus,
                            swarmStatusPaneProvider: Provider[SwarmStatusPane]
                          ) extends LazyLogging:
  val knownNodeIdentity: ObservableBuffer[NodeIdentity] = ObservableBuffer.empty[NodeIdentity]
  private val knownFdHoursBuffer: ObservableBuffer[FdHours] = ObservableBuffer.empty[FdHours]

  val nodeMap: TrieMap[NodeIdentity, NodeStatus] = TrieMap.empty[NodeIdentity, NodeStatus]
  private val valueProperties = TrieMap.empty[(NodeIdentity, NodeDataField), StringProperty]
  private val knownFdHourValues = TrieMap.empty[FdHour, FdHour]

  private val stampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

  private def swarmStatusPane: SwarmStatusPane = swarmStatusPaneProvider.get()

  def ourNodeIdentity: NodeIdentity =
    nodeIdentityManager.ourNodeIdentity

  Option(localNodeStatus.current.get()).foreach(
    nodeStatus =>
      update(nodeStatus)
  )
  localNodeStatus.current.addListener(
    (
      _: javafx.beans.value.ObservableValue[? <: NodeStatus],
      _: NodeStatus,
      newStatus: NodeStatus
    ) =>
      if newStatus != null then
        update(newStatus)
  )

  def updateLocalDigests(digests: Seq[FdHourDigest]): Unit =
    localNodeStatus.updateDigests(digests)

  def refresh(): Unit =
    val pane = swarmStatusPane
    if pane != null then
      pane.update(nodeMap.values.toSeq)

  def clear(): Unit =
    val localStatus = nodeMap.get(ourNodeIdentity)
    nodeMap.clear()
    localStatus.foreach(
      status =>
        nodeMap.put(
          status.nodeIdentity,
          status
        )
    )
    updateKnownCollectionsFromNodeMap()
    refresh()
    logger.debug("Cleared swarm status data, retaining local node.")

  def remove(nodeIdentity: NodeIdentity): Unit =
    if nodeIdentity != ourNodeIdentity then
      nodeMap.remove(nodeIdentity)
      updateKnownCollectionsFromNodeMap()
      refresh()
      logger.debug(s"Removed node status for $nodeIdentity")

  def knownFdHours: Seq[FdHours] = knownFdHoursBuffer.toSeq

  def update(nodeStatus: NodeStatus): Unit =
    val nodeIdentity = nodeStatus.nodeIdentity
    nodeMap.put(nodeIdentity, nodeStatus)
    val newlyDiscoveredFdHours = nodeStatus.statusMessage.fdDigests
      .flatMap(fd => if knownFdHourValues.putIfAbsent(fd.fdHour, fd.fdHour).isEmpty then Some(fd.fdHour) else None)
      .distinct
      .sorted

    updateOnFxThread {
      if newlyDiscoveredFdHours.nonEmpty then
        val mergedFdHours = (knownFdHoursBuffer.map(_.fdHour) ++ newlyDiscoveredFdHours).distinct.sorted.map(FdHours.apply)
        knownFdHoursBuffer.clear()
        knownFdHoursBuffer ++= mergedFdHours

      if !knownNodeIdentity.contains(nodeIdentity) then
        knownNodeIdentity += nodeIdentity

      val staticValues = staticFieldValues(nodeStatus)
      staticValues.foreach { case (field, value) =>
        propertyFor(nodeIdentity, field).value = value
      }

      val countByHour = nodeStatus.statusMessage.fdDigests.map(fd => fd.fdHour -> fd.count).toMap
      knownFdHours.foreach { fdHourEnum =>
        val field = NodeDataField.FdHoursField(fdHourEnum)
        val value = countByHour.getOrElse(fdHourEnum.fdHour, 0).toString
        propertyFor(nodeIdentity, field).value = value
      }
    }
    refresh()

  def buildGridPane(fields: Seq[NodeDataField | FdHours.type]): Parent =
    val container = new StackPane()
    val includeAllFdHours = fields.contains(FdHours)

    def rebuildGrid(): Unit =
      container.children.setAll(buildGrid(resolveFields(fields)))

    rebuildGrid()
    val nodeListener: ListChangeListener[NodeIdentity] =
      (_: ListChangeListener.Change[? <: NodeIdentity]) => rebuildGrid()
    knownNodeIdentity.delegate.addListener(nodeListener)

    val fdHoursListener: ListChangeListener[FdHours] =
      (_: ListChangeListener.Change[? <: FdHours]) => rebuildGrid()
    if includeAllFdHours then
      knownFdHoursBuffer.delegate.addListener(fdHoursListener)

    // Remove listener once the wrapper leaves the scene graph.
    val sceneListener: ChangeListener[javafx.scene.Scene] = new ChangeListener[javafx.scene.Scene]:
      override def changed(
          observable: javafx.beans.value.ObservableValue[? <: javafx.scene.Scene],
          oldScene: javafx.scene.Scene,
          newScene: javafx.scene.Scene
      ): Unit =
        if newScene == null then
          knownNodeIdentity.delegate.removeListener(nodeListener)
          if includeAllFdHours then
            knownFdHoursBuffer.delegate.removeListener(fdHoursListener)
          container.delegate.sceneProperty.removeListener(this)
    container.delegate.sceneProperty.addListener(sceneListener)

    container

  private def buildGrid(fields: Seq[NodeDataField]): GridPane =
    val builder = GridBuilder()
    val nodes = knownNodeIdentity.toSeq.sorted
    builder("", nodes.map(_.hostName)*)
    fields.foreach { field =>
      val values = nodes.map(node => propertyFor(node, field))
      builder(field.label, values*)
    }
    builder.result

  private[status] def addKnownFdHours(fdHours: Seq[FdHour]): Unit =
    if fdHours.nonEmpty then
      fdHours.foreach(fdHour => knownFdHourValues.putIfAbsent(fdHour, fdHour))
      val mergedFdHours = (knownFdHoursBuffer.map(_.fdHour) ++ fdHours).distinct.sorted.map(FdHours.apply)
      knownFdHoursBuffer.clear()
      knownFdHoursBuffer ++= mergedFdHours

  private[status] def resolveFields(fields: Seq[NodeDataField | FdHours.type]): Seq[NodeDataField] =
    fields
      .flatMap {
        case nodeDataField: NodeDataField => Seq(nodeDataField)
        case FdHours => knownFdHours.map(NodeDataField.FdHoursField.apply)
      }
      .distinct

  private def staticFieldValues(nodeStatus: NodeStatus): Map[NodeDataField, String] =
    val bno = nodeStatus.statusMessage.bandNodeOperator
    val contest = nodeStatus.statusMessage.contestConfig
    Map(
      NodeDataField.HostIp -> nodeStatus.nodeIdentity.hostIp,
      NodeDataField.Port -> nodeStatus.nodeIdentity.port.toString,
      NodeDataField.HostName -> nodeStatus.nodeIdentity.hostName,
      NodeDataField.InstanceId -> nodeStatus.nodeIdentity.instanceId,
      NodeDataField.Received -> stampFormatter.format(nodeStatus.received),
      NodeDataField.IsLocal -> nodeStatus.isLocal.toString,
      NodeDataField.QsoCount -> nodeStatus.qsoCount.toString,
      NodeDataField.StatusId -> nodeStatus.statusMessage.id,
      NodeDataField.Operator -> bno.operator.toString,
      NodeDataField.Band -> bno.bandMode.band,
      NodeDataField.Mode -> bno.bandMode.mode,
      NodeDataField.BandModeStamp -> stampFormatter.format(bno.stamp),
      NodeDataField.ContestType -> contest.contestType.toString,
      NodeDataField.ContestCallsign -> contest.ourCallsign.toString,
      NodeDataField.ContestTransmitters -> contest.transmitters.toString,
      NodeDataField.ContestClass -> contest.ourClass,
      NodeDataField.ContestSection -> contest.ourSection,
      NodeDataField.ContestStamp -> stampFormatter.format(contest.stamp)
    )

  private def propertyFor(node: NodeIdentity, field: NodeDataField): StringProperty =
    valueProperties.getOrElseUpdate((node, field), StringProperty(""))

  private def updateOnFxThread(action: => Unit): Unit =
    if Platform.isFxApplicationThread then
      action
    else
      Platform.runLater(() => action)

  private def updateKnownCollectionsFromNodeMap(): Unit =
    val nodeStatuses = nodeMap.values.toSeq
    val nodes = nodeStatuses.map(_.nodeIdentity).distinct.sorted
    val mergedFdHours = nodeStatuses
      .flatMap(_.statusMessage.fdDigests.map(_.fdHour))
      .distinct
      .sorted
      .map(FdHours.apply)
    updateOnFxThread {
      knownNodeIdentity.clear()
      knownNodeIdentity ++= nodes
      knownFdHoursBuffer.clear()
      knownFdHoursBuffer ++= mergedFdHours
    }
