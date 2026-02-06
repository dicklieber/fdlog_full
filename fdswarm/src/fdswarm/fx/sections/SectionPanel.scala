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
import scalafx.scene.layout.{ColumnConstraints, GridPane, Priority, Region}

class SectionPanel @Inject()(sectionsProvider: SectionsProvider, qsoEntryPanel: QsoEntryPanel) extends LazyLogging:
  val gridPane = new GridPane():
    hgap = 5
    vgap = 5
    padding = Insets(5)
    columnConstraints = Seq(
      new ColumnConstraints() { hgrow = Priority.Never },
      new ColumnConstraints() { hgrow = Priority.Always }
    )

  for
    case (sectionGroup, row) <- sectionsProvider.sectionGroups.zipWithIndex
  do
    val nameLabel = new Label(sectionGroup.name):
      minWidth = Region.USE_PREF_SIZE
    gridPane.add(nameLabel, 0, row)
    sectionGroup.sections.foreach(_.onSelect(qsoEntryPanel.sectionFieldProperty))
    val groupGrid = GridUtils.toGrid(sectionGroup.sections, 10)
    groupGrid.maxWidth = Double.MaxValue
    gridPane.add(groupGrid, 1, row)

  val node: Node = GridUtils.fieldSet("Sections", gridPane)

