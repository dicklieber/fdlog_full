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
import fdswarm.DebugConfig
import fdswarm.fx.bandmodes.{BandModeMatrixPane, SelectedBandModeStore}
import fdswarm.model.{BandMode, Callsign}
import javafx.collections.ListChangeListener
import net.codingwell.scalaguice.InjectorExtensions.*
import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{ColumnConstraints, GridPane, Priority, VBox}

final class NodeConfigGridPane(
  nodeConfigManager: NodeConfigManager,
  injector: Injector,
  ownerWindow: javafx.stage.Window
) extends ScrollPane:

  fitToWidth = true
  pannable = true

  private val grid = new GridPane:
    hgap = 10
    vgap = 8
    padding = Insets(10)
    columnConstraints ++= Seq(
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

  content = new VBox:
    children = Seq(grid)

  private def addHeaderRow(): Unit =
    val headers = Seq("Id", "Operator", "BandMode", "Startup Config", "Clear QSOs", "Delete")
    headers.zipWithIndex.foreach { case (title, col) =>
      grid.add(
        new Label(title):
          style = "-fx-font-weight: bold;"
        ,
        col,
        0
      )
    }

  private def openBandModeDialog(index: Int): Unit =
    if index >= 0 && index < nodeConfigManager.observableBuffer.size then
      val oldConfig = nodeConfigManager.observableBuffer(index)
      val matrixPane = injector.instance[BandModeMatrixPane]
      val selectedStore = injector.instance[SelectedBandModeStore]
      selectedStore.save(oldConfig.bandMode)

      val dialog = new Dialog[BandMode]:
        initOwner(ownerWindow)
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

  private def operatorField(index: Int, config: DebugConfig): TextField =
    new TextField:
      text = config.operator.value
      textFormatter = new javafx.scene.control.TextFormatter[String](
        new java.util.function.UnaryOperator[javafx.scene.control.TextFormatter.Change]:
          override def apply(change: javafx.scene.control.TextFormatter.Change): javafx.scene.control.TextFormatter.Change =
            if change.isContentChange then change.setText(change.getText.toUpperCase)
            change
      )
      focused.onChange { (_, hadFocus, hasFocus) =>
        if hadFocus && !hasFocus && index < nodeConfigManager.observableBuffer.size then
          val updated = text.value.trim
          val old = nodeConfigManager.observableBuffer(index)
          if old.operator.value != updated && updated.nonEmpty then
            nodeConfigManager.observableBuffer(index) = old.copy(operator = Callsign(updated))
      }

  private def startupCheckBox(index: Int, config: DebugConfig): CheckBox =
    new CheckBox:
      selected = config.showStartupConfig
      selected.onChange { (_, _, newValue) =>
        if index < nodeConfigManager.observableBuffer.size then
          val old = nodeConfigManager.observableBuffer(index)
          if old.showStartupConfig != newValue then
            nodeConfigManager.observableBuffer(index) = old.copy(showStartupConfig = newValue)
      }

  private def clearQsosCheckBox(index: Int, config: DebugConfig): CheckBox =
    new CheckBox:
      selected = config.clearQsos
      selected.onChange { (_, _, newValue) =>
        if index < nodeConfigManager.observableBuffer.size then
          val old = nodeConfigManager.observableBuffer(index)
          if old.clearQsos != newValue then
            nodeConfigManager.observableBuffer(index) = old.copy(clearQsos = newValue)
      }

  private def refreshGrid(): Unit =
    grid.children.clear()
    addHeaderRow()

    nodeConfigManager.observableBuffer.zipWithIndex.foreach { case (config, index) =>
      val row = index + 1

      grid.add(new Label(config.id), 0, row)
      grid.add(operatorField(index, config), 1, row)
      grid.add(
        new Button(config.bandMode.toString):
          maxWidth = Double.MaxValue
          onAction = _ => openBandModeDialog(index)
        ,
        2,
        row
      )
      grid.add(startupCheckBox(index, config), 3, row)
      grid.add(clearQsosCheckBox(index, config), 4, row)
      grid.add(
        new Button("Delete"):
          onAction = _ =>
            if index < nodeConfigManager.observableBuffer.size then
              nodeConfigManager.observableBuffer.remove(index)
        ,
        5,
        row
      )
    }

  nodeConfigManager.observableBuffer.delegate.addListener(
    new ListChangeListener[DebugConfig]:
      override def onChanged(change: ListChangeListener.Change[? <: DebugConfig]): Unit =
        refreshGrid()
  )

  refreshGrid()
