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

package fdswarm.fx.tools

import fdswarm.fx.contest.{ContestConfig, ContestDiscovery}
import fdswarm.util.NodeIdentity
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.animation.{KeyFrame, Timeline}
import scalafx.application.Platform
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.scene.control.*
import scalafx.scene.layout.{HBox, Priority, Region, VBox}
import scalafx.stage.Window
import scalafx.util.Duration

import java.time.format.DateTimeFormatter

@Singleton
final class DiscoveryDialogService @Inject() (
    contestDiscovery: ContestDiscovery
) {

  private case class DiscoveryResult(node: NodeIdentity, config: ContestConfig)

  private val resultsBuffer = ObservableBuffer.empty[DiscoveryResult]

  private val table = new TableView[DiscoveryResult](resultsBuffer) {
    columns ++= List(
      new TableColumn[DiscoveryResult, String] {
        text = "Node"
        cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.node.toString) }
        prefWidth = 150
      },
      new TableColumn[DiscoveryResult, String] {
        text = "ID"
        cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.config.id) }
        prefWidth = 150
      },
      new TableColumn[DiscoveryResult, String] {
        text = "Callsign"
        cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.config.ourCallsign.toString) }
        prefWidth = 100
      },
      new TableColumn[DiscoveryResult, String] {
        text = "Contest"
        cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.config.contestType.name) }
        prefWidth = 150
      },
      new TableColumn[DiscoveryResult, String] {
        text = "Class"
        cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.config.ourClass) }
        prefWidth = 50
      },
      new TableColumn[DiscoveryResult, String] {
        text = "Section"
        cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.config.ourSection) }
        prefWidth = 80
      },
      new TableColumn[DiscoveryResult, String] {
        text = "Tx"
        cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.config.transmitters.toString) }
        prefWidth = 50
      },
      new TableColumn[DiscoveryResult, String] {
        text = "Stamp"
        cellValueFactory = { cellData =>
          ReadOnlyStringWrapper(
            cellData.value.config.stamp.toString
          )
        }
        prefWidth = 200
      }
    )
  }

  def show(ownerWindow: Window): Unit = {
    val progressBar = new ProgressBar {
      prefWidth = 200
      progress = 0
      visible = false
      managed = false
    }

    val statusLabel = new Label {
      visible = false
      managed = false
    }

    val discoverButton = new Button("Discover") {
      minWidth = Region.USE_PREF_SIZE
      onAction = _ => {
        disable = true
        resultsBuffer.clear()
        progressBar.visible = true
        progressBar.managed = true
        progressBar.progress = 0
        statusLabel.visible = true
        statusLabel.managed = true
        statusLabel.text = "Searching..."

        val totalMs = contestDiscovery.timeoutSec * 1000.0
        val updateIntervalMs = 50.0
        val startTime = System.currentTimeMillis()
        var lastResponseTime = startTime
        var responseCount = 0

        val timeline = new Timeline {
          cycleCount = (totalMs / updateIntervalMs).toInt
          keyFrames = Seq(
            KeyFrame(
              Duration(updateIntervalMs),
              onFinished = (_: ActionEvent) => {
                progressBar.progress.value =
                  progressBar.progress.value + (updateIntervalMs / totalMs)
              }
            )
          )
        }

        timeline.play()

        // Run discovery in a background thread to keep UI responsive
        new Thread(() => {
          val results = contestDiscovery.discoverContest((_, _) => {
            Platform.runLater {
              responseCount += 1
              lastResponseTime = System.currentTimeMillis()
              statusLabel.text = s"Received $responseCount responses"
            }
          })
          Platform.runLater {
            resultsBuffer ++= results.map { case (node, config) =>
              DiscoveryResult(node, config)
            }
            disable = false
            progressBar.visible = false
            progressBar.managed = false
            timeline.stop()

            val durationSec = (lastResponseTime - startTime) / 1000.0
            val totalTimeoutSec = contestDiscovery.timeoutSec
            val wastedSec = totalTimeoutSec - durationSec

            if (responseCount > 0) {
              statusLabel.text =
                s"Discovered $responseCount nodes in ${"%.2f".format(durationSec)}s (${"%.2f".format(wastedSec)}s wasted)"
            } else {
              statusLabel.text = s"No nodes discovered (${"%.2f".format(totalTimeoutSec)}s wasted)"
            }
          }
        }).start()
      }
    }

    val dialog = new Dialog[Unit]() {
      initOwner(ownerWindow)
      title = "Contest Discovery"
      headerText = "Discover contest configurations from other nodes"
      initModality(scalafx.stage.Modality.None)
    }

    val dialogPane = dialog.dialogPane.value
    dialogPane.content = new VBox {
      spacing = 10
      padding = scalafx.geometry.Insets(10)
      children = Seq(
        new HBox {
          spacing = 10
          alignment = scalafx.geometry.Pos.CenterLeft
          children = Seq(discoverButton, progressBar, statusLabel)
          HBox.setHgrow(progressBar, Priority.Always)
        },
        table
      )
    }
    dialogPane.buttonTypes = Seq(ButtonType.OK)

    dialog.show()
  }
}
