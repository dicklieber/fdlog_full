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
import fdswarm.fx.bands.{BandCheckBoxPane, ModeCatalog, ModeCheckBoxPane}
import jakarta.inject.{Inject, Singleton}
import javafx.event.{EventHandler, ActionEvent as JfxActionEvent}
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
final class BandsAndModesPane @Inject()(
                                         bandCheckBoxPane: BandCheckBoxPane,
                                         modeCheckBoxPane: ModeCheckBoxPane,
                                         matrixPane: BandModeMatrixPane
                                      ) extends GridPane:

  padding = Insets(10)
  hgap = 10
  vgap = 10

  add(modeCheckBoxPane.node, 0, 1)
  add(bandCheckBoxPane.node, 1, 0)
  add(matrixPane.node, 1,1)

/*
  private val modeSelected: Map[String, BooleanProperty] =
    val cur = store.currentBandMode.modes
    allModes.map(m => m -> BooleanProperty(cur.contains(m))).toMap
  private val buttons: HBox =
    new HBox:
      spacing = 8
      padding = Insets(6, 0, 0, 0)
      children = Seq(
        new Button("Defaults (all on)") {
          onAction = handler {
            allBands.foreach(b => bandSelected(b).value = true)
            allModes.foreach(m => modeSelected(m).value = true)

            val enabled = allModes.map(m => m -> allBands.toSet).toMap
            store.setBands(allBands.toSet)
            store.setModes(allModes.toSet)
            store.setEnabled(enabled)

            refreshMatrixVisibility()
            matrixPane.refreshEnabledFromStore()
            matrixPane.refreshFromStore()
          }
        },
        new Button("Clear") {
          onAction = handler {
            allBands.foreach(b => bandSelected(b).value = false)
            allModes.foreach(m => modeSelected(m).value = false)
            store.setBands(Set.empty)
            store.setModes(Set.empty)
            store.setEnabled(Map.empty)
            refreshMatrixVisibility()
            matrixPane.refreshEnabledFromStore()
            matrixPane.refreshFromStore()
            matrixPane.clearSelection()
          }
        },
        new Button("Clear Cell Selection") {
          onAction = handler {
            matrixPane.clearSelection()
          }
        }
      )

  private def handler(body: => Unit): EventHandler[JfxActionEvent] =
    new EventHandler[JfxActionEvent]:
      override def handle(e: JfxActionEvent): Unit = body

  padding = Insets(10)
  left = new VBox:
    spacing = 10
    padding = Insets(0, 10, 0, 0)
    children = Seq(bandsPane, modesPane, buttons)

  center = matrixPane

  // initial matrix visibility
  refreshMatrixVisibility()
  matrixPane.refreshEnabledFromStore()
  matrixPane.refreshFromStore()

  private def persistSelections(): Unit =
    store.setBands(selectedBandsNow.toSet)
    store.setModes(selectedModesNow.toSet)

    refreshMatrixVisibility()
    // Enabled matrix may have changed elsewhere; always re-apply disabled styling.
    matrixPane.refreshEnabledFromStore()
    matrixPane.refreshFromStore()

  private def refreshMatrixVisibility(): Unit =
    matrixPane.setVisible(selectedModesNow, selectedBandsNow)

  private def selectedBandsNow: Seq[String] =
    allBands.filter(b => bandSelected(b).value)

  private def selectedModesNow: Seq[String] =
    allModes.filter(m => modeSelected(m).value)
*/