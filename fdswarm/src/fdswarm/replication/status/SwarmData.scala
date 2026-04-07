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
import scalafx.application.Platform
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.layout.GridPane

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import scala.collection.concurrent.TrieMap

enum FdHours(val fdHour: FdHour):
  case Value(override val fdHour: FdHour) extends FdHours(fdHour)

  def label: String = s"FdHour:${fdHour.display}"

object FdHours:
  def apply(fdHour: FdHour): FdHours = Value(fdHour)

enum NodeDataField:
  case HostIp
  case Port
  case HostName
  case InstanceId
  case Received
  case IsLocal
  case QsoCount
  case StatusId
  case Operator
  case Band
  case Mode
  case BandModeStamp
  case ContestType
  case ContestCallsign
  case ContestTransmitters
  case ContestClass
  case ContestSection
  case ContestStamp
  case FdHoursField(fdHour: FdHours)

  def label: String = this match
    case HostIp => "hostIp"
    case Port => "port"
    case HostName => "hostName"
    case InstanceId => "instanceId"
    case Received => "received"
    case IsLocal => "isLocal"
    case QsoCount => "qsoCount"
    case StatusId => "statusId"
    case Operator => "operator"
    case Band => "band"
    case Mode => "mode"
    case BandModeStamp => "bandModeStamp"
    case ContestType => "contestType"
    case ContestCallsign => "contestCallsign"
    case ContestTransmitters => "contestTransmitters"
    case ContestClass => "contestClass"
    case ContestSection => "contestSection"
    case ContestStamp => "contestStamp"
    case FdHoursField(fdHour) => fdHour.label

object NodeDataField:
  val staticFields: Seq[NodeDataField] = Seq(
    NodeDataField.HostIp,
    NodeDataField.Port,
    NodeDataField.HostName,
    NodeDataField.InstanceId,
    NodeDataField.Received,
    NodeDataField.IsLocal,
    NodeDataField.QsoCount,
    NodeDataField.StatusId,
    NodeDataField.Operator,
    NodeDataField.Band,
    NodeDataField.Mode,
    NodeDataField.BandModeStamp,
    NodeDataField.ContestType,
    NodeDataField.ContestCallsign,
    NodeDataField.ContestTransmitters,
    NodeDataField.ContestClass,
    NodeDataField.ContestSection,
    NodeDataField.ContestStamp
  )

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

  def buildGridPane(fields: Seq[NodeDataField]): GridPane =
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
