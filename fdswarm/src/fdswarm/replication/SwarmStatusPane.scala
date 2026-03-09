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
import fdswarm.fx.{GridBuilder, GridColumns}
import fdswarm.fx.qso.FdHour
import fdswarm.util.{AgeStyleService, DurationFormat, NodeIdentity}
import jakarta.inject.{Inject, Singleton}
import scalafx.animation.{KeyFrame, Timeline}
import scalafx.application.Platform
import scalafx.beans.binding.Bindings
import scalafx.beans.property.{LongProperty, StringProperty}
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.{Alert, Button, ButtonType, Label, Tooltip}
import scalafx.scene.layout.{ColumnConstraints, GridPane, Priority, Region, StackPane}
import scalafx.Includes.*
import scalafx.util.Duration

import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Singleton
class SwarmStatusPane @Inject()(swarmStatus: SwarmStatus, ageStyleService: AgeStyleService) extends LazyLogging:

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
      container.children = Seq(GridColumns.fieldSet("Swarm Status", new Label("No nodes discovered yet.")))
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
      grid.add(bg, colIdx + 1, 0, 1, hours.size + 2)
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

        private val ageProperty = StringProperty("")

        swarmStatus.nodeMap.get(nodeIdentity).foreach { nodeDetails =>
          def updateStyle(): Unit =
            Platform.runLater {
              val last = nodeDetails.lastUpdate.value
              styleClass.removeAll("fresh", "recent", "stale")
              if last != null && last != java.time.Instant.EPOCH then
                val styleAndAge = ageStyleService.calc("node", last)
                styleClass.add(styleAndAge.style)
            }

          nodeDetails.lastUpdate.onChange(updateStyle())
          nowProperty.onChange(updateStyle())

          // Initial style
          updateStyle()

          ageProperty <== Bindings.createStringBinding(
            () => {
              val last = nodeDetails.lastUpdate.value
              if nodeIdentity == ourNode || last == null || last == java.time.Instant.EPOCH then ""
              else
                val now = nowProperty.value
                val duration = java.time.Duration.between(last, java.time.Instant.ofEpochMilli(now))
                s"${DurationFormat(duration)} ago"
            },
            nodeDetails.lastUpdate,
            nowProperty
          )
        }

        style = "-fx-font-weight: bold;"

        val tt = new Tooltip {
          styleClass += "tooltip" // Use existing stylesheet style if needed, though Tooltip usually has its own
        }
        val builder = GridBuilder()

        var rowIdx = 0
        if nodeIdentity == ourNode then
          val ourNodeLabel = new Label("Our Node") {
            style = "-fx-font-weight: bold; -fx-text-fill: blue;"
          }
          builder.result.add(ourNodeLabel, 0, rowIdx, 2, 1)
          rowIdx += 1

        swarmStatus.nodeMap.get(nodeIdentity).foreach { _ =>
          if nodeIdentity != ourNode then
            builder("Age:", ageProperty)
            rowIdx += 1
        }

        builder("IP:", nodeIdentity.host)
        builder("Port:", nodeIdentity.port.toString)
        builder("InstanceId:", nodeIdentity.instanceId)

        tt.graphic = builder.result
        tooltip = tt

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

    // Rows: FdHours
    hours.zipWithIndex.foreach { case (hour, rowIdx) =>
      val gridRow = rowIdx + 2
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

    val clearButton = new Button("Clear All Data") {
      styleClass += "clear-button"
      onAction = _ => {
        val alert = new Alert(Alert.AlertType.Confirmation) {
          title = "Clear Swarm Status"
          headerText = "Clear all swarm status data?"
          contentText = "This will remove all discovered nodes and their QSO counts. This cannot be undone."
        }

        alert.showAndWait() match {
          case Some(ButtonType.OK) => swarmStatus.clear()
          case _ =>
        }
      }
    }

    val footer = new scalafx.scene.layout.HBox {
      spacing = 20
      alignment = Pos.CenterLeft
      children = Seq(helpText, new Region { hgrow = Priority.Always }, clearButton)
    }

    val vBox = new scalafx.scene.layout.VBox {
      children = Seq(grid, footer)
    }

    container.children = Seq(GridColumns.fieldSet("Swarm Status", vBox))
