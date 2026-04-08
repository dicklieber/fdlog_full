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
import fdswarm.fx.station.StationEditor
import fdswarm.replication.LocalNodeStatus
import fdswarm.replication.NodeStatus
import fdswarm.store.FdHourDigest
import fdswarm.util.NodeIdentity
import fdswarm.util.NodeIdentityManager
import jakarta.inject.{Inject, Singleton}
import javafx.beans.value.ChangeListener
import javafx.event.EventHandler
import javafx.scene.input.MouseEvent
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.Parent
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.StackPane

import javafx.collections.ListChangeListener
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong
import scala.collection.concurrent.TrieMap

enum FdHours(val fdHour: FdHour):
  case Value(override val fdHour: FdHour) extends FdHours(fdHour)

  def label: String = s"FdHour:${fdHour.display}"

object FdHours:
  def apply(fdHour: FdHour): FdHours = Value(fdHour)

/**
 * Holds data about each node in the swarm.
 * @param nodeIdentityManager
 * @param localNodeStatus
 */
@Singleton
class SwarmData @Inject() (
                            nodeIdentityManager: NodeIdentityManager,
                            localNodeStatus: LocalNodeStatus,
                            stationEditor: StationEditor
                          ) extends LazyLogging:
  type CellNodeListener = (
    NodeStatus,
    String,
    Node
  ) => Unit

  private case class GridBuildResult(
                                      grid: GridPane,
                                      cellNodes: Map[(NodeIdentity, NodeDataField), Seq[Node]]
                                    )

  val knownNodeIdentity: ObservableBuffer[NodeIdentity] = ObservableBuffer.empty[NodeIdentity]
  private val knownFdHoursBuffer: ObservableBuffer[FdHours] = ObservableBuffer.empty[FdHours]

  val nodeMap: TrieMap[NodeIdentity, NodeStatus] = TrieMap.empty[NodeIdentity, NodeStatus]
  private val valueProperties = TrieMap.empty[(NodeIdentity, NodeDataField), StringProperty]
  private val renderedCellNodes = TrieMap.empty[(NodeIdentity, NodeDataField), Vector[Node]]
  private val knownFdHourValues = TrieMap.empty[FdHour, FdHour]
  private val nodeStatusListeners = TrieMap.empty[Long, Seq[NodeStatus] => Unit]
  private val cellNodeListeners = TrieMap.empty[Long, CellNodeListener]
  private val nodeStatusListenerId = AtomicLong(0L)
  private val cellNodeListenerId = AtomicLong(0L)
  private val ourNodeStyleClass = "ourNode"
  private val operatorLinkStyleClass = "operatorLink"
  private val operatorFieldName = NodeDataField.Operator.label

  private val stampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

  def ourNodeIdentity: NodeIdentity =
    nodeIdentityManager.ourNodeIdentity

  def allNodeStatuses: Seq[NodeStatus] =
    nodeMap.values.toSeq

  def addNodeStatusListener(
                             listener: Seq[NodeStatus] => Unit
                           ): () => Unit =
    val id = nodeStatusListenerId.incrementAndGet()
    nodeStatusListeners.put(id, listener)
    listener(
      allNodeStatuses
    )
    () => nodeStatusListeners.remove(id)

  def addCellNodeListener(
                           listener: CellNodeListener
                         ): () => Unit =
    val id = cellNodeListenerId.incrementAndGet()
    cellNodeListeners.put(id, listener)
    updateOnFxThread {
      renderedCellNodes.foreach {
        case ((nodeIdentity, field), cells) =>
          nodeMap.get(nodeIdentity).foreach(
            status =>
              cells.foreach(
                cell =>
                  listener(
                    status,
                    field.label,
                    cell
                  )
              )
          )
      }
    }
    () => cellNodeListeners.remove(id)

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
    notifyNodeStatusListeners()
    logger.debug("Cleared swarm status data, retaining local node.")

  def remove(nodeIdentity: NodeIdentity): Unit =
    if nodeIdentity != ourNodeIdentity then
      nodeMap.remove(nodeIdentity)
      updateKnownCollectionsFromNodeMap()
      notifyNodeStatusListeners()
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
        notifyCellNodeListeners(
          nodeStatus,
          field
        )
      }

      val countByHour = nodeStatus.statusMessage.fdDigests.map(fd => fd.fdHour -> fd.count).toMap
      knownFdHours.foreach { fdHourEnum =>
        val field = NodeDataField.FdHoursField(fdHourEnum)
        val value = countByHour.getOrElse(fdHourEnum.fdHour, 0).toString
        propertyFor(nodeIdentity, field).value = value
        notifyCellNodeListeners(
          nodeStatus,
          field
        )
      }
    }
    notifyNodeStatusListeners()

  def buildGridPane(fields: Seq[NodeDataField | FdHours.type]): Parent =
    val container = new StackPane()
    val includeAllFdHours = fields.contains(FdHours)
    var renderedByThisPane = Map.empty[(NodeIdentity, NodeDataField), Seq[Node]]

    def rebuildGrid(): Unit =
      unregisterRenderedCells(
        renderedByThisPane
      )
      val result = buildGrid(resolveFields(fields))
      renderedByThisPane = result.cellNodes
      registerRenderedCells(
        renderedByThisPane
      )
      container.children.setAll(
        result.grid
      )

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
          unregisterRenderedCells(
            renderedByThisPane
          )
          renderedByThisPane = Map.empty
          container.delegate.sceneProperty.removeListener(this)
    container.delegate.sceneProperty.addListener(sceneListener)

    container

  private def buildGrid(fields: Seq[NodeDataField]): GridBuildResult =
    val builder = GridBuilder()
    val nodes = knownNodeIdentity.toSeq.sorted
    val cellsByField = scala.collection.mutable.Map.empty[(NodeIdentity, NodeDataField), Vector[Node]]
    fields.foreach { field =>
      val values = nodes.map(
        node =>
          val cellNode = GridBuilder.valueToLabel(
            propertyFor(
              node,
              field
            )
          )
          val key = (
            node,
            field
          )
          val updated = cellsByField.getOrElse(
            key,
            Vector.empty
          ) :+ cellNode
          cellsByField.update(
            key,
            updated
          )
          cellNode
      )
      builder(
        field.label,
        values*
      )
    }
    GridBuildResult(
      builder.result,
      cellsByField.view.mapValues(_.toSeq).toMap
    )

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
      NodeDataField.BandMode -> bno.bandMode.toString,
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

  private def notifyNodeStatusListeners(): Unit =
    val statuses = allNodeStatuses
    nodeStatusListeners.values.foreach(
      listener =>
        listener(
          statuses
        )
    )

  private def registerRenderedCells(
                                     cellsByField: Map[(NodeIdentity, NodeDataField), Seq[Node]]
                                   ): Unit =
    cellsByField.foreach {
      case (key, cells) =>
        val merged = renderedCellNodes.getOrElse(key, Vector.empty) ++ cells
        renderedCellNodes.put(
          key,
          merged
        )
        nodeMap.get(key._1).foreach(
          status =>
            cells.foreach(
              cell =>
                notifyCellNodeListeners(
                  status,
                  key._2,
                  Seq(cell)
                )
            )
        )
    }

  private def unregisterRenderedCells(
                                       cellsByField: Map[(NodeIdentity, NodeDataField), Seq[Node]]
                                     ): Unit =
    cellsByField.foreach {
      case (key, removedCells) =>
        renderedCellNodes.get(key).foreach(
          existing =>
            val remaining = existing.filterNot(
              existingCell =>
                removedCells.exists(
                  removed =>
                    removed.delegate eq existingCell.delegate
                )
            )
            if remaining.isEmpty then
              renderedCellNodes.remove(key)
            else
              renderedCellNodes.put(
                key,
                remaining
              )
        )
    }

  private def notifyCellNodeListeners(
                                       nodeStatus: NodeStatus,
                                       field: NodeDataField,
                                       targetCells: Seq[Node]
                                     ): Unit =
    targetCells.foreach(
      cell =>
        doStyle(
          nodeStatus,
          field.label,
          cell
        )
        if cellNodeListeners.nonEmpty then
          cellNodeListeners.values.foreach(
            listener =>
              listener(
                nodeStatus,
                field.label,
                cell
              )
          )
    )

  private def notifyCellNodeListeners(
                                       nodeStatus: NodeStatus,
                                       field: NodeDataField
                                     ): Unit =
    val cells = renderedCellNodes.getOrElse(
      (nodeStatus.nodeIdentity, field),
      Vector.empty
    )
    notifyCellNodeListeners(
      nodeStatus,
      field,
      cells
    )

  /**
   * Applies the appropriate styling and behavior to a given node based on its status and field name.
   *
   * @param nodeStatus the status of the node, providing context about its state and whether it is local
   * @param fieldName  the name of the field associated with the node, used to determine specific styling rules
   * @param node       the node to which the styling rules and behaviors will be applied
   * @return Unit this method does not return a value; it modifies the node's style and event handlers directly
   */
  private def doStyle(
                       nodeStatus: NodeStatus,
                       fieldName: String,
                       node: Node
                     ): Unit =
    // Keep all current and future styling rules in one place.
    setStyleClass(
      node,
      ourNodeStyleClass,
      nodeStatus.isLocal
    )

    val isLocalOperatorField = nodeStatus.isLocal && fieldName == operatorFieldName
    setStyleClass(
      node,
      operatorLinkStyleClass,
      isLocalOperatorField
    )
    if isLocalOperatorField then
      node.delegate.setOnMouseClicked(
        new EventHandler[MouseEvent]:
          override def handle(
                               event: MouseEvent
                             ): Unit =
            Option(node.scene.value)
              .flatMap(scene => Option(scene.delegate.getWindow))
              .foreach(
                window =>
                  stationEditor.show(
                    window
                  )
              )
      )
    else
      node.delegate.setOnMouseClicked(
        null
      )

  private def setStyleClass(
                             node: Node,
                             styleClassName: String,
                             enabled: Boolean
                           ): Unit =
    if enabled then
      if !node.styleClass.contains(styleClassName) then
        node.styleClass += styleClassName
    else
      while node.styleClass.contains(styleClassName) do
        node.styleClass -= styleClassName
