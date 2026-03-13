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
import fdswarm.util.TimeHelpers
import scalafx.Includes.*
import scalafx.beans.property.ObjectProperty
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, HBox, Region}
import scalafx.stage.{Modality, Stage}

import java.time.{LocalTime, ZonedDateTime}

class ZonedDateTimeEditor(initial: ZonedDateTime, editorTitle: String) extends HBox(5) {
  val valueProperty: ObjectProperty[ZonedDateTime] = ObjectProperty[ZonedDateTime](this, "value", initial)
  def value: ZonedDateTime = valueProperty.value
  def value_=(zdt: ZonedDateTime): Unit = valueProperty.value = zdt

  private val label = new Label(TimeHelpers.formatter.format(initial)) {
    minWidth = Region.USE_PREF_SIZE
  }

  private val editButton = IconButton("pencil", 16, s"Edit $editorTitle")

  editButton.onAction = _ => showEditDialog()

  children = Seq(label, editButton)

  valueProperty.onChange { (_, _, newValue) =>
    label.text = TimeHelpers.formatter.format(newValue)
  }

  private def showEditDialog(): Unit = {
    val current = valueProperty.value
    val datePicker = new DatePicker(current.toLocalDate)
    val hourSpinner = new Spinner[Int](0, 23, current.getHour) {
      prefWidth = 70
      minWidth = Region.USE_PREF_SIZE
    }
    val minSpinner = new Spinner[Int](0, 59, current.getMinute) {
      prefWidth = 70
      minWidth = Region.USE_PREF_SIZE
    }

    val dialog = new Stage() {
      title = s"Edit $editorTitle"
      initModality(Modality.ApplicationModal)
      initOwner(editButton.getScene.getWindow)
      resizable = false
      scene = new Scene {
        root = new GridPane {
          hgap = 10
          vgap = 10
          padding = scalafx.geometry.Insets(10)
          add(new Label("Date:"), 0, 0)
          add(datePicker, 1, 0)
          add(new Label("Hour:"), 0, 1)
          add(hourSpinner, 1, 1)
          add(new Label("Minute:"), 0, 2)
          add(minSpinner, 1, 2)

          val okButton = new Button("OK") {
            onAction = _ => {
              valueProperty.value = ZonedDateTime.of(
                datePicker.value.value,
                LocalTime.of(hourSpinner.value.value, minSpinner.value.value),
                current.getZone
              )
              close()
            }
          }
          val cancelButton = new Button("Cancel") {
            onAction = _ => close()
          }
          val buttonBox = new HBox(10, okButton, cancelButton)
          add(buttonBox, 1, 3)
        }
      }
    }
    dialog.showAndWait()
  }

  def setOnAction(f: => Unit): Unit = {
    valueProperty.onChange { (_, _, _) => f }
  }

  override def disable_=(v: Boolean): Unit = {
    super.disable = v
    label.disable = v
    editButton.disable = v
  }
}
