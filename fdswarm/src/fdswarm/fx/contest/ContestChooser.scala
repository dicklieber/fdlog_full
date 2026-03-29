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

package fdswarm.fx.contest

import fdswarm.fx.utils.editor.CustomFieldEditor
import scalafx.beans.property.Property
import scalafx.scene.Node
import scalafx.beans.property.ObjectProperty
import scalafx.scene.control.{OverrunStyle, RadioButton, ToggleGroup}
import scalafx.scene.layout.{Priority, Region, VBox}

/**
 * A custom field editor for selecting a contest type.
 *
 * This class extends the `CustomFieldEditor` trait and provides a user interface
 * for editing properties of type `ObjectProperty[ContestType]`. The UI consists
 * of a group of radio buttons, each corresponding to a value from the `ContestType`
 * enumeration, allowing the user to select one of the predefined contest types.
 *
 * Behavior:
 * - The editor dynamically generates radio buttons based on the available
 * `ContestType` values.
 * - The radio buttons are grouped within a `ToggleGroup`, ensuring that a single
 * selection is allowed at any time.
 * - The currently selected value of the `fieldProperty` is reflected in the UI
 * by pre-selecting the corresponding radio button.
 * - Changes in the radio button selection update the value of the `fieldProperty`
 * accordingly.
 *
 * Typical Usage Scenario:
 * - The `ContestChooser` is designed to be used in forms or property editors
 * where the user needs to specify a contest type such as "Winter Field Day"
 * or "ARRL Field Day".
 *
 * Constraints:
 * - The `fieldProperty` must be of type `ObjectProperty[ContestType]`. If a
 * different property type is passed, an `IllegalArgumentException` is thrown.
 */
class ContestChooser extends CustomFieldEditor:
  override def editor(fieldProperty: Property[?, ?]): Node = fieldProperty match {
    case p: ObjectProperty[?] =>
      val value = p.asInstanceOf[ObjectProperty[ContestType]]
      val tg = new ToggleGroup()
      
      val buttons: Seq[(ContestType, RadioButton)] =
        ContestType.values.toSeq.map: contestType =>
          val button = new RadioButton:
            text = contestType.name
            toggleGroup = tg
            minWidth = Region.USE_PREF_SIZE
            hgrow = Priority.Never
            textOverrun = OverrunStyle.Clip
          contestType -> button
      
      buttons.find(_._1 == value.value).foreach: (_, button) =>
        button.selected = true
      
      tg.selectedToggle.onChange { (_, _, newToggle) =>
        if newToggle != null then
          buttons.find(_._2 == newToggle).foreach: (contestType, _) =>
            if value.value != contestType then
              value.value = contestType
        }
      
      new VBox:
        spacing = 8
        children = buttons.map(_._2)
    case _ =>
      throw new IllegalArgumentException(s"ContestChooser requires ObjectProperty[ContestType], got ${fieldProperty.getClass.getSimpleName}")
  }
 