package fdswarm.fx.bandmodes

import scalafx.Includes.*
import scalafx.scene.control.ComboBox
import javafx.util.Callback

/** Utility to wire band/mode ComboBoxes so that:
 *  - They stay in sync with the selected matrix cell (SelectedBandModeStore)
 *  - Illegal choices are disabled based on the BandModeStore enable matrix
 */
object BandModeUiWiring {

  def wireCombos(
                  bandCombo: ComboBox[String],
                  modeCombo: ComboBox[String],
                  store: BandModeStore,
                  selectedStore: SelectedBandModeStore
                ): Unit = {

    // ------------------------------------------------------------
    // When matrix selection changes → update combo selections
    // ------------------------------------------------------------
    selectedStore.selected.onChange { (_, _, nv) =>
      nv.foreach { bm =>
        if (bandCombo.value.value != bm.band)
          bandCombo.value.value = bm.band
        if (modeCombo.value.value != bm.mode)
          modeCombo.value.value = bm.mode
      }
    }

    // ------------------------------------------------------------
    // Band ComboBox cell factory
    // ------------------------------------------------------------
    val bandCellFactory =
      new Callback[javafx.scene.control.ListView[String],
        javafx.scene.control.ListCell[String]] {

        override def call(
                           lv: javafx.scene.control.ListView[String]
                         ): javafx.scene.control.ListCell[String] =
          new javafx.scene.control.ListCell[String]() {

            override def updateItem(item: String, empty: Boolean): Unit = {
              super.updateItem(item, empty)

              if (empty || item == null) {
                setText(null)
                setDisable(false)
              } else {
                setText(item)

                val selectedMode = Option(modeCombo.value.value)
                val enabled =
                  selectedMode match
                    case Some(m) => store.isEnabled(m, item)
                    case None    => store.modesForBand(item).nonEmpty

                setDisable(!enabled)
              }
            }
          }
      }

    bandCombo.delegate.setCellFactory(bandCellFactory)
    bandCombo.delegate.setButtonCell(bandCellFactory.call(null))

    // ------------------------------------------------------------
    // Mode ComboBox cell factory
    // ------------------------------------------------------------
    val modeCellFactory =
      new Callback[javafx.scene.control.ListView[String],
        javafx.scene.control.ListCell[String]] {

        override def call(
                           lv: javafx.scene.control.ListView[String]
                         ): javafx.scene.control.ListCell[String] =
          new javafx.scene.control.ListCell[String]() {

            override def updateItem(item: String, empty: Boolean): Unit = {
              super.updateItem(item, empty)

              if (empty || item == null) {
                setText(null)
                setDisable(false)
              } else {
                setText(item)

                val selectedBand = Option(bandCombo.value.value)
                val enabled =
                  selectedBand match
                    case Some(b) => store.isEnabled(item, b)
                    case None    => store.bandsForMode(item).nonEmpty

                setDisable(!enabled)
              }
            }
          }
      }

    modeCombo.delegate.setCellFactory(modeCellFactory)
    modeCombo.delegate.setButtonCell(modeCellFactory.call(null))

    // ------------------------------------------------------------
    // User-driven combo changes → update SelectedBandModeStore
    // ------------------------------------------------------------
    bandCombo.value.onChange { (_, _, newBand) =>
      (Option(newBand), Option(modeCombo.value.value)) match
        case (Some(b), Some(m)) =>
          selectedStore.set(Some(BandMode(band = b, mode = m)))
        case _ => ()
    }

    modeCombo.value.onChange { (_, _, newMode) =>
      (Option(bandCombo.value.value), Option(newMode)) match
        case (Some(b), Some(m)) =>
          selectedStore.set(Some(BandMode(band = b, mode = m)))
        case _ => ()
    }
  }
}