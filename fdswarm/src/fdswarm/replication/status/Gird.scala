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
import fdswarm.replication.NodeDetails
import fdswarm.util.DurationFormat
import scalafx.beans.property.LongProperty
import scalafx.scene.layout.GridPane

class Gird(allNodeDetails: Seq[NodeDetails], nowProperty: LongProperty):

  val hours: Array[FdHour] = allNodeDetails.flatMap(_.map.keys).distinct.sorted.toArray

  def grid: Array[Array[IntLabel]] = hours.map { hour =>
    allNodeDetails.map { nodeDetails =>
      val count = nodeDetails.map.get(hour).map(_.lhData.value.fdHourDigest.count).getOrElse(0)
      IntLabel(count)
    }.toArray
  }

  def populate(builder: GridBuilder, rowStyleCallback: Seq[IntLabel] => String): Unit =
    // Header rows
    builder("InstanceId", allNodeDetails.map(_.nodeIdentity.instanceId)*)
    builder("IP", allNodeDetails.map(_.nodeIdentity.host)*)
    builder("Age", allNodeDetails.map { nd =>
      val binding = scalafx.beans.binding.Bindings.createStringBinding(
        () => DurationFormat(nd.lastUpdate.value),
        nd.lastUpdate,
        nowProperty
      )
      new scalafx.scene.control.Label {
        text <== binding
      }
    }*)
    builder("Qso Count", allNodeDetails.map(_.qsoCount.value.toString)*)

    val currentGrid = grid
    hours.zip(currentGrid).foreach { (hour, rowLabels) =>
      val styleClass = rowStyleCallback(rowLabels)
      if (styleClass.nonEmpty) {
        rowLabels.foreach(_.styleClass += styleClass)
      }
      builder(hour.display, rowLabels*)
    }
  
  
  
