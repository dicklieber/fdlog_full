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

import fdswarm.fx.GridBuilder
import fdswarm.fx.qso.FdHour
import fdswarm.replication.NodeStatus
import fdswarm.util.NodeIdentity
import jakarta.inject.{Inject, Singleton}
import javafx.beans.value.ChangeListener
import scalafx.application.Platform
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.Parent
import scalafx.scene.layout.GridPane
import scalafx.scene.layout.StackPane

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javafx.collections.ListChangeListener
import scala.collection.concurrent.TrieMap

enum FdHours(val fdHour: FdHour):
  case Value(override val fdHour: FdHour) extends FdHours(fdHour)

  def label: String = s"FdHour:${fdHour.display}"

object FdHours:
  def apply(fdHour: FdHour): FdHours = Value(fdHour)

@Singleton
class SwarmData @Inject() ():
  val knownNodeIdentity: ObservableBuffer[NodeIdentity] = ObservableBuffer.empty[NodeIdentity]

  private val lastStatusByNode = TrieMap.empty[NodeIdentity, NodeStatus]
  private val valueProperties = TrieMap.empty[(NodeIdentity, NodeDataField), StringProperty]
  private val knownFdHourValues = TrieMap.empty[FdHour, FdHour]

  private val stampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

  def knownFdHours: Seq[FdHours] = knownFdHourValues.keys.toSeq.sorted.map(FdHours.apply)

  def update(nodeStatus: NodeStatus): Unit =
    val nodeIdentity = nodeStatus.nodeIdentity
    lastStatusByNode.put(nodeIdentity, nodeStatus)
    nodeStatus.statusMessage.fdDigests.foreach(fd => knownFdHourValues.getOrElseUpdate(fd.fdHour, fd.fdHour))

    updateOnFxThread {
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

  def buildGridPane(fields: Seq[NodeDataField]): Parent =
    val container = new StackPane()

    def rebuildGrid(): Unit =
      container.children.setAll(buildGrid(fields))

    rebuildGrid()
    val nodeListener: ListChangeListener[NodeIdentity] =
      (_: ListChangeListener.Change[? <: NodeIdentity]) => rebuildGrid()
    knownNodeIdentity.delegate.addListener(nodeListener)

    // Remove listener once the wrapper leaves the scene graph.
    val sceneListener: ChangeListener[javafx.scene.Scene] = new ChangeListener[javafx.scene.Scene]:
      override def changed(
          observable: javafx.beans.value.ObservableValue[? <: javafx.scene.Scene],
          oldScene: javafx.scene.Scene,
          newScene: javafx.scene.Scene
      ): Unit =
        if newScene == null then
          knownNodeIdentity.delegate.removeListener(nodeListener)
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
