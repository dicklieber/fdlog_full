package fdswarm.fx.bandmodes

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.bands.{AvailableBandsManager, AvailableModesManager}
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

  // (mode, band) -> button
  private val buttonsByKey = scala.collection.mutable.Map.empty[(String, String), ToggleButton]
  // BandMode -> (mode, band)
  private val keyByBandMode = scala.collection.mutable.Map.empty[BandMode, (String, String)]

  private val grid = new GridPane:
    hgap = 3
    vgap = 3
//    alignment = Pos.TopLeft

  for
    (mode,row) <- availableModesManager.modes.zipWithIndex
    _ = grid.addRow(row, new Label(mode))
    (band,col)<- availableBandsStore.bands.zipWithIndex
  do
    logger.info(s"Adding band $band to mode $mode")
    grid.add(ModeBandButton(band,mode),col+1,row)


  val node:Node =
    new TitledPane {
      content = grid
      collapsible = false
      text = "Band & Mode"
    }



  case class ModeBandButton(band:Band, mode:Mode) extends ToggleButton():
    text = band
    graphic = null
    graphicTextGap = 0
    toggleGroup = tg
    styleClass += "custom-radio" // Apply custom CSS class

    val bandMode = BandMode(band, mode)