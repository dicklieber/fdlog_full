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
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.control.ComboBox

class SectionComboBox(
                       sectionsProvider: SectionsProvider
                     ) extends CustomFieldEditor, CompactComboBoxSupport:

  override def editor(fieldProperty: Any): Node =
    fieldProperty match
      case stringProp: StringProperty =>
        val combo = configureCompactComboBox(new ComboBox[Section])(
          buttonText = _.code,
          listText = s => s"${s.code} - ${s.name}"
        )

        def updateItems(): Unit =
          val choices: Seq[Section] = sectionsProvider.allSections
          combo.items = ObservableBuffer.from(choices)

          val currentSectionStr = stringProp.value
          combo.value =
            if currentSectionStr != null && currentSectionStr.nonEmpty then
              choices.find(_.code == currentSectionStr).orNull
            else
              null

        combo.value.onChange { (_, _, newChoice) =>
          stringProp.value = Option(newChoice).map(_.code).getOrElse("")
        }

        stringProp.onChange { (_, _, newStr) =>
          if newStr != null && newStr.nonEmpty then
            val maybeSection = combo.items.value.find(_.code == newStr)
            if maybeSection.isDefined && combo.value.value != maybeSection.get then
              combo.value = maybeSection.get
          else if combo.value.value != null then
            combo.value = null
        }

        updateItems()
        combo

      case _ =>
        throw new IllegalArgumentException(
          s"SectionComboBox requires StringProperty, got ${fieldProperty.getClass.getSimpleName}"
        )