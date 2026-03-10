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
import fdswarm.fx.utils.IntLabel
import fdswarm.fx.{GridBuilder, GridColumns}
import fdswarm.replication.NodeDetails
import fdswarm.util.{AgeStyleService, NodeIdentityManager}
import jakarta.inject.{Inject, Singleton}
import scalafx.animation.{KeyFrame, Timeline}
import scalafx.application.Platform
import scalafx.beans.property.LongProperty
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.util.Duration

@Singleton
class SwarmStatusPane @Inject()(ageStyleService: AgeStyleService,
                                swarmStatusApi: SwarmStatusApi,
                                nodeIdentityManager: NodeIdentityManager) extends LazyLogging:

  private val nowProperty = LongProperty(System.currentTimeMillis())

  private val timeline = new Timeline {
    cycleCount = Timeline.Indefinite
    keyFrames = Seq(
      KeyFrame(Duration(1000), onFinished = _ => nowProperty.value = System.currentTimeMillis())
    )
  }
  timeline.play()

  private val container = new BorderPane()

  /**
   * Updates the swarm status pane with the given node map.
   *
   * @param allNodeDetails the whole swarm.
   */
  def update(allNodeDetails: Seq[NodeDetails]): Unit =
    Platform.runLater {
      buildGrid(allNodeDetails)
    }

  def node: BorderPane = container

  def clearData(): Unit =
    val alert = new Alert(Alert.AlertType.Confirmation) {
      title = "Clear Swarm Status"
      headerText = "Clear all swarm status data?"
      contentText = "This will remove all discovered nodes and their QSO counts. This cannot be undone."
    }

    alert.showAndWait() match {
      case Some(ButtonType.OK) => swarmStatusApi.clear()
      case _ =>
    }

  private def buildGrid(allNodeDetails: Seq[NodeDetails]): Unit =
    val ourNode = nodeIdentityManager.nodeIdentity
    val nodes = allNodeDetails.map(_.nodeIdentity).distinct.sorted
    val allHours = allNodeDetails.flatMap(_.map.keys).toSet
    val hours = allHours.toSeq.sorted

    val helpText = new Label("TODO: help text below grid") {
      style = "-fx-font-style: italic; -fx-padding: 10 0 0 0;"
    }

    val footer = new HBox {
      spacing = 20
      alignment = Pos.CenterLeft
      children = Seq(helpText, new Region { hgrow = Priority.Always })
      padding = Insets(10, 0, 0, 0)
    }
    container.bottom = footer

    if nodes.isEmpty then
      container.center = GridColumns.fieldSet("Swarm Status", new Label("No nodes discovered yet."))
      return

    val builder = GridBuilder()
    builder.hgap = 1
    builder.vgap = 1
    builder.padding = Insets(0)
    builder.style = "-fx-background-color: darkgray;"

    // Background for local node column
    nodes.zipWithIndex.find(_._1 == ourNode).foreach { case (_, colIdx) =>
      val bg = new Region {
        styleClass += "local-node-column"
        mouseTransparent = true
      }
      builder.add(bg, colIdx + 1, 0, 1, hours.size + 6) // Updated rowspan for 4 header rows
    }

    val rowStyleCallback: Seq[IntLabel] => String = (labels: Seq[IntLabel]) => {
      logger.trace("rowStyleCallback passed IntLabels: {}", labels)
      val values = labels.map(_.value)
      if (values.isEmpty) ""
      else {
        val counts = values.groupBy(identity).view.mapValues(_.size).toMap
        if (counts.size <= 1) "countGood"
        else
          val majorityValue = counts.maxBy(_._2)._1
          val diffCount = values.count(_ != majorityValue)
          if (diffCount <= 2) "countOff"
          else "countBad"
      }
    }

    val gird = Gird(allNodeDetails.sorted, nowProperty)
    gird.populate(builder, rowStyleCallback)

    container.center = builder.result
