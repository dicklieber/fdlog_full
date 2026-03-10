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
import fdswarm.fx.utils.IntLabel
import fdswarm.replication.{NodeDetails, ReceivedNodeStatus}
import fdswarm.util.{AgeStyleService, DurationFormat, NodeIdentity, NodeIdentityManager}
import java.time.Instant
import scalafx.beans.property.LongProperty
import scalafx.scene.text.{Font, FontPosture, FontWeight}
import scalafx.scene.layout.GridPane

class SwarmStatusGrid(allNodes: Seq[ReceivedNodeStatus],
                      nowProperty: LongProperty,
                      ageStyleService: AgeStyleService,
                      ourInstanceId: String):

  val fdHours: Seq[FdHour] =
    val allFdHours = for
      receivedNodeStatus <- allNodes
      fdDigest <- receivedNodeStatus.statusMessage.fdDigests
    yield
      fdDigest.fdHour
    allFdHours.distinct.sorted
  

  def bodyCounts: Array[Array[IntLabel]] =
    fdHours.map { fdHour =>
      allNodes.map { nodeStatus =>
        val count = nodeStatus.statusMessage.fdDigests.find(_.fdHour == fdHour).map(_.count).getOrElse(0)
        IntLabel(count)
      }.toArray
    }.toArray

  def populate(builder: GridBuilder, rowStyleCallback: Seq[IntLabel] => String): Unit =
    // Header rows
    builder("InstanceId", allNodes.map(_.nodeIdentity.instanceId)*)
    builder("IP", allNodes.map(_.nodeIdentity.host)*)
    builder("Age", allNodes.map { receivedNodeStatus =>
      if (receivedNodeStatus.nodeIdentity.instanceId == ourInstanceId) {
        new scalafx.scene.control.Label {
          text = "Our Node"
          styleClass.addAll("grid-value", "ourNode")
        }
      } else {
        val binding = scalafx.beans.binding.Bindings.createStringBinding(
          () => {
            val now = Instant.ofEpochMilli(nowProperty.value)
            val styleAndAge = ageStyleService.calc("node", receivedNodeStatus.received, now)
            DurationFormat(styleAndAge.age)
          },
          nowProperty
        )
        new scalafx.scene.control.Label {
          text <== binding
          nowProperty.onChange { (_, _, _) =>
            val now = Instant.ofEpochMilli(nowProperty.value)
            val styleAndAge = ageStyleService.calc("node", receivedNodeStatus.received, now)
            styleClass.removeAll("fresh", "recent", "stale")
            styleClass.add(styleAndAge.style)
          }
        }
      }
    }*)
    builder("Qso Count", allNodes.map(receivedNodeStatus =>
      receivedNodeStatus.qsoCount.toString)*)

    val currentGrid = bodyCounts
    fdHours.zip(currentGrid).foreach { (hour, rowLabels) =>
      val styleClass = rowStyleCallback(rowLabels)
      if (styleClass.nonEmpty) {
        rowLabels.foreach(_.styleClass += styleClass)
      }
      builder(hour.display, rowLabels*)
    }
  
  
  
