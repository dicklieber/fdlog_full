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

import fdswarm.fx.utils.IconButton
import fdswarm.util.{LevelEnum, LoggerLevel, LoggingManager}
import jakarta.inject.{Inject, Named, Singleton}
import com.typesafe.scalalogging.LazyLogging
import scalafx.Includes.*
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.input.KeyCode
import scalafx.scene.layout.{GridPane, HBox, Priority, VBox}
import scalafx.scene.paint.Color
import scalafx.stage.Window
import scalafx.util.StringConverter
import scala.jdk.CollectionConverters.*

@Singleton
class LoggingDialog @Inject() (
    loggingManager: LoggingManager,
    @Named("discoveredLoggerNames") discoveredLoggerNamesInjected: Seq[String]
):

  class LoggerRow(logger: String, level: LevelEnum):
    val loggerName = new StringProperty(this, "loggerName", logger)
    val levelProp = new ObjectProperty[LevelEnum](this, "level", level)

  def show(ownerWindow: Window): Unit =
    val rows = ObservableBuffer.from(loggingManager.getLoggers.map(ll => new LoggerRow(ll.logger, ll.level)))

    // Discovered logger names from injected sequence
    val discoveredLoggerNames: Seq[String] =
      discoveredLoggerNamesInjected
        .distinct
        .sorted(using Ordering.String)

    lazy val tv: TableView[LoggerRow] = new TableView[LoggerRow](rows):
      columns ++= Seq(
        new TableColumn[LoggerRow, String]:
          text = "Logger"
          cellValueFactory = { _.value.loggerName }
          prefWidth = 300
        ,
        new TableColumn[LoggerRow, LevelEnum]:
          text = "Level"
          cellValueFactory = { _.value.levelProp }
          cellFactory = (col: TableColumn[LoggerRow, LevelEnum]) =>
            new TableCell[LoggerRow, LevelEnum]:
              item.onChange { (_, _, newLevel) =>
                if newLevel != null then
                  graphic = new ComboBox[LevelEnum](LevelEnum.values.toSeq):
                    value = newLevel
                    onAction = _ =>
                      val row = tv.items.value(index.value)
                      row.levelProp.value = value.value
                      loggingManager.updateLogger(row.loggerName.value, value.value)
                else
                  graphic = null
              }
          prefWidth = 100
      )

    val newLoggerField = new TextField {
      promptText = "New Logger Name"
      hgrow = Priority.Always
    }

    val newLevelCombo = new ComboBox[LevelEnum](LevelEnum.values.toSeq) {
      value = LevelEnum.INFO
    }

    // Button to pick from discovered LazyLogging implementations
    val pickButton = IconButton("search", 24, "Find from list possible loggers")
    pickButton.onAction = _ =>
        // Build popup dialog with filter and list
        val namesAll = discoveredLoggerNames
        val namesBuf = ObservableBuffer.from(namesAll)

        val filterField = new TextField {
          promptText = "Filter loggers"
          hgrow = Priority.Always
        }

        val listView = new ListView[String](namesBuf) {
          prefHeight = 300
        }

        def applyFilter(): Unit =
          val f = Option(filterField.text.value).map(_.trim.toLowerCase).getOrElse("")
          val filtered = if f.isEmpty then namesAll else namesAll.filter(_.toLowerCase.contains(f))
          namesBuf.setAll(filtered*)

        filterField.text.onChange { (_, _, _) => applyFilter() }

        val picker = new Dialog[Unit] {
          title = "Select Logger"
          headerText = "Choose a logger name"
          initOwner(ownerWindow)
        }
        picker.dialogPane().buttonTypes = Seq(ButtonType.Close)
        picker.dialogPane().content = new VBox {
          spacing = 8
          padding = Insets(10)
          children = Seq(
            new HBox {
              spacing = 8
              children = Seq(new Label("Filter:"), filterField)
            },
            listView
          )
        }

        def chooseAndClose(): Unit =
          val sel = listView.selectionModel().getSelectedItem
          if sel != null then
            val level = LevelEnum.DEBUG
            loggingManager.addLogger(sel, level)
            // Update table if not already there, or update existing row
            val existingIndex = rows.indexWhere(_.loggerName.value == sel)
            if existingIndex >= 0 then
              rows(existingIndex).levelProp.value = level
            else
              rows.add(new LoggerRow(sel, level))
            picker.close()

        listView.onMouseClicked = me => if me.clickCount == 2 then chooseAndClose()
        listView.onKeyPressed = ke => if ke.code == KeyCode.Enter then chooseAndClose()

        // initialize filter to show all
        applyFilter()
        picker.showAndWait()

    val addButton = new Button("Add") {
      onAction = _ =>
        val name = newLoggerField.text.value.trim
        if name.nonEmpty then
          val level = newLevelCombo.value.value
          loggingManager.addLogger(name, level)
          rows.add(new LoggerRow(name, level))
          newLoggerField.clear()
    }

    val removeAllButton = IconButton("trash3-fill", 24, "Remove all loggers", Color.Red)
    removeAllButton.styleClass += "discardButton"
    removeAllButton.onAction = _ =>
        loggingManager.removeAllLoggers()
        rows.clear()

    val addBox = new HBox {
      spacing = 10
      children = Seq(pickButton, newLoggerField, newLevelCombo, addButton, removeAllButton)
    }

    val dialog = new Dialog[Unit] {
      title = "Logging Configuration"
      headerText = "Manage Log4j2 Logger Levels"
      initOwner(ownerWindow)
    }

    dialog.dialogPane().buttonTypes = Seq(ButtonType.Close)
    dialog.dialogPane().content = new VBox {
      spacing = 10
      padding = Insets(10)
      prefWidth = 450
      prefHeight = 500
      children = Seq(tv, addBox)
    }

    dialog.showAndWait()
