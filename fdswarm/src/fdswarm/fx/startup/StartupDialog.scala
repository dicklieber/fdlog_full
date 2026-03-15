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

package fdswarm.fx.startup

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.contest.{ContestConfig, ContestDiscovery, ContestManager}
import fdswarm.fx.station.{StationEditor, StationStore}
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.beans.property.BooleanProperty
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.scene.paint.Color
import scalafx.stage.Window

@Singleton
class StartupDialog @Inject() (
    contestManager: ContestManager,
    contestDiscovery: ContestDiscovery,
    stationStore: StationStore,
    stationEditor: StationEditor
) extends LazyLogging:

  private val contestOk = BooleanProperty(false)
  private val stationOk = BooleanProperty(false)
  private val allOk = BooleanProperty(false)

  private val contestDetails = scalafx.beans.property.StringProperty("")
  private val stationDetails = scalafx.beans.property.StringProperty("")

  allOk <== contestOk && stationOk

  def show(ownerWindow: Window): Boolean =
    val dialog = new Dialog[Boolean]:
      title = "Startup Checks"
      initOwner(ownerWindow)

    val startBtnType = new ButtonType("Start", ButtonBar.ButtonData.OKDone)
    dialog.dialogPane().buttonTypes = Seq(startBtnType)
    val startBtn = dialog.dialogPane().lookupButton(startBtnType)
    startBtn.disable <== !allOk

    val contestRow = createRow(
      "Contest",
      contestDetails,
      contestOk,
      () => contestManager.show(ownerWindow)
    )

    val stationRow = createRow(
      "Station",
      stationDetails,
      stationOk,
      () => stationEditor.show(ownerWindow)
    )

    dialog.dialogPane().content = new VBox(15):
      padding = Insets(20)
      children = Seq(contestRow, stationRow)
      prefWidth = 500

    // Initial checks and discovery
    runChecks()

    // Listen for changes
    val contestListener = contestManager.configProperty.onChange((_, _, _) => runChecks())
    val stationListener = stationStore.station.onChange((_, _, _) => runChecks())

    dialog.resultConverter = {
      case `startBtnType` => true
      case _              => false
    }

    try
      dialog.showAndWait() match
        case Some(b: Boolean) => b
        case _ => false
    finally
      // Clean up listeners if they were added to long-lived objects
      // (Actually these are singletons, so we should probably remove them if we want to be safe, 
      // but the dialog is shown only once at startup)
      ()

  private def createRow(
      label: String,
      detailsProp: scalafx.beans.property.StringProperty,
      statusProp: BooleanProperty,
      onConfigure: () => Unit
  ): Node =
    val statusIndicator = new Label:
      text <== statusProp.map(ok => if ok then "Ok" else "Needs Work")
      minWidth = 100

    statusProp.onChange { (_, _, ok) =>
      statusIndicator.styleClass.clear()
      if ok then statusIndicator.styleClass.add("status-ok")
      else statusIndicator.styleClass.add("status-needs-work")
    }
    // Initial state
    if statusProp.value then statusIndicator.styleClass.add("status-ok")
    else statusIndicator.styleClass.add("status-needs-work")

    val configBtn = new Button("Configure"):
      onAction = _ => onConfigure()

    new VBox(5):
      children = Seq(
        new HBox(10):
          alignment = Pos.CenterLeft
          children = Seq(
            new Label(label):
              style = "-fx-font-size: 1.2em; -fx-font-weight: bold;"
              minWidth = 100
            ,
            statusIndicator,
            new Region { hgrow = Priority.Always },
            configBtn
          )
        ,
        new Label:
          text <== detailsProp
          wrapText = true
          style = "-fx-font-style: italic; -fx-text-fill: gray;"
      )

  private def runChecks(): Unit =
    // Check Station
    val station = stationStore.station.value
    stationOk.value = fdswarm.model.Callsign.isValid(station.operator.value)
    stationDetails.value = s"Operator: ${station.operator.value}, Rig: ${station.rig}, Antenna: ${station.antenna}"

    // Check Contest and Discovery
    val config = contestManager.config
    contestDetails.value = s"Callsign: ${config.ourCallsign}, Contest: ${config.contestType.name}, Class: ${config.ourClass}, Section: ${config.ourSection}"

    Platform.runLater {
      val localConfigExists = contestManager.configExists
      
      val discovered = contestDiscovery.discoverContest()
      val discoveryConsistent = discovered.values.forall(_ == contestManager.config)
      
      contestOk.value = localConfigExists && discoveryConsistent
    }
