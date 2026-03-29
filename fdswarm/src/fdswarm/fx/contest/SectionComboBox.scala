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

import fdswarm.fx.sections.{Section, SectionsProvider}
import fdswarm.fx.utils.editor.CustomFieldEditor
import scalafx.Includes.*
import scalafx.beans.property.{Property, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.control.{ComboBox, ListCell, ListView}

class SectionComboBox(
  sectionsProvider: SectionsProvider
) extends CustomFieldEditor:
  override def editor(fieldProperty: Property[?, ?]): Node =
    val stringProp = fieldProperty.asInstanceOf[StringProperty]

    val combo = new ComboBox[Section]:
      cellFactory = (lv: ListView[Section]) => new ListCell[Section]:
        item.onChange(
          (_, _, newValue) => text = Option(newValue).map(s => s"${s.code} - ${s.name}").getOrElse("")
        )
      buttonCell = new ListCell[Section]:
        item.onChange(
          (_, _, newValue) => text = Option(newValue).map(_.code).getOrElse("")
        )

    def updateItems(): Unit =
      val choices: Seq[Section] = sectionsProvider.allSections
      combo.items = ObservableBuffer.from(choices)
      // Preserve selection if possible
      val currentSectionStr = stringProp.value
      combo.value = if currentSectionStr != null && currentSectionStr.nonEmpty && choices.exists(_.code == currentSectionStr) then
        choices.find(_.code == currentSectionStr).get
      else
        null.asInstanceOf[Section]

    combo.value.onChange(
      (_, _, newChoice) =>
        stringProp.value = Option(newChoice).map(_.code).getOrElse("")
    )

    stringProp.onChange(
      (_, _, newStr) =>
        if newStr != null && newStr.nonEmpty then
          combo.items.value.find(_.code == newStr).foreach(combo.value = _)
        else
          combo.value = null.asInstanceOf[Section]
    )

    updateItems()
    combo