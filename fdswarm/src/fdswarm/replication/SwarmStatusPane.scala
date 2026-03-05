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

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.GridUtils
import fdswarm.fx.qso.FdHour
import fdswarm.util.NodeIdentity
import jakarta.inject.{Inject, Singleton}
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.scene.layout.{ColumnConstraints, GridPane, Priority, StackPane}
import scalafx.Includes.*

@Singleton
class SwarmStatusPane @Inject()(swarmStatus: SwarmStatus) extends LazyLogging:

  private val container = new StackPane()
  
  // Rebuild the grid whenever nodeMap changes
  swarmStatus.nodeMap.onChange {
    Platform.runLater {
      buildGrid()
    }
  }

  buildGrid()

  def node: Node = container

  private def buildGrid(): Unit =
    val grid = new GridPane():
      hgap = 5
      vgap = 5
      gridLinesVisible = true

    val nodes: Seq[NodeIdentity] = swarmStatus.nodeMap.keys.toSeq.sorted
    val allHours: Set[FdHour] = swarmStatus.nodeMap.values.flatMap(_.map.keys).toSet
    val hours: Seq[FdHour] = allHours.toSeq.sorted

    if nodes.isEmpty then
      container.children = Seq(GridUtils.fieldSet("Swarm Status", new Label("No nodes discovered yet.")))
      return

    // Header row: Nodes
    grid.add(new Label("Hour \\ Node") {
      style = "-fx-font-weight: bold;"
    }, 0, 0)
    nodes.zipWithIndex.foreach { case (node, colIdx) =>
      grid.add(new Label(node.short) {
        tooltip = node.toString
        style = "-fx-font-weight: bold;"
      }, colIdx + 1, 0)
    }

    // Rows: FdHours
    hours.zipWithIndex.foreach { case (hour, rowIdx) =>
      grid.add(new Label(hour.display) {
        style = "-fx-font-weight: bold;"
      }, 0, rowIdx + 1)
      
      nodes.zipWithIndex.foreach { case (node, colIdx) =>
        val nodeDetails = swarmStatus.nodeMap(node)
        val cell = nodeDetails.map.get(hour) match
          case Some(hourNodeCell) =>
            val label = new Label()
            // Bind label text to lhData count
            label.text <== Bindings.createStringBinding(
              () => {
                val data = hourNodeCell.lhData.value
                if data == null then "-" else data.fdHourDigest.count.toString
              },
              hourNodeCell.lhData
            )
            label
          case None =>
            new Label("-")
        
        grid.add(cell, colIdx + 1, rowIdx + 1)
      }
    }

    container.children = Seq(GridUtils.fieldSet("Swarm Status", grid))
