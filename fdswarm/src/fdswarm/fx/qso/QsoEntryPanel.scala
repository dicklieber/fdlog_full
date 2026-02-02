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

package fdswarm.fx.qso

import com.typesafe.scalalogging.LazyLogging
import fdswarm.StationManager
import fdswarm.fx.UpperCase
import fdswarm.fx.bandmodes.SelectedBandModeStore
import fdswarm.model.*
import fdswarm.store.QsoStore
import jakarta.inject.{Inject, Singleton}
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.GridPane

@Singleton
class QsoEntryPanel @Inject()(
                               qsoStore: QsoStore,
                               selectedBandModeStore: SelectedBandModeStore,
                               stationManager: StationManager
                             ) extends LazyLogging:

  val callSignField = UpperCase(new TextField())
  val contestClassField = UpperCase(new TextField())
  val sectionField = UpperCase(new TextField())

  val node: Node =

    val grid = new GridPane {

      add(new Label("Their Callsign:"), 0, 0)
      add(callSignField, 0, 1)

      add(new Label("Received Class:"), 1, 0)
      add(contestClassField, 1, 1)

      add(new Label("Received Section:"), 2, 0)
      add(sectionField, 2, 1)
    }
    new TitledPane() {
      text = "QSO"
      collapsible = false
      content = grid
    }
  sectionField.onAction = _ =>
    submit()
  private val bandMode = BandMode() // TODO: wire from SelectedBandModeStore

  // ---- controls ----------------------------------------------------------
  private val qsoMetadata = //todo add a QsoMetadataStore
    QsoMetadata(
      station = stationManager.station,
      node = "local",
      contest = Contest.WFD
    )


  private def submit(): Unit =
    logger.debug(
      s"Submitting QSO: call=${callSignField.text.value}, " +
        s"class=${contestClassField.text.value}, " +
        s"section=${sectionField.text.value}"
    )

    val qso = Qso(
      callSignField.text.value,
      contestClassField.text.value,
      sectionField.text.value,
      bandMode,
      qsoMetadata
    )

    qsoStore.add(qso)
    callSignField.text.value = ""