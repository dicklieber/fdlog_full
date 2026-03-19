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

package manager

import com.google.inject.Injector
import fdswarm.{StartupConfig, DebugMode}
import fdswarm.fx.bandmodes.{BandModeMatrixPane, SelectedBandModeStore}
import fdswarm.model.{BandMode, Callsign}
import javafx.collections.ListChangeListener
import net.codingwell.scalaguice.InjectorExtensions.*
import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.collections.ObservableBuffer
import scalafx.scene.layout.{ColumnConstraints, GridPane, Priority, VBox}
import scalafx.application.Platform
import scalafx.stage.Stage

final class NodeConfigGridPane(
  nodeConfigManager: NodeConfigManager,
  injector: Injector,
  ownerStage: scalafx.stage.Stage
) extends VBox:

  private var enableBulkNext: Boolean = false
  private var clearQsosBulkNext: Boolean = false
  private val grid = new GridPane:
    hgap = 10
    vgap = 8
    padding = Insets(10)
    columnConstraints ++= Seq(
      new ColumnConstraints:
        hgrow = Priority.Never
      ,
      new ColumnConstraints:
        hgrow = Priority.Never
      ,
      new ColumnConstraints:
        hgrow = Priority.Always
        fillWidth = true
      ,
      new ColumnConstraints:
        hgrow = Priority.Always
        fillWidth = true
      ,
      new ColumnConstraints:
        hgrow = Priority.Never
      ,
      new ColumnConstraints:
        hgrow = Priority.Never
      ,
      new ColumnConstraints:
        hgrow = Priority.Never
    )

  children = Seq(grid)

  private def addHeaderRow(): Unit =
    Seq(
      ("Id", 0),
      ("Enable", 1),
      ("Operator", 2),
      ("BandMode", 3),
      ("Debug", 4),
      ("Clear QSOs", 5),
      ("Delete", 6)
    ).foreach { case (title, col) =>
      val header =
        if col == 1 then
          new Button("Enable") {
            style = "-fx-font-weight: bold; -fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand;"
            onAction = _ => toggleAllEnables()
            ellipsisString = ""
          }
        else if col == 5 then
          new Button("Clear QSOs") {
            style = "-fx-font-weight: bold; -fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: hand;"
            onAction = _ => toggleAllClearQsos()
            ellipsisString = ""
          }
        else
          new Label(title) {
            style = "-fx-font-weight: bold;"
            ellipsisString = ""
          }
      grid.add(header, col, 0)
    }

  private def openBandModeDialog(index: Int): Unit =
    if index >= 0 && index < nodeConfigManager.observableBuffer.size then
      val oldConfig = nodeConfigManager.observableBuffer(index)
      val matrixPane = injector.instance[BandModeMatrixPane]
      val selectedStore = injector.instance[SelectedBandModeStore]
      selectedStore.save(oldConfig.bandMode)

      val dialog = new Dialog[BandMode]:
        initOwner(ownerStage.delegate)
        title = "Select BandMode"
        headerText = s"Select BandMode for ${oldConfig.id}"

      matrixPane.showConfigButton.value = false
      dialog.dialogPane().content = matrixPane.node
      dialog.dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)
      dialog.resultConverter = {
        case ButtonType.OK => selectedStore.selected.value
        case _             => null
      }

      dialog.showAndWait() match
        case Some(bm: BandMode) =>
          nodeConfigManager.observableBuffer(index) = oldConfig.copy(bandMode = bm)
        case _ =>

  private def operatorField(index: Int, config: StartupConfig): TextField =
    new TextField:
      text = config.operator.value
      textFormatter = new javafx.scene.control.TextFormatter[String](
        new java.util.function.UnaryOperator[javafx.scene.control.TextFormatter.Change]:
          override def apply(change: javafx.scene.control.TextFormatter.Change): javafx.scene.control.TextFormatter.Change =
            if change.isContentChange then change.setText(change.getText.toUpperCase)
            change
      )
      focused.onChange { (_, oldFocus, newFocus) =>
        if oldFocus && !newFocus && index < nodeConfigManager.observableBuffer.size then
          val updated = text.value.trim
          val old = nodeConfigManager.observableBuffer(index)
          if old.operator.value != updated && updated.nonEmpty then
            nodeConfigManager.observableBuffer(index) = old.copy(operator = Callsign(updated))
      }


  private def enableCheckBox(index: Int, config: StartupConfig): CheckBox =
    new CheckBox:
      selected = config.enable
      selected.onChange { (_, _, newValue) =>
        if index < nodeConfigManager.observableBuffer.size then
          val old = nodeConfigManager.observableBuffer(index)
          if old.enable != newValue then
            nodeConfigManager.observableBuffer(index) = old.copy(enable = newValue)
      }

  private def clearQsosCheckBox(index: Int, config: StartupConfig): CheckBox =
    new CheckBox:
      selected = config.clearQsos
      selected.onChange { (_, _, newValue) =>
        if index < nodeConfigManager.observableBuffer.size then
          val old = nodeConfigManager.observableBuffer(index)
          if old.clearQsos != newValue then
            nodeConfigManager.observableBuffer(index) = old.copy(clearQsos = newValue)
      }

  private def debugCombo(index: Int, config: StartupConfig): ComboBox[DebugMode] =
    new ComboBox[DebugMode](ObservableBuffer(DebugMode.values.toSeq *)):
      value = config.debugMode
      value.onChange { (_, _, newValue) =>
        if index < nodeConfigManager.observableBuffer.size && newValue != null then
          if newValue == DebugMode.Debug || newValue == DebugMode.Wait then {
            val bufferSize = nodeConfigManager.observableBuffer.size
            for (j <- 0 until bufferSize if j != index) {
              val otherOld = nodeConfigManager.observableBuffer(j)
              nodeConfigManager.observableBuffer(j) = otherOld.copy(debugMode = DebugMode.Off)
            }
          }
          val old = nodeConfigManager.observableBuffer(index)
          nodeConfigManager.observableBuffer(index) = old.copy(debugMode = newValue)
      }
  
  private def toggleAllEnables(): Unit =
    val target = enableBulkNext
    val buffer = nodeConfigManager.observableBuffer
    for (i <- 0 until buffer.size) {
      buffer(i) = buffer(i).copy(enable = target)
    }
    enableBulkNext = !enableBulkNext
  
  private def toggleAllClearQsos(): Unit =
    val target = clearQsosBulkNext
    val buffer = nodeConfigManager.observableBuffer
    for (i <- 0 until buffer.size) {
      buffer(i) = buffer(i).copy(clearQsos = target)
    }
    clearQsosBulkNext = !clearQsosBulkNext
  
  private def refreshGrid(): Unit =
    grid.children.clear()
    addHeaderRow()

    nodeConfigManager.observableBuffer.zipWithIndex.foreach { case (config, index) =>
      val row = index + 1

      grid.add(new Label(config.id) { ellipsisString = "" }, 0, row)
      grid.add(enableCheckBox(index, config), 1, row)
      grid.add(operatorField(index, config), 2, row)
      grid.add(
        new Button(config.bandMode.toString):
          maxWidth = Double.MaxValue
          ellipsisString = ""
          onAction = _ => openBandModeDialog(index)
        ,
        3,
        row
      )
      grid.add(debugCombo(index, config), 4, row)
      grid.add(clearQsosCheckBox(index, config), 5, row)
      grid.add(
        new Button("Delete"):
          ellipsisString = ""
          onAction = _ =>
            if index < nodeConfigManager.observableBuffer.size then
              nodeConfigManager.observableBuffer.remove(index)
        ,
        6,
        row
      )
    }

  nodeConfigManager.observableBuffer.delegate.addListener(
    new ListChangeListener[StartupConfig]:
      override def onChanged(change: ListChangeListener.Change[? <: StartupConfig]): Unit =
        refreshGrid()
        nodeConfigManager.persist()
        grid.requestLayout()
        Platform.runLater { () => ownerStage.sizeToScene() }
  )

  refreshGrid()
  grid.requestLayout()
  Platform.runLater { () => ownerStage.sizeToScene() }
