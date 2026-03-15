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
import fdswarm.fx.contest.{ContestConfig, ContestDiscovery, ContestManager, ContestStation}
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

trait StartupCondition:
  def name: String
  val problems = ObservableBuffer[String]()
  def ok: Boolean = problems.isEmpty
  val details = StringProperty("")
  def editButton(ownerWindow: Window): Unit
  def update(discovered: Map[NodeIdentity, ContestStation]): Unit

class ContestCondition(
    private val contestManager: ContestManager,
    private val contestDiscovery: ContestDiscovery
) extends StartupCondition:
  override def name: String = "Contest"

  override def editButton(ownerWindow: Window): Unit =
    contestManager.show(ownerWindow)

  override def update(discovered: Map[NodeIdentity, ContestStation]): Unit =
    val config = contestManager.config
    val currentDetails =
      s"Callsign: ${config.ourCallsign}, Contest: ${config.contestType.name}, Class: ${config.ourClass}, Section: ${config.ourSection}"
    
    val localConfigExists = contestManager.configExists
    val discoveryConsistent =
      discovered.isEmpty || discovered.values.forall(_ == contestManager.config)

    val newProblems = scala.collection.mutable.ListBuffer[String]()
    if !localConfigExists then newProblems += "No local configuration found"
    if !discoveryConsistent then newProblems += "Inconsistent with other nodes"
    
    problems.clear()
    problems ++= newProblems
    details.value = if discovered.nonEmpty then
      s"$currentDetails (Found ${discovered.size} other nodes)"
    else currentDetails

class StationCondition(
    private val stationStore: StationStore,
    private val stationEditor: StationEditor
) extends StartupCondition:
  override def name: String = "Station"

  override def editButton(ownerWindow: Window): Unit =
    stationEditor.show(ownerWindow)

  override def update(discovered: Map[NodeIdentity, ContestStation]): Unit =
    val station = stationStore.station.value
    val ourOperator = station.operator
    val duplicateOperator = discovered.values.exists(_.station.operator == ourOperator)

    val newProblems = scala.collection.mutable.ListBuffer[String]()
    if duplicateOperator then
      newProblems += s"Operator $ourOperator is already in use on another node!"
    if !fdswarm.model.Callsign.isValid(station.operator.value) then
      newProblems += s"Invalid Operator callsign: ${station.operator.value}"
    
    problems.clear()
    problems ++= newProblems
    details.value = s"Operator: ${station.operator.value}, Rig: ${station.rig}, Antenna: ${station.antenna}"

@Singleton
class StartupDialog @Inject() (
    config: Config,
    contestManager: ContestManager,
    contestDiscovery: ContestDiscovery,
    stationStore: StationStore,
    stationEditor: StationEditor
) extends LazyLogging:

  private val startupSeconds: Int = config.getInt("fdswarm.autoStartSeconds")

  private val contestCond = ContestCondition(contestManager, contestDiscovery)
  private val stationCond = StationCondition(stationStore, stationEditor)
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
              // This will be set in show()
              if startBtnInternal != null && !startBtnInternal.isDisabled then
                startBtnInternal.fire()
            }
      )
    )
    cycleCount = startupSeconds

  private var startBtnInternal: javafx.scene.control.Button = uninitialized

  allOk.value = conditions.forall(_.ok) && !isDiscovering.value

  def show(ownerWindow: Window, autoStart: Boolean = true): Boolean =
    val dialog = new Dialog[Boolean]:
      title = "Startup Checks"
      initOwner(ownerWindow)

    val startBtnType = new ButtonType("Start", ButtonBar.ButtonData.OKDone)
    dialog.dialogPane().buttonTypes = Seq(startBtnType)
    val startBtn = dialog.dialogPane().lookupButton(startBtnType).asInstanceOf[javafx.scene.control.Button]
    startBtnInternal = startBtn
    startBtn.disable <== !allOk

    allOk.onChange { (_, _, ok) =>
      if ok && autoStart then
        autoStartSeconds.value = startupSeconds
        autoStartActive.value = true
        timer.playFromStart()
      else
        timer.stop()
        autoStartActive.value = false
    }

    // Handle the case where allOk is already true after initial checks
    // This is problematic because allOk is false until initialChecks() finishes
    // But we should ONLY start it if autoStart is true.

    val autoStartLabel = new Label:
      text <== autoStartSeconds.map(s => s"Auto-starting in $s seconds...")
      style = "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
      visible <== autoStartActive
      managed <== autoStartActive

    val rows = conditions.map(cond => createRow(cond, ownerWindow))

    val mainView = new VBox(5):
      padding = Insets(5)
      children = rows ++ Seq(autoStartLabel)
      prefWidth = 500
      visible <== !isDiscovering
      managed <== !isDiscovering

    val discoveryView = new VBox:
      alignment = Pos.Center
      padding = Insets(10)
      prefWidth = 500
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
      children = Seq(mainView, discoveryView)

    // Initial checks and discovery
    initialChecks()

    autoStartActive.value = false // Ensure it's false before potentially setting it true
    if allOk.value && autoStart then
      autoStartSeconds.value = startupSeconds
      autoStartActive.value = true
      timer.playFromStart()

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
      timer.stop()
      contestListener.cancel()
      stationListener.cancel()

  private def createRow(
      condition: StartupCondition,
      ownerWindow: Window
  ): Node =
    val statusIndicator = new Label:
      text <== scalafx.beans.binding.Bindings.createStringBinding(
        () => if condition.ok then "Ok" else s"Needs Work (${condition.problems.mkString(", ")})",
        condition.problems,
        condition.details
      )
      maxWidth = 400
      wrapText = true
      padding = Insets(0, 5, 0, 5)

    condition.problems.onChange { (_, _) =>
      statusIndicator.styleClass.clear()
      if condition.ok then statusIndicator.styleClass.add("status-ok")
      else statusIndicator.styleClass.add("status-needs-work")
    }
    // Initial state
    if condition.ok then statusIndicator.styleClass.add("status-ok")
    else statusIndicator.styleClass.add("status-needs-work")

    val configBtn = new Button("Configure"):
      onAction = _ => 
        autoStartActive.value = false
        timer.stop()
        condition.editButton(ownerWindow)

    new HBox(10):
      alignment = Pos.CenterLeft
      children = Seq(
        new Label(condition.name):
          style = "-fx-font-size: 1.0em; -fx-font-weight: bold;"
          minWidth = 60
        ,
        statusIndicator,
        new Region { hgrow = Priority.Always },
        configBtn
      )

  private var discoveredStations: Map[NodeIdentity, ContestStation] = Map.empty

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
      val discovered = contestDiscovery.discoverContest()

      Platform.runLater {
        discoveredStations = discovered
        conditions.foreach(_.update(discovered))
        allOk.value = conditions.forall(_.ok)
        isDiscovering.value = false
      }
    }).start()
