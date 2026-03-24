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

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.contest.{ContestConfig, ContestManager}
import fdswarm.fx.bands.{AvailableBandsManager, AvailableModesManager}
import fdswarm.fx.discovery.{ContestDiscovery, DiscoveryWire}
import fdswarm.fx.station.{StationEditor, StationStore}
import fdswarm.util.NodeIdentity
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.animation.{KeyFrame, Timeline}
import scalafx.application.Platform
import scalafx.beans.property.{BooleanProperty, IntegerProperty, ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.scene.paint.Color
import scalafx.stage.Window
import scalafx.util.Duration
import scala.compiletime.uninitialized

@Singleton
class StartupDialog @Inject() (
    config: Config,
    contestManager: ContestManager,
    contestDiscovery: ContestDiscovery,
    stationStore: StationStore,
    bandsManager: AvailableBandsManager,
    modesManager: AvailableModesManager,
    contestCond: ContestCondition,
    stationCond: StationCondition
) extends LazyLogging:

  private val startupSeconds: Int = config.getInt("fdswarm.autoStartSeconds")

  private val conditions = Seq(contestCond, stationCond)

  private val allOk = BooleanProperty(false)
  private val isDiscovering = BooleanProperty(false)

  private val autoStartSeconds = IntegerProperty(startupSeconds)
  private val autoStartActive = BooleanProperty(false)
  private val timer = new Timeline:
    keyFrames = Seq(
      KeyFrame(
        Duration(1000),
        onFinished = _ =>
          autoStartSeconds.value = autoStartSeconds.value - 1
          if autoStartSeconds.value <= 0 then
            Platform.runLater {
              if startBtnInternal != null && !startBtnInternal.isDisabled then
                startBtnInternal.fire()
            }
      )
    )
    cycleCount = Timeline.Indefinite

  private var startBtnInternal: javafx.scene.control.Button = uninitialized
  private var shouldAutoStart: Boolean = false

  // Set up the listener ONCE since this is a singleton
  allOk.onChange { (_, _, ok) =>
    if ok && shouldAutoStart then
      autoStartSeconds.value = startupSeconds
      autoStartActive.value = true
      timer.playFromStart()
    else
      timer.stop()
      autoStartActive.value = false
  }

  def show(ownerWindow: Window, autoStart: Boolean = true): Boolean =
    shouldAutoStart = autoStart
    val dialog = new Dialog[Boolean]:
      title = "Startup Checks"
      initOwner(ownerWindow)

    val startBtnType = new ButtonType("Start", ButtonBar.ButtonData.OKDone)
    dialog.dialogPane().buttonTypes = Seq(startBtnType)
    val startBtn = dialog.dialogPane().lookupButton(startBtnType).asInstanceOf[javafx.scene.control.Button]
    startBtnInternal = startBtn

    val autoStartLabel = new Label:
      text <== autoStartSeconds.map(s => s"Auto-starting in $s seconds...")
      style = "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
      visible <== autoStartActive
      managed <== autoStartActive

    val grid = new GridPane:
      hgap = 10
      vgap = 10
      padding = Insets(10)

    val col1 = new ColumnConstraints()
    val col2 = new ColumnConstraints { hgrow = Priority.Always }
    val col3 = new ColumnConstraints()
    grid.columnConstraints = Seq(col1, col2, col3)

    conditions.zipWithIndex.foreach { (cond, i) =>
      val label = new Label(cond.name):
        style = "-fx-font-size: 1.0em; -fx-font-weight: bold;"
        minWidth = Region.USE_PREF_SIZE

      val statusIndicator = new Label:
        text <== scalafx.beans.binding.Bindings.createStringBinding(
          () => if cond.ok then "Ok" else cond.problems.mkString(", "),
          cond.problems,
          cond.details
        )
        maxWidth = 400
        wrapText = true
        padding = Insets(0, 5, 0, 5)

      cond.problems.onChange { (_, _) =>
        statusIndicator.styleClass.clear()
        if cond.ok then statusIndicator.styleClass.add("status-ok")
        else statusIndicator.styleClass.add("status-needs-work")
      }
      // Initial state
      if cond.ok then statusIndicator.styleClass.add("status-ok")
      else statusIndicator.styleClass.add("status-needs-work")

      val configBtn = new Button("Configure"):
        onAction = _ => 
          autoStartActive.value = false
          timer.stop()
          cond.editButton(ownerWindow)
        minWidth = Region.USE_PREF_SIZE

      grid.add(label, 0, i)
      grid.add(statusIndicator, 1, i)
      grid.add(configBtn, 2, i)
    }

    val mainView = new VBox(10):
      padding = Insets(15)
      children = Seq(grid, autoStartLabel)
      prefWidth = 650
      visible <== !isDiscovering
      managed <== !isDiscovering

    val discoveryView = new VBox:
      alignment = Pos.Center
      padding = Insets(10)
      prefWidth = 650
      children = Seq(
        new ProgressIndicator:
          maxWidth = 40
          maxHeight = 40
        ,
        new Label("Looking for other FdSwarm nodes..."):
          styleClass.add("parenthetic")
          style = "-fx-font-size: 1.1em;"
          padding = Insets(10, 0, 0, 0)
      )
      visible <== isDiscovering
      managed <== isDiscovering

    dialog.dialogPane().content = new StackPane:
      padding = Insets(0, 0, 10, 0)
      children = Seq(mainView, discoveryView)

    // Initial checks and discovery
    initialChecks()

    // Listen for changes
    val contestListener = contestManager.configProperty.onChange((_, _, _) => runChecks())
    val stationListener = stationStore.station.onChange((_, _, _) => runChecks())
    val bandsListener = bandsManager.bands.onChange((_, _) => runChecks())
    val modesListener = modesManager.modes.onChange((_, _) => runChecks())

    dialog.resultConverter = {
      case `startBtnType` => true
      case _              => false
    }

    try
      dialog.showAndWait() match
        case Some(b: Boolean) => b
        case _ => false
    finally
      timer.stop()
      contestListener.cancel()
      stationListener.cancel()
      bandsListener.cancel()
      modesListener.cancel()

  private var discoveredStations: Map[NodeIdentity, DiscoveryWire] = Map.empty

  private def runChecks(): Unit =
    if isDiscovering.value then return
    conditions.foreach(_.update(discoveredStations))
    allOk.value = conditions.forall(_.ok)

  private def initialChecks(): Unit =
    conditions.foreach(_.update(Map.empty))
    allOk.value = false
    isDiscovering.value = true

    // Start discovery in a background thread to avoid blocking the UI
    new Thread(() => {
//      val discovered = contestDiscovery.discoverContest()
//
//      Platform.runLater {
//        discoveredStations = discovered
//        conditions.foreach(_.update(discovered))
//        allOk.value = conditions.forall(_.ok)
//        isDiscovering.value = false
//      }
    }).start()
