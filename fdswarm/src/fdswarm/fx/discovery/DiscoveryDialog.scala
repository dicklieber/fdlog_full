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

package fdswarm.fx.discovery

import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.utils.GridTableBuilder
import jakarta.inject.Inject
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.GridPane

import scala.collection.mutable.ArrayBuffer

class DiscoveryDialog @Inject() (contestDiscovery: ContestDiscovery)
  extends Dialog with LazyLogging:

  //  def show(window: Window): Unit =
  val discovered = ArrayBuffer[NodeContestStation]()
  contestDiscovery.discoverContest((ncs: NodeContestStation) =>
    logger.info(s"Discovery UI added: $ncs")
    discovered.append(ncs)
    Platform.runLater {
      populateGrid()
    }
  )
  logger.trace("Done waiting for responses from other nodes.")
  title = "Discovered Contest Stations"
  resizable = true

  var gridPane: GridPane = new GridPane {
    hgap = 10
    vgap = 10
    padding = Insets(20, 100, 10, 10)
  }

  def populateGrid(): Unit =
    val gridTableBuilder: GridTableBuilder = GridTableBuilder()
      .addHeaders("Host IP", "Host Name", "Port", "Contest", "Exchange", "Our Call", "Operator")
    discovered.foreach { ncs =>
      gridTableBuilder.addRow(
        ncs.nodeIdentity.hostIp,
        ncs.nodeIdentity.hostName,
        ncs.nodeIdentity.port.toString,
        ncs.discoveryWire.contestConfig.contestType.toString,
        ncs.exchange,
        ncs.discoveryWire.contestConfig.ourCallsign.toString,
        ncs.discoveryWire.stationConfig.operator.toString
      )
    }
    gridPane = gridTableBuilder.grid

    val scrollPane: ScrollPane = new ScrollPane {
      content = gridPane
      prefWidth = 1000.0
      prefHeight = 500.0
    }

    dialogPane().content = scrollPane
    dialogPane().buttonTypes = Seq(ButtonType.OK)

//      dialog.showAndWait()
