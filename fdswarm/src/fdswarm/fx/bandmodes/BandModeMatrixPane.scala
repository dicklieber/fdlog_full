package fdswarm.fx.bandmodes

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.bands.{AvailableBandsManager, AvailableModesManager}
import fdswarm.model.BandMode.{Band, Mode}
import jakarta.inject.{Inject, Singleton}
import javafx.event.{EventHandler, ActionEvent as JfxActionEvent}
import scalafx.Includes.*
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Node
import scalafx.scene.control.{Button, Label, TitledPane, ToggleButton, ToggleGroup}
import scalafx.scene.layout.{GridPane, Pane, Priority, Region, VBox}

import scala.jdk.CollectionConverters.*

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


  private val group = new ToggleGroup()

  // (mode, band) -> button
  private val buttonsByKey = scala.collection.mutable.Map.empty[(String, String), ToggleButton]
  // BandMode -> (mode, band)
  private val keyByBandMode = scala.collection.mutable.Map.empty[BandMode, (String, String)]

  private val selectedStyle =
    "-fx-border-color: -fx-focus-color;" +
      "-fx-border-width: 2;" +
      "-fx-border-radius: 10;" +
      "-fx-background-radius: 10;" +
      "-fx-padding: 6 10 6 10;"

  private val unselectedStyle =
    "-fx-border-color: transparent;" +
      "-fx-border-width: 2;" +
      "-fx-border-radius: 10;" +
      "-fx-background-radius: 10;" +
      "-fx-padding: 6 10 6 10;"

  private val disabledStyle =
    // keep the same padding so the grid doesn't "jitter" visually
    "-fx-border-color: transparent;" +
      "-fx-border-width: 2;" +
      "-fx-border-radius: 10;" +
      "-fx-background-radius: 10;" +
      "-fx-padding: 6 10 6 10;" +
      "-fx-opacity: 0.45;"

  private val grid = new GridPane:
    hgap = 8
    vgap = 8
    alignment = Pos.TopLeft


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
  /*

  private def allBandsInOrder: Seq[String] =
    availableBandsStore.availableBands.bandNames.toSeq.sorted

  private def allModesInOrder: Seq[String] =
    config.getStringList("fdswarm.modes").asScala.toSeq.sorted

  private def buildGrid(): Unit =
    grid.children.clear()
    buttonsByKey.clear()
    keyByBandMode.clear()

    val bands = allBandsInOrder
    val modes = allModesInOrder

    for (mode, r) <- modes.zipWithIndex do
      val modeLbl = new Label(mode):
        style = "-fx-font-weight: bold;"
        minWidth = Region.USE_PREF_SIZE
      grid.add(modeLbl, 0, r)

      for (band, c0) <- bands.zipWithIndex do
        val c = c0 + 1
        val bm = BandMode(band = band, mode = mode)
        keyByBandMode.update(bm, (mode, band))

        val btn = new ToggleButton(band):
          toggleGroup = group
          focusTraversable = false
          maxWidth = Double.MaxValue
          style = unselectedStyle

        btn.onAction = new EventHandler[JfxActionEvent]:
          override def handle(e: JfxActionEvent): Unit =
            // Don't allow selecting a disabled cell.
            if !btn.disable.value then
              selectedStore.set(Some(bm))

        btn.selected.onChange { (_, _, isSel) =>
          if btn.disable.value then btn.style = disabledStyle
          else btn.style = if isSel then selectedStyle else unselectedStyle
        }

        // Keep style consistent if enabled/disabled changes while selected
        btn.disable.onChange { (_, _, isDis) =>
          if isDis then
            btn.style = disabledStyle
          else
            btn.style = if btn.selected.value then selectedStyle else unselectedStyle
        }

        buttonsByKey.update((mode, band), btn)
        grid.add(btn, c, r)
        GridPane.setHgrow(btn, Priority.Always)

    refreshEnabledFromStore()

  /** Apply enabled/disabled state based on BandModeStore.enabled matrix. */
  def refreshEnabledFromStore(): Unit =
    for ((mode, band), btn) <- buttonsByKey do
      val enabled = bandModeStore.isEnabled(mode, band)
      btn.disable = !enabled

    // If the currently-selected cell is now illegal, move selection to a valid cell.
    selectedStore.current.foreach { bm =>
      if !bandModeStore.isEnabled(bm.mode, bm.band) then
        selectFirstEnabledVisibleCellOrClear()
    }

  /** Select the store's current cell if present. */
  def refreshFromStore(): Unit =
    selectedStore.current match
      case Some(bm) =>
        keyByBandMode.get(bm).foreach { key =>
          buttonsByKey.get(key).foreach { b =>
            if !b.selected.value then b.selected = true
          }
        }
      case None =>
        ()

  /** Clear UI + persisted selection. */
  def clearSelection(): Unit =
    group.selectToggle(null)
    selectedStore.set(None)

  /**
   * Overload (not overriding Node.setVisible(Boolean)):
   * show/hide buttons based on selected modes/bands.
   */
  def setVisible(selectedModes: Seq[String], selectedBands: Seq[String]): Unit =
    val modeSet = selectedModes.toSet
    val bandSet = selectedBands.toSet

    for ((mode, band), btn) <- buttonsByKey do
      val show = modeSet.contains(mode) && bandSet.contains(band)
      btn.visible = show
      btn.managed = show

    // If the selected cell became invisible, choose the first visible+enabled cell.
    selectedStore.current.foreach { bm =>
      val maybeBtn = buttonsByKey.get((bm.mode, bm.band))
      if maybeBtn.forall(b => !b.visible.value || b.disable.value) then
        selectFirstEnabledVisibleCellOrClear()
    }

  private def selectFirstEnabledVisibleCellOrClear(): Unit =
    val maybe =
      // prefer visible + enabled
      buttonsByKey.collectFirst {
        case ((m, b), btn) if btn.visible.value && !btn.disable.value => BandMode(b, m)
      }.orElse {
        // else: visible cell (even if disabled) to satisfy "top-left" UX in empty matrix scenarios
        buttonsByKey.collectFirst {
          case ((m, b), btn) if btn.visible.value => BandMode(b, m)
        }
      }

    maybe match
      case Some(bm) => selectedStore.set(Some(bm))
      case None     => clearSelection()

*/
/*  // ----- reactive wiring -----

  // Rebuild once at startup
  buildGrid()
  ensureDefaultSelectionIfNonePersisted()
  refreshFromStore()

  // When persisted selection changes elsewhere, select it here.
  selectedStore.selected.onChange { (_, _, _) =>
    refreshFromStore()
  }

  // When enabled-matrix changes, update disabled state in real-time.
  bandModeStore.bandModes.onChange { (_, _, _) =>
    refreshEnabledFromStore()
  }*/


case class ModeBandButton(band:Band, mode:Mode) extends Button(band):
  graphic = null
  graphicTextGap = 0
  padding = Insets(3, 7, 3, 7) // optional: tighten overall button padding


  val bandMode = BandMode(band, mode)
//  onAction =