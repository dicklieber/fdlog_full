package fdswarm.fx.bandmodes

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.bands.{AvailableBandsManager, AvailableModesManager, HamBand}
import fdswarm.model.BandMode.{Band, Mode}
import jakarta.inject.{Inject, Singleton}
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.GridPane

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
@Singleton
final class BandModeMatrixPane @Inject() (
                                           availableBandsStore: AvailableBandsManager,
                                           availableModesManager: AvailableModesManager,
                                           config: Config,
                                           selectedStore: SelectedBandModeStore
                                         ) extends  LazyLogging:

  private val tg = new ToggleGroup()

  private val pane: TitledPane = new TitledPane {
    collapsible = false
    text = "Band & Mode"
  }
  buildGrid()

  availableBandsStore.bands.onChange {
    buildGrid()
  }
  availableModesManager.modes.onChange {
    buildGrid()
  }

  def buildGrid():Unit=
    val grid = new GridPane(3,3)

    for
      (mode,row) <- availableModesManager.modes.zipWithIndex
      _ = grid.addRow(row, new Label(mode))
      (band,col)<- availableBandsStore.bands.zipWithIndex
    do
      logger.trace(s"Adding band $band to mode $mode")
      grid.add(ModeBandButton(band,mode, selectedStore.selected),col+1,row)
    pane.content = grid

  val node:Node =
    pane


  case class ModeBandButton(band:Band, mode:Mode, selectedHamBand:BandMode) extends ToggleButton():
    val bandMode: BandMode = BandMode(band, mode)
    text = band
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
