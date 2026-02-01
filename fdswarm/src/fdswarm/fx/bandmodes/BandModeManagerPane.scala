package fdswarm.fx.bandmodes

import com.typesafe.config.Config
import fdswarm.fx.bands.AvailableBandsStore
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.beans.property.BooleanProperty
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.*

import scala.jdk.CollectionConverters.*

/** 3-pane editor:
 * 1) band checkboxes (from AvailableBandsStore.availableBands.bandNames)
 * 2) mode checkboxes (from application.conf fdswarm.modes)
 * 3) matrix pane (BandModeMatrixPane) showing only selected bands/modes
 *
 * Persists:
 * - selected bands/modes + enabled matrix via BandModeStore (bandmodes.json)
 * - selected cell via SelectedBandModeStore (selected-bandmode.json) through matrix pane
 */
@Singleton
final class BandModeManagerPane @Inject() (
                                            availableBandsStore: AvailableBandsStore,
                                            config: Config,
                                            store: BandModeStore,
                                            matrixPane: BandModeMatrixPane
                                          ) extends BorderPane {

  private val allBands: Seq[String] =
    availableBandsStore.availableBands.bandNames.toSeq.sorted

  private val allModes: Seq[String] =
    config.getStringList("fdswarm.modes").asScala.toSeq.sorted

  // local checkbox props (initialized from store state)
  private val bandSelected: Map[String, BooleanProperty] = {
    val cur = store.currentBandMode.bands
    allBands.map(b => b -> BooleanProperty(cur.contains(b))).toMap
  }

  private val modeSelected: Map[String, BooleanProperty] = {
    val cur = store.currentBandMode.modes
    allModes.map(m => m -> BooleanProperty(cur.contains(m))).toMap
  }

  // panes
  private val bandsPane: TitledPane =
    new TitledPane {
      text = "Bands"
      collapsible = false
      content = new VBox {
        spacing = 6
        padding = Insets(8)
        children = allBands.map { band =>
          val cb = new CheckBox(band)
          cb.selected <==> bandSelected(band)
          cb.selected.onChange { (_, _, _) =>
            persistSelections()
          }
          cb
        }
      }
    }

  private val modesPane: TitledPane =
    new TitledPane {
      text = "Modes"
      collapsible = false
      content = new VBox {
        spacing = 6
        padding = Insets(8)
        children = allModes.map { mode =>
          val cb = new CheckBox(mode)
          cb.selected <==> modeSelected(mode)
          cb.selected.onChange { (_, _, _) =>
            persistSelections()
          }
          cb
        }
      }
    }

  private val buttons: HBox =
    new HBox {
      spacing = 8
      padding = Insets(6, 0, 0, 0)
      children = Seq(
        new Button("Defaults (all on)") {
          onAction = _ => {
            allBands.foreach(b => bandSelected(b).value = true)
            allModes.foreach(m => modeSelected(m).value = true)

            val enabled = allModes.map(m => m -> allBands.toSet).toMap
            store.setBands(allBands.toSet)
            store.setModes(allModes.toSet)
            store.setEnabled(enabled)

            refreshMatrixVisibility()
            matrixPane.refreshFromStore()
          }
        },
        new Button("Clear") {
          onAction = _ => {
            allBands.foreach(b => bandSelected(b).value = false)
            allModes.foreach(m => modeSelected(m).value = false)
            store.setBands(Set.empty)
            store.setModes(Set.empty)
            store.setEnabled(Map.empty)
            refreshMatrixVisibility()
            matrixPane.refreshFromStore()
            matrixPane.clearSelection()
          }
        },
        new Button("Clear Cell Selection") {
          onAction = _ => matrixPane.clearSelection()
        }
      )
    }

  padding = Insets(10)
  left = new VBox {
    spacing = 10
    padding = Insets(0, 10, 0, 0)
    children = Seq(bandsPane, modesPane, buttons)
  }
  center = matrixPane

  // initial matrix visibility
  refreshMatrixVisibility()
  matrixPane.refreshFromStore()

  private def selectedBandsNow: Seq[String] =
    allBands.filter(b => bandSelected(b).value)

  private def selectedModesNow: Seq[String] =
    allModes.filter(m => modeSelected(m).value)

  private def refreshMatrixVisibility(): Unit =
    matrixPane.setVisible(selectedModesNow, selectedBandsNow)

  private def persistSelections(): Unit = {
    store.setBands(selectedBandsNow.toSet)
    store.setModes(selectedModesNow.toSet)

    refreshMatrixVisibility()
    matrixPane.refreshFromStore()
  }
}
