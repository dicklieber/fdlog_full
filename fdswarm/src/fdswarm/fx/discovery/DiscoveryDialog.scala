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
import jakarta.inject.Inject
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.*
import scalafx.stage.Window
import scalafx.Includes.*
import javafx.beans.property.{SimpleStringProperty, SimpleIntegerProperty}
import fdswarm.util.NodeIdentity
import fdswarm.fx.contest.ContestConfig
import fdswarm.model.StationConfig

class DiscoveryDialog @Inject() (contestDiscovery: ContestDiscovery)
    extends LazyLogging:
  def show(window: Window): Unit =
    val observableBuffer = ObservableBuffer[NodeContestStation]()
    contestDiscovery.discoverContest((ncs: NodeContestStation) =>
      logger.info(s"Discovery UI added: $ncs")
      Platform.runLater {
        observableBuffer += ncs
      }
    )
    logger.trace("Done waiting for responses from other nodes.")
    val dialog: scalafx.scene.control.Dialog[Unit] = new scalafx.scene.control.Dialog[Unit] {
      title = "Discovered Contest Stations"
      resizable = true

      val tableView: scalafx.scene.control.TableView[NodeContestStation] = new scalafx.scene.control.TableView[NodeContestStation] {
        items = observableBuffer
        columnResizePolicy = TableView.UnconstrainedResizePolicy
        stylesheets ++= Seq("data:text/css,table-view .table-cell { -fx-text-overrun: clip; -fx-ellipsis-string: ''; }")
        prefWidth = 1000.0
        prefHeight = 500.0
        columns ++= Seq(
          new TableColumn[NodeContestStation, String] {
            text = "Host IP"
            cellValueFactory = { cellData => new SimpleStringProperty(cellData.value.nodeIdentity.hostIp) }
          },
          new TableColumn[NodeContestStation, String] {
            text = "Host Name"
            cellValueFactory = { cellData => new SimpleStringProperty(cellData.value.nodeIdentity.hostName) }
            prefWidth = 350.0
          },
          new TableColumn[NodeContestStation, String] {
            text = "Port"
            cellValueFactory = { cellData => new SimpleStringProperty(cellData.value.nodeIdentity.port.toString) }
          },
          new TableColumn[NodeContestStation, String] {
            text = "Contest"
            cellValueFactory = { cellData => new SimpleStringProperty(cellData.value.discoveryWire.contestConfig.contestType.toString) }
          },
          new TableColumn[NodeContestStation, String] {
            text = "Exchange"
            cellValueFactory = { cellData => new SimpleStringProperty(cellData.value.exchange) }
          },
          new TableColumn[NodeContestStation, String] {
            text = "Our Call"
            cellValueFactory = { cellData => new SimpleStringProperty(cellData.value.discoveryWire.contestConfig.ourCallsign.toString) }
          },
           new TableColumn[NodeContestStation, String] {
            text = "Operator"
            cellValueFactory = { cellData => new SimpleStringProperty(cellData.value.discoveryWire.stationConfig.operator.toString) }
          }
        )
      }

      dialogPane().content = tableView
      dialogPane().buttonTypes = Seq(ButtonType.OK)
      initOwner(window)
    }
    dialog.showAndWait()
