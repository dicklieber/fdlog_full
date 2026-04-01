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
import scalafx.Includes.*
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.control.ComboBox

class ClassComboBox(
                     catalog: ContestCatalog,
                     contestTypeProperty: ObjectProperty[ContestType]
                   ) extends CustomFieldEditor, CompactComboBoxSupport:

  override def editor(fieldProperty: Any): Node =
    val stringProp = fieldProperty.asInstanceOf[StringProperty]

    val combo = configureCompactComboBox(new ComboBox[ClassChoice])(
      buttonText = _.ch,
      listText = _.label
    )

    def updateItems(): Unit =
      val contestType = contestTypeProperty.value
      val choices: Seq[ClassChoice] =
        if contestType != null then
          catalog.getContest(contestType).map(_.classChoices).getOrElse(Seq.empty)
        else
          Seq.empty

      combo.items = ObservableBuffer.from(choices)

      val currentClassStr = stringProp.value
      combo.value =
        if currentClassStr != null && currentClassStr.nonEmpty && choices.nonEmpty then
          choices.find(_.ch == currentClassStr).orNull
        else
          null.asInstanceOf[ClassChoice]

    contestTypeProperty.onChange { (_, _, _) =>
      updateItems()
    }

    combo.value.onChange { (_, _, newChoice) =>
      stringProp.value = Option(newChoice).map(_.ch).getOrElse("")
    }

    stringProp.onChange { (_, _, newStr) =>
      if newStr != null && newStr.nonEmpty then
        combo.items.value.find(_.ch == newStr) match
          case Some(choice) =>
            if combo.value.value != choice then
              combo.value = choice
          case None =>
            combo.value = null.asInstanceOf[ClassChoice]
      else
        combo.value = null.asInstanceOf[ClassChoice]
    }

    updateItems()
    combo