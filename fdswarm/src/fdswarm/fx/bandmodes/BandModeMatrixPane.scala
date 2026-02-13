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

package fdswarm.fx.bandmodes

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.GridUtils
import fdswarm.fx.bands.{AvailableBandsManager, AvailableModesManager, BandModeBuilder, HamBand}
import fdswarm.model.BandMode
import fdswarm.model.BandMode.{Band, Mode}
import jakarta.inject.{Inject, Singleton}
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.{ColumnConstraints, GridPane, Priority}

/**
 * Mode × Band matrix.
 *
 * - One row per mode
 * - Each row contains all known bands (button label = band name)
 * - Exactly one cell selected (persisted via SelectedBandModeStore)
 * - Selection rendered as an outlined rounded rectangle
 * - Clicking any band in a row selects that (band, mode)
 * - Cells are disabled when the BandModeStore's enabled-matrix says that pair is illegal
 * - If there is no persisted BandMode, the top-left cell is selected (or the first enabled one if available)
 */
final class BandModeMatrixPane @Inject()(availableBandsStore: AvailableBandsManager,
                                         availableModesManager: AvailableModesManager,
                                         selectedStore: SelectedBandModeStore,
                                         bandModeBuilder: BandModeBuilder) extends  LazyLogging:

  private val tg = new ToggleGroup()

  private val container = new scalafx.scene.layout.StackPane()
  buildGrid()

  availableBandsStore.bands.onChange {
    buildGrid()
  }
  availableModesManager.modes.onChange {
    buildGrid()
  }

  def buildGrid():Unit=
    val grid = new GridPane():
      hgap = 2
      vgap = 2

    val nBands = availableBandsStore.bands.size
    val firstColCc = new ColumnConstraints() { hgrow = Priority.Never }
    val bandColCc = new ColumnConstraints() {
      hgrow = Priority.Never
    }
    grid.columnConstraints = Seq(firstColCc) ++ (1 to nBands).map(_ => bandColCc)

    for
      (mode,row) <- availableModesManager.modes.zipWithIndex
    do
      grid.add(new Label(mode), 0, row)
      for (band, col) <- availableBandsStore.bands.zipWithIndex do
        logger.trace(s"Adding band $band and mode $mode cell to grid.")
        grid.add(ModeBandButton(band,mode, selectedStore.selected.value),col+1,row)
    container.children = Seq(GridUtils.fieldSet("Band & Mode", grid))

  val node:Node =
    container


  case class ModeBandButton(band:Band, mode:Mode, selectedHamBand:BandMode) extends ToggleButton():
    val bandMode: BandMode = bandModeBuilder(band, mode)
    text = band
    padding = Insets(2, 4, 2, 4)
    minWidth = 0
    graphic = null
    graphicTextGap = 0
    toggleGroup = tg
    styleClass += "custom-radio" // Apply custom CSS class
    selected.onChange { (_, _, isSelected) =>
      if isSelected then
        selectedStore.save(bandMode)
    }
    if selectedHamBand == bandMode then
      selected.value = true
