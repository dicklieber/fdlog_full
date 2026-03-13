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
import fdswarm.fx.qso.FdHour
import fdswarm.fx.utils.IntLabel
import fdswarm.fx.{GridBuilder, GridColumns}
import fdswarm.replication.{NodeDetails, ReceivedNodeStatus}
import fdswarm.store.FdHourDigest
import fdswarm.util.{AgeStyleService, NodeIdentity, NodeIdentityManager}
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
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
      KeyFrame(Duration(3000), onFinished = _ => {
        swarmStatusApi.refresh()
        nowProperty.value = System.currentTimeMillis()
      })
    )
  }
  timeline.play()


  //  private val container = new BorderPane()
  //
  //  val hours = fdHours.toSeq.sorted
  //
  //  def node: BorderPane = container

  def clearData(): Unit =
    val alert = new Alert(Alert.AlertType.Confirmation) {
      title = "Clear Swarm Status"
      headerText = "Clear remote swarm status?"
      contentText = "This will remove all discovered remote nodes and their QSOcounts. The local node status and all QSOs will remain."
    }

    alert.showAndWait() match {
      case Some(ButtonType.OK) => swarmStatusApi.clear()
      case _ =>
    }

  private val helpText = new Label("TODO: help text below grid") {
    style = "-fx-font-style: italic; -fx-padding: 10 0 0 0;"
  }

  private val clearButton = new Button("Clear Remote Data") {
    onAction = _ => clearData()
    minWidth = Region.USE_PREF_SIZE
  }

  private val closeButton = new Button("Close") {
    onAction = _ => container.scene.value.window.value.hide()
    minWidth = Region.USE_PREF_SIZE
    defaultButton = true
  }

  private val footer = new HBox {
    spacing = 20
    alignment = Pos.CenterLeft
    children = Seq(clearButton, closeButton, new Region {
      hgrow = Priority.Always
    }, helpText)
    padding = Insets(10, 0, 0, 0)
  }

  /**
   * This will old the grid pane.
   */
  val container: VBox = new VBox()

  def node: VBox = container

  /**
   * Updates the swarm status pane with the given node map.
   *
   * @param allNodeDetails the whole swarm.
   */
  def update(allNodeDetails: Seq[ReceivedNodeStatus]): Unit =
    Platform.runLater {
      buildGrid(allNodeDetails)
    }

  private def buildGrid(receivedNodeStatuses: Seq[ReceivedNodeStatus]): Unit =
    val builder = GridBuilder()
    builder.hgap = 1
    builder.vgap = 1
    builder.padding = Insets(0)
    builder.style = "-fx-background-color: darkgray;"

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

    val gird: SwarmStatusGrid = SwarmStatusGrid(receivedNodeStatuses, nowProperty, ageStyleService, nodeIdentityManager.nodeIdentity.instanceId, swarmStatusApi)
    gird.populate(builder, rowStyleCallback)
    val gridPane = builder.result
    VBox.setVgrow(gridPane, Priority.Always)
    container.children.setAll(gridPane, footer)
    Option(container.scene.value).foreach(_.window.value.sizeToScene())
