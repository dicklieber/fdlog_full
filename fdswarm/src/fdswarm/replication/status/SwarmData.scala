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

import fdswarm.fx.station.StationEditor
import fdswarm.fx.{FdLogUi, GridBuilder}
import fdswarm.logging.LazyStructuredLogging
import fdswarm.logging.Locus.Replication
import fdswarm.replication.{NodeStatus, NodeStatusDispatcher}
import fdswarm.util.{NodeIdentity, NodeIdentityManager}
import jakarta.inject.{Inject, Singleton}
import javafx.beans.value.ChangeListener
import javafx.collections.ListChangeListener
import javafx.event.EventHandler
import javafx.scene.input.MouseEvent
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.beans.property.{IntegerProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{Label, ScrollPane, Tooltip}
import scalafx.scene.layout.{GridPane, StackPane}
import scalafx.scene.{Node, Parent}

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong
import scala.collection.concurrent.TrieMap

/** Holds data about each node in the swarm.
  */
@Singleton
class SwarmData @Inject() (
    nodeIdentityManager: NodeIdentityManager,
    stationEditor: StationEditor,
    ageCellStyleRefresher: AgeCellStyleRefresher,
    nodeStatusDispatcher: NodeStatusDispatcher)
    extends LazyStructuredLogging(Replication):
  type CellNodeListener = (NodeStatus, String, Node) => Unit
  val knownNodeIdentity: ObservableBuffer[NodeIdentity] = ObservableBuffer.empty[NodeIdentity]
  val nodeMap: TrieMap[NodeIdentity, NodeStatus] = TrieMap.empty[NodeIdentity, NodeStatus]
  val size: IntegerProperty = new IntegerProperty(
    this,
    "size",
    0
  )
  private val valueProperties = TrieMap.empty[(NodeIdentity, NodeDataField), StringProperty]
  private val renderedCellNodes = TrieMap.empty[(NodeIdentity, NodeDataField), Vector[Node]]
  private val nodeStatusListeners = TrieMap.empty[Long, Seq[NodeStatus] => Unit]
  private val cellNodeListeners = TrieMap.empty[Long, CellNodeListener]
  private val nodeStatusListenerId = AtomicLong(0L)
  private val cellNodeListenerId = AtomicLong(0L)
  private val ourNodeStyleClass = "ourNode"
  private val operatorLinkStyleClass = "operatorLink"
  private val operatorFieldName = NodeDataField.Operator.label
  private val stampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
    .withZone(ZoneId.systemDefault())

  def addNodeStatusListener(listener: Seq[NodeStatus] => Unit): () => Unit =
    val id = nodeStatusListenerId.incrementAndGet()
    nodeStatusListeners.put(id, listener)
    listener(allNodeStatuses)
    () => nodeStatusListeners.remove(id)

  ageCellStyleRefresher.setPurgeCallback(nodeIdentity =>
    logger.info("Purging", "Node" -> nodeIdentity.toString)
    remove(nodeIdentity))
  nodeStatusDispatcher.addNodeStatusListener(update)

  def allNodeStatuses: Seq[NodeStatus] = nodeMap.values.toSeq

  def clear(): Unit =
    val localStatus = nodeMap.get(ourNodeIdentity)
    nodeMap.keys.foreach(nodeIdentity => ageCellStyleRefresher.remove(nodeIdentity))
    nodeMap.clear()
    localStatus.foreach(status => nodeMap.put(status.nodeIdentity, status))
    updateKnownCollectionsFromNodeMap()
    notifyNodeStatusListeners()
    logger.debug("Cleared swarm status data, retaining local node.")

  def remove(nodeIdentity: NodeIdentity): Unit = if nodeIdentity != ourNodeIdentity then
    nodeMap.remove(nodeIdentity).foreach(_ => ageCellStyleRefresher.remove(nodeIdentity))
    updateKnownCollectionsFromNodeMap()
    notifyNodeStatusListeners()
    logger.debug(s"Removed node status for $nodeIdentity")

  def ourNodeIdentity: NodeIdentity = nodeIdentityManager.ourNodeIdentity

  private def updateKnownCollectionsFromNodeMap(): Unit =
    val nodeStatuses = nodeMap.values.toSeq
    val nodes = nodeStatuses.sorted.map(_.nodeIdentity).distinct
    updateOnFxThread {
      knownNodeIdentity.clear()
      knownNodeIdentity ++= nodes
      size.value = nodes.size
    }

  private def updateOnFxThread(action: => Unit): Unit =
    if Platform.isFxApplicationThread then action
    else
      try Platform.runLater(() => action)
      catch case _: IllegalStateException => action

  private def notifyNodeStatusListeners(): Unit =
    val statuses = allNodeStatuses
    nodeStatusListeners.values.foreach(listener => listener(statuses))

  def update(nodeStatus: NodeStatus): Unit =
    val nodeIdentity = nodeStatus.nodeIdentity
    nodeMap.put(nodeIdentity, nodeStatus)
    ageCellStyleRefresher.track(nodeStatus = nodeStatus)

    updateOnFxThread {
      val staticValues = staticFieldValues(nodeStatus)
      staticValues.foreach { case (field, value) =>
        propertyFor(nodeIdentity, field).value = value
        notifyCellNodeListeners(nodeStatus, field)
      }
    }
    updateKnownCollectionsFromNodeMap()
    notifyNodeStatusListeners()

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
      NodeDataField.QsoCount -> nodeStatus.statusMessage.hashCount.qsoCount.toString,
      NodeDataField.Hash -> nodeStatus.statusMessage.hashCount.hash,
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
      NodeDataField.Exchange -> contest.exchange
    )

  private def notifyCellNodeListeners(nodeStatus: NodeStatus, field: NodeDataField): Unit =
    val cells = renderedCellNodes.getOrElse((nodeStatus.nodeIdentity, field), Vector.empty)
    notifyCellNodeListeners(nodeStatus, field, cells)

  def buildGridPane(fields: Seq[NodeDataField]): Parent =
    val gridContainer = new StackPane()
    val scrollPane = new ScrollPane:
      content = gridContainer
      hbarPolicy = ScrollPane.ScrollBarPolicy.Always
      vbarPolicy = ScrollPane.ScrollBarPolicy.Never
      fitToHeight = false
      fitToWidth = false
      pannable = true
    var renderedByThisPane = Map.empty[(NodeIdentity, NodeDataField), Seq[Node]]

    def rebuildGrid(): Unit =
      unregisterRenderedCells(renderedByThisPane)
      val result = buildGrid(fields.distinct)
      renderedByThisPane = result.cellNodes
      registerRenderedCells(renderedByThisPane)
      gridContainer.children.setAll(result.grid)

    rebuildGrid()
    val nodeListener: ListChangeListener[NodeIdentity] =
      (_: ListChangeListener.Change[? <: NodeIdentity]) => rebuildGrid()
    knownNodeIdentity.delegate.addListener(nodeListener)

    // Remove listener once the wrapper leaves the scene graph.
    val sceneListener: ChangeListener[javafx.scene.Scene] = new ChangeListener[javafx.scene.Scene]:
      override def changed(
          observable: javafx.beans.value.ObservableValue[? <: javafx.scene.Scene],
          oldScene: javafx.scene.Scene,
          newScene: javafx.scene.Scene): Unit = if newScene == null then
        knownNodeIdentity.delegate.removeListener(nodeListener)
        unregisterRenderedCells(renderedByThisPane)
        renderedByThisPane = Map.empty
        scrollPane.delegate.sceneProperty.removeListener(this)
    scrollPane.delegate.sceneProperty.addListener(sceneListener)

    scrollPane

  private def buildGrid(fields: Seq[NodeDataField]): GridBuildResult =
    val builder = GridBuilder()
    builder.hgap = 1
    builder.vgap = 1
    builder.padding = scalafx.geometry.Insets(1)
    builder.style = "-fx-background-color: #808080; -fx-background-insets: 0;"
    val nodes = knownNodeIdentity.toSeq
    val cellsByField = scala.collection.mutable.Map.empty[(NodeIdentity, NodeDataField), Vector[Node]]
    fields.foreach { field =>
      val values = nodes.map(node =>
        val cellNode = buildCellNode(node, field)
        val key = (node, field)
        val updated = cellsByField.getOrElse(key, Vector.empty) :+ cellNode
        cellsByField.update(key, updated)
        cellNode)
      builder(field.label, values*)
    }
    val grid = builder.result
    grid.styleClass += "swarm-status-grid"
    GridBuildResult(grid, cellsByField.view.mapValues(_.toSeq).toMap)

  private def buildCellNode(node: NodeIdentity, field: NodeDataField): Node =
    val valueProperty = propertyFor(node, field)
    if field == NodeDataField.Hash then
      val shortHashBinding = Bindings
        .createStringBinding(() => Option(valueProperty.value).getOrElse("").take(5), valueProperty)
      new Label:
        text <== shortHashBinding
        tooltip = new Tooltip:
          text <== valueProperty
    else GridBuilder.valueToLabel(valueProperty)

  private def propertyFor(node: NodeIdentity, field: NodeDataField): StringProperty = valueProperties
    .getOrElseUpdate((node, field), StringProperty(""))

  private def registerRenderedCells(cellsByField: Map[(NodeIdentity, NodeDataField), Seq[Node]]): Unit = cellsByField
    .foreach { case (key, cells) =>
      val merged = renderedCellNodes.getOrElse(key, Vector.empty) ++ cells
      renderedCellNodes.put(key, merged)
      nodeMap.get(key._1).foreach(status => cells.foreach(cell => notifyCellNodeListeners(status, key._2, Seq(cell))))
    }

  private def notifyCellNodeListeners(nodeStatus: NodeStatus, field: NodeDataField, targetCells: Seq[Node]): Unit =
    targetCells.foreach(cell =>
      doStyle(CellStyleContext(nodeStatus, field.label, cell))
      if cellNodeListeners.nonEmpty then
        cellNodeListeners.values.foreach(listener => listener(nodeStatus, field.label, cell)))

  /** Applies the appropriate styling and behavior to a given node based on its status and field name.
    *
    * @param nodeStyler wraps the status, field, and node to which styling rules will be applied
    * @return Unit this method does not return a value; it modifies the node's style and event handlers directly
    */
  private def doStyle(nodeStyler: CellStyleContext): Unit =
    val nodeStatus = nodeStyler.nodeStatus
    val fieldName = nodeStyler.fieldName
    val node = nodeStyler.node
    // Keep all current and future styling rules in one place.
    if nodeStatus.isLocal then
      if !node.styleClass.contains(ourNodeStyleClass) then node.styleClass += ourNodeStyleClass
      else while node.styleClass.contains(ourNodeStyleClass) do node.styleClass -= ourNodeStyleClass

    val isLocalOperatorField = nodeStatus.isLocal && fieldName == operatorFieldName
    if isLocalOperatorField then
      if !node.styleClass.contains(operatorLinkStyleClass) then node.styleClass += operatorLinkStyleClass
      else while node.styleClass.contains(operatorLinkStyleClass) do node.styleClass -= operatorLinkStyleClass
    if isLocalOperatorField then
      node.delegate.setOnMouseClicked(
        new EventHandler[MouseEvent]:
          override def handle(event: MouseEvent): Unit =
            stationEditor.show(FdLogUi.primaryStage)
      )
    else node.delegate.setOnMouseClicked(null)

    if fieldName == NodeDataField.Received.label then ageCellStyleRefresher.add(nodeStyler)

  private def unregisterRenderedCells(cellsByField: Map[(NodeIdentity, NodeDataField), Seq[Node]]): Unit = cellsByField
    .foreach { case (key, removedCells) =>
      renderedCellNodes.get(key).foreach(existing =>
        val remaining = existing
          .filterNot(existingCell => removedCells.exists(removed => removed.delegate eq existingCell.delegate))
        if remaining.isEmpty then renderedCellNodes.remove(key) else renderedCellNodes.put(key, remaining))
    }

  private case class GridBuildResult(grid: GridPane, cellNodes: Map[(NodeIdentity, NodeDataField), Seq[Node]])
