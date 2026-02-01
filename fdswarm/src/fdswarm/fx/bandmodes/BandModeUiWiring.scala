package fdswarm.fx.bandmodes

import javafx.util.Callback
import scalafx.Includes.*
import scalafx.scene.control.ComboBox

/** Utility to wire band/mode ComboBoxes so that:
 *  - They stay in sync with the selected matrix cell (SelectedBandModeStore)
 *  - Illegal choices are disabled based on the BandModeStore enable matrix
 *  - The disabled/enabled state refreshes immediately when the enabled matrix changes
 *
 * IMPORTANT: JavaFX list cells won't necessarily re-run updateItem() just because some
 * external state changed. We therefore re-install the cell factories when either:
 *  - the enabled matrix changes, or
 *  - the other combo selection changes.
 */
object BandModeUiWiring:

  def wireCombos(
                  bandCombo: ComboBox[String],
                  modeCombo: ComboBox[String],
                  store: BandModeStore,
                  selectedStore: SelectedBandModeStore
                ): Unit =

    // ------------------------------------------------------------
    // Cell factories (disable illegal choices)
    // ------------------------------------------------------------
    val bandCellFactory =
      new Callback[javafx.scene.control.ListView[String], javafx.scene.control.ListCell[String]]:
        override def call(lv: javafx.scene.control.ListView[String]) =
          new javafx.scene.control.ListCell[String]():
            override def updateItem(item: String, empty: Boolean): Unit =
              super.updateItem(item, empty)
              if empty || item == null then
                setText(null)
                setDisable(false)
              else
                setText(item)
                val selectedMode = Option(modeCombo.value.value)
                val enabled =
                  selectedMode match
                    case Some(m) => store.isEnabled(m, item)
                    case None    => store.modesForBand(item).nonEmpty
                setDisable(!enabled)

    val modeCellFactory =
      new Callback[javafx.scene.control.ListView[String], javafx.scene.control.ListCell[String]]:
        override def call(lv: javafx.scene.control.ListView[String]) =
          new javafx.scene.control.ListCell[String]():
            override def updateItem(item: String, empty: Boolean): Unit =
              super.updateItem(item, empty)
              if empty || item == null then
                setText(null)
                setDisable(false)
              else
                setText(item)
                val selectedBand = Option(bandCombo.value.value)
                val enabled =
                  selectedBand match
                    case Some(b) => store.isEnabled(item, b)
                    case None    => store.bandsForMode(item).nonEmpty
                setDisable(!enabled)

    def installFactories(): Unit =
      bandCombo.delegate.setCellFactory(bandCellFactory)
      bandCombo.delegate.setButtonCell(bandCellFactory.call(null))
      modeCombo.delegate.setCellFactory(modeCellFactory)
      modeCombo.delegate.setButtonCell(modeCellFactory.call(null))

      // Nudge JavaFX to repaint
      bandCombo.delegate.applyCss()
      bandCombo.delegate.layout()
      modeCombo.delegate.applyCss()
      modeCombo.delegate.layout()

    installFactories()

    // ------------------------------------------------------------
    // When matrix selection changes → update combo selections
    // ------------------------------------------------------------
    selectedStore.selected.onChange { (_, _, nv) =>
      nv.foreach { bm =>
        if bandCombo.value.value != bm.band then bandCombo.value.value = bm.band
        if modeCombo.value.value != bm.mode then modeCombo.value.value = bm.mode
      }
      // repaint disabled state based on the new pair
      installFactories()
    }

    // ------------------------------------------------------------
    // User-driven combo changes → update SelectedBandModeStore
    // ------------------------------------------------------------
    bandCombo.value.onChange { (_, _, newBand) =>
      installFactories() // mode selection constrains which bands are legal
      (Option(newBand), Option(modeCombo.value.value)) match
        case (Some(b), Some(m)) =>
          if store.isEnabled(m, b) then selectedStore.set(Some(BandMode(band = b, mode = m)))
        case _ => ()
    }

    modeCombo.value.onChange { (_, _, newMode) =>
      installFactories() // band selection constrains which modes are legal
      (Option(bandCombo.value.value), Option(newMode)) match
        case (Some(b), Some(m)) =>
          if store.isEnabled(m, b) then selectedStore.set(Some(BandMode(band = b, mode = m)))
        case _ => ()
    }

    // ------------------------------------------------------------
    // Enabled-matrix changes → refresh disabled/enabled state immediately
    // ------------------------------------------------------------
    store.bandModes.onChange { (_, _, _) =>
      installFactories()

      // If the current combo-pair became illegal, push to a legal one (or clear).
      selectedStore.current.foreach { bm =>
        if !store.isEnabled(bm.mode, bm.band) then
          val fallbackBand = store.bandsForMode(bm.mode).headOption
          val fallbackMode = store.modesForBand(bm.band).headOption

          (fallbackBand, fallbackMode) match
            case (Some(b), _) => selectedStore.set(Some(BandMode(band = b, mode = bm.mode)))
            case (_, Some(m)) => selectedStore.set(Some(BandMode(band = bm.band, mode = m)))
            case _            => selectedStore.set(None)
      }
    }