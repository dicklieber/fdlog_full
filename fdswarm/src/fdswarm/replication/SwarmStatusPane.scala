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
import fdswarm.util.{DurationFormat, NodeIdentity}
import jakarta.inject.{Inject, Singleton}
import scalafx.animation.{KeyFrame, Timeline}
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.beans.property.LongProperty
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.{Label, Tooltip}
import scalafx.scene.layout.{ColumnConstraints, GridPane, Priority, Region, StackPane}
import scalafx.Includes.*
import scalafx.util.Duration

import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Singleton
class SwarmStatusPane @Inject()(swarmStatus: SwarmStatus) extends LazyLogging:

  private val container = new StackPane()
  
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("d, HH:mm:ss").withZone(ZoneId.systemDefault())

  private val nowProperty = LongProperty(System.currentTimeMillis())
  private val timer = new Timeline {
    keyFrames = Seq(
      KeyFrame(Duration(1000), onFinished = _ => nowProperty.value = System.currentTimeMillis())
    )
    cycleCount = Timeline.Indefinite
  }
  timer.play()

  // Rebuild the grid whenever nodeMap changes
  swarmStatus.nodeMap.onChange {
    Platform.runLater {
      buildGrid()
    }
  }

  buildGrid()

  def node: StackPane = container

  private def buildGrid(): Unit =
    val grid = new GridPane():
      hgap = 1
      vgap = 1
      gridLinesVisible = true

    val ourNode = swarmStatus.ourNodeIdentity
    val nodes: Seq[NodeIdentity] = (swarmStatus.nodeMap.keys.toSeq :+ ourNode).distinct.sorted
    val allHours: Set[FdHour] = swarmStatus.nodeMap.values.flatMap(_.map.keys).toSet
    val hours: Seq[FdHour] = allHours.toSeq.sorted

    if nodes.isEmpty then
      container.children = Seq(GridUtils.fieldSet("Swarm Status", new Label("No nodes discovered yet.")))
      return

    // Equal column widths
    val columnPercent = 100.0 / (nodes.size + 1)
    grid.columnConstraints = (0 to nodes.size).map { _ =>
      new ColumnConstraints {
        percentWidth = columnPercent
        hgrow = Priority.Always
      }
    }

    // Background for local node column
    nodes.zipWithIndex.find(_._1 == ourNode).foreach { case (_, colIdx) =>
      val bg = new Region {
        styleClass += "local-node-column"
        mouseTransparent = true
      }
      grid.add(bg, colIdx + 1, 0, 1, hours.size + 3)
    }

    // Header row: Nodes
    grid.add(new Label("Hour \\ Node") {
      style = "-fx-font-weight: bold;"
      maxWidth = Double.MaxValue
      alignment = scalafx.geometry.Pos.Center
    }, 0, 0)
    nodes.zipWithIndex.foreach { case (nodeIdentity, colIdx) =>
      grid.add(new Label(nodeIdentity.instanceId) {
        val ourNodeLine = if nodeIdentity == ourNode then "Our Node\n" else ""
        tooltip =
          s"""${ourNodeLine}IP: ${nodeIdentity.host}
             |Port: ${nodeIdentity.port}
             |InstanceId: ${nodeIdentity.instanceId}
             |""".stripMargin


        style = "-fx-font-weight: bold;"
        maxWidth = Double.MaxValue
        alignment = Pos.Center
      }, colIdx + 1, 0)
    }

    // Row: Total QSOs
    grid.add(new Label("Total QSOs") {
      style = "-fx-font-weight: bold;"
      maxWidth = Double.MaxValue
      alignment = Pos.Center
    }, 0, 1)

    nodes.zipWithIndex.foreach { case (node, colIdx) =>
      val label = new Label() {
        maxWidth = Double.MaxValue
        alignment = Pos.Center
      }
      swarmStatus.nodeMap.get(node).foreach { nodeDetails =>
        label.text <== nodeDetails.qsoCount.asString()
      }
      grid.add(label, colIdx + 1, 1)
    }

    // Row: Last Update
    grid.add(new Label("Last Update") {
      style = "-fx-font-weight: bold;"
      maxWidth = Double.MaxValue
      alignment = Pos.Center
    }, 0, 2)

    nodes.zipWithIndex.foreach { case (node, colIdx) =>
      val label = new Label() {
        maxWidth = Double.MaxValue
        alignment = Pos.Center
      }
      swarmStatus.nodeMap.get(node).foreach { nodeDetails =>
        label.text <== Bindings.createStringBinding(
          () => {
            val last = nodeDetails.lastUpdate.value
            if last == null || last == java.time.Instant.EPOCH then "-"
            else
              // Access nowProperty to trigger re-binding update
              val now = nowProperty.value
              DurationFormat(java.time.Duration.between(last, java.time.Instant.ofEpochMilli(now))) + " ago"
          },
          nodeDetails.lastUpdate,
          nowProperty
        )
        label.tooltip <== Bindings.createObjectBinding(
          () => {
            val last = nodeDetails.lastUpdate.value
            if last == null || last == java.time.Instant.EPOCH then null
            else Tooltip(dateTimeFormatter.format(last)).delegate
          },
          nodeDetails.lastUpdate
        )
      }
      grid.add(label, colIdx + 1, 2)
    }

    // Rows: FdHours
    hours.zipWithIndex.foreach { case (hour, rowIdx) =>
      val gridRow = rowIdx + 3
      grid.add(new Label(hour.display) {
        style = "-fx-font-weight: bold;"
        maxWidth = Double.MaxValue
        alignment = Pos.Center
      }, 0, gridRow)
      
      nodes.zipWithIndex.foreach { case (node, colIdx) =>
        val cell = swarmStatus.nodeMap.get(node) match
          case Some(nodeDetails) =>
            nodeDetails.map.get(hour) match
              case Some(hourNodeCell) =>
                val label = new Label() {
                  maxWidth = Double.MaxValue
                  alignment = Pos.Center
                }
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
                new Label("-") {
                  maxWidth = Double.MaxValue
                  alignment = Pos.Center
                }
          case None =>
            new Label("-") {
              maxWidth = Double.MaxValue
              alignment = Pos.Center
            }
        
        grid.add(cell, colIdx + 1, gridRow)
      }
    }

    val helpText = new Label("TODO: help text below grid") {
      style = "-fx-font-style: italic; -fx-padding: 10 0 0 0;"
    }

    val vBox = new scalafx.scene.layout.VBox {
      children = Seq(grid, helpText)
    }

    container.children = Seq(GridUtils.fieldSet("Swarm Status", vBox))
