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

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.GridUtils
import fdswarm.fx.qso.QsoEntryPanel
import jakarta.inject.Inject
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.scene.layout.{HBox, Priority, Region, VBox}

class SectionPanel @Inject()(sectionsProvider: SectionsProvider, qsoEntryPanel: QsoEntryPanel) extends LazyLogging:
  val mainVBox: VBox = new VBox():
    spacing = 10
    padding = Insets(5)

  for
    case (sectionGroup, row) <- sectionsProvider.sectionGroups.zipWithIndex
  do
    val nameLabel = new Label(sectionGroup.name):
      minWidth = Region.USE_PREF_SIZE
      padding = Insets(0, 10, 0, 0)
      style = "-fx-font-weight: bold;"

    sectionGroup.sections.foreach(_.onSelect(qsoEntryPanel.sectionFieldProperty))
    val groupGrid = GridUtils.toGrid(sectionGroup.sections, 10)
    groupGrid.maxWidth = Double.MaxValue
    HBox.setHgrow(groupGrid, Priority.Always)

    val rowContainer = new HBox():
      children = Seq(nameLabel, groupGrid)
      padding = Insets(5)
      style = "-fx-background-color: #f4f4f4; -fx-background-radius: 5; -fx-border-color: #cccccc; -fx-border-radius: 5; -fx-border-width: 1;"
      alignment = scalafx.geometry.Pos.CenterLeft

    mainVBox.children.add(rowContainer)

  val node: Node = GridUtils.fieldSet("Sections", mainVBox)

