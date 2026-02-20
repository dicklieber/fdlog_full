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

package fdswarm.fx.sections

import scalafx.beans.property.StringProperty
import scalafx.geometry.Insets
import scalafx.scene.control.{Label, OverrunStyle, Tooltip}
import io.circe.Codec

case class SectionGroup(name: String, sections: Seq[Section]) derives Codec.AsObject

case class Section(code: String, name: String) extends Label derives Codec.AsObject:
  text = code
  tooltip = Tooltip(name)
  padding = Insets(2, 4, 2, 4)
  minWidth = 0
  textOverrun = OverrunStyle.Clip

  def onSelect(sectionField: StringProperty): Unit =
    onMouseClicked = _ => sectionField.value = code
    onMouseEntered = _ => style = "-fx-background-color: lightgray; -fx-cursor: hand;"
    onMouseExited = _ => style = "-fx-background-color: transparent; -fx-cursor: default;"

    sectionField.onChange { (_, _, newValue) =>
      val input = if newValue == null then "" else newValue.trim.toUpperCase
      if input.nonEmpty && code.toUpperCase.startsWith(input) then
        if !styleClass.contains("section-match") then styleClass.add("section-match")
      else
        styleClass.remove("section-match")
    }

import jakarta.inject.{Inject, Singleton}
@Singleton
class Sections @Inject()(sectionsProvider: SectionsProvider):
  val all: Seq[Section] = sectionsProvider.allSections
