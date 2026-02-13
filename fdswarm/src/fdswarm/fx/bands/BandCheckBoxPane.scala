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

package fdswarm.fx.bands

import fdswarm.fx.GridUtils
import fdswarm.io.DirectoryProvider
import fdswarm.model.BandMode.Band
import jakarta.inject.{Inject, Singleton}
import os.Path
import scalafx.scene.Node
import scalafx.scene.control.{CheckBox, Label, Tooltip}
import scalafx.scene.layout.GridPane

@Singleton
final class BandCheckBoxPane @Inject()(
                                        dirProvider: DirectoryProvider,
                                        hamBandCatalog: BandCatalog,
                                        availableBandsManager: AvailableBandsManager
                                      ):

  private val grid = new GridPane:
    hgap = 12.0
    vgap = 6.0

  private val checkBoxes: Seq[BandCheckBox] =
    hamBandCatalog.hamBands.map(BandCheckBox(_))

  private val byBandClass: Map[BandClass, Seq[BandCheckBox]] =
    checkBoxes.groupBy(_.hamBand.bandClass)

  // layout
  for
    (bandClass, row) <- BandClass.values.zipWithIndex
    if byBandClass.contains(bandClass)
    _ = grid.addRow(row, new Label(bandClass.toString))
    (bandCheckBox, col) <- byBandClass(bandClass).zipWithIndex
  do
    grid.add(bandCheckBox, col + 1, row)

  val node: Node =
    GridUtils.fieldSet("Ham bands", grid)

  private def checked: Seq[Band] =
    checkBoxes.iterator.filter(_.selected.value).map(_.bandName).toSeq

  final case class BandCheckBox(hamBand: HamBand) extends CheckBox:
    text = hamBand.bandName

    // initialize selection from persisted bands
    selected = availableBandsManager.bands.contains(hamBand.bandName)

    tooltip = new Tooltip(
      s"${hamBand.bandClass}  ${hamBand.startFrequencyHz}–${hamBand.endFrequencyHz} Hz"
    )

    selected.onChange { (_, _, _) =>
      // replace everything in the store from current UI state
      availableBandsManager.bands.setAll(checked*)
    }

    val bandName: Band =
      hamBand.bandName