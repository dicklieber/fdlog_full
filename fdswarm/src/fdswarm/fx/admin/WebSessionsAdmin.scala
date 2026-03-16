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

package fdswarm.fx.admin

import fdswarm.fx.InputHelper.forceCaps
import com.typesafe.scalalogging.LazyLogging
import fdswarm.web.{WebSession, WebSessionStore}
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, HBox, Priority, Region, VBox}
import scalafx.stage.Window
import java.time.format.DateTimeFormatter

@Singleton
class WebSessionsAdmin @Inject()(store: WebSessionStore) extends LazyLogging:

  private val timeFmt = DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss")

  def show(ownerWindow: Window): Unit =
    val sessions = store.allSessions

    val listView = new ListView[WebSession](scalafx.collections.ObservableBuffer.from(sessions))
    listView.cellFactory = { (_: javafx.scene.control.ListView[WebSession]) =>
      new javafx.scene.control.ListCell[WebSession] {
        override def updateItem(item: WebSession, empty: Boolean): Unit =
          super.updateItem(item, empty)
          if empty || item == null then setText("")
          else setText(item.station.operator.value)
      }
    }

    val idLabel = new Label("")
    val rigField = new TextField()
    val antennaField = new TextField()
    val opField = new TextField()
    forceCaps(opField)
    val qsoLinesField = new Spinner[Int](1, 200, 10)
    val qsosEnteredLabel = new Label("0")
    val lastTouchedLabel = new Label("")

    listView.getSelectionModel.selectedItemProperty.onChange { (_, _, ws) =>
      if ws != null then
        idLabel.text = ws.sessionId
        rigField.text = ws.station.rig
        antennaField.text = ws.station.antenna
        opField.text = ws.station.operator.value
        qsoLinesField.getValueFactory.setValue(ws.qsoLines)
        store.getStats(ws.sessionId) match
          case Some(stats) =>
            qsosEnteredLabel.text = stats.qsosEntered.toString
            lastTouchedLabel.text = stats.lastTouched.format(timeFmt)
          case None =>
            qsosEnteredLabel.text = "0"
            lastTouchedLabel.text = "N/A"
      else
        idLabel.text = ""
        rigField.text = ""
        antennaField.text = ""
        opField.text = ""
        qsosEnteredLabel.text = "0"
        lastTouchedLabel.text = ""
    }

    def lbl(s: String) = new Label(s) { minWidth = Region.USE_PREF_SIZE }

    val form = new GridPane():
      hgap = 5
      vgap = 5
      padding = Insets(5)
      add(lbl("ID:"), 0, 0)
      add(idLabel, 1, 0)
      add(lbl("Operator:"), 0, 1)
      add(opField, 1, 1)
      add(lbl("Rig:"), 0, 2)
      add(rigField, 1, 2)
      add(lbl("Antenna:"), 0, 3)
      add(antennaField, 1, 3)
      add(lbl("QSO lines:"), 0, 4)
      add(qsoLinesField, 1, 4)
      add(lbl("QSOs Entered:"), 0, 5)
      add(qsosEnteredLabel, 1, 5)
      add(lbl("Last Touched:"), 0, 6)
      add(lastTouchedLabel, 1, 6)

    val saveButton = new Button("Save"):
      onAction = _ =>
        val ws = listView.getSelectionModel.getSelectedItem
        if ws != null then
          val updated = ws.copy(
            station = ws.station.copy(operator = fdswarm.model.Callsign(opField.text.value.toUpperCase), rig = rigField.text.value, antenna = antennaField.text.value),
            qsoLines = qsoLinesField.getValue
          )
          store.saveSession(updated)
          // refresh list label
          listView.refresh()

    val deleteButton = new Button("Delete"):
      style = "-fx-text-fill: red;"
      onAction = _ =>
        val ws = listView.getSelectionModel.getSelectedItem
        if ws != null then
          val alert = new Alert(Alert.AlertType.Confirmation):
            initOwner(ownerWindow)
            title = "Delete Session"
            headerText = s"Delete session for ${ws.station.operator.value}?"
            contentText = "This cannot be undone."

          alert.showAndWait() match
            case Some(ButtonType.OK) =>
              store.deleteSession(ws.sessionId)
              listView.items.get().remove(ws)
              listView.getSelectionModel.clearSelection()
            case _ => ()

    val root = new HBox(10,
      new VBox(5, new Label("Sessions"), listView) { VBox.setVgrow(listView, Priority.Always) },
      new VBox(5, new Label("Details"), form, new HBox(10, saveButton, deleteButton))
    )
    root.padding = Insets(10)

    val d = new Dialog[Unit]():
      initOwner(ownerWindow)
      title = "Web Sessions Admin"
      dialogPane().content = root
      dialogPane().buttonTypes = Seq(ButtonType.Close)

    d.showAndWait()
