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
import fdswarm.fx.utils.GridHeaderCell
import jakarta.inject.Inject
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{ColumnConstraints, GridPane, Priority}
import scalafx.stage.Window

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

      val gridPane: GridPane = new GridPane {
//        id = "discovery-grid"
        hgap = 1
        vgap = 1
        gridLinesVisible = true
        padding = Insets(10)
        stylesheets ++= Seq("/styles/app.css")
        columnConstraints ++= Seq(
          new ColumnConstraints { hgrow = Priority.Always },
          new ColumnConstraints { hgrow = Priority.Always },
          new ColumnConstraints { hgrow = Priority.Always },
          new ColumnConstraints { hgrow = Priority.Always },
          new ColumnConstraints { hgrow = Priority.Always },
          new ColumnConstraints { hgrow = Priority.Always },
          new ColumnConstraints { hgrow = Priority.Always }
        )
      }

      def populateGrid(): Unit = {
        gridPane.children.clear()
        // Headers
        gridPane.add(GridHeaderCell("Host IP"), 0, 0)
        gridPane.add(GridHeaderCell("Host Name"), 1, 0)
        gridPane.add(GridHeaderCell("Port"), 2, 0)
        gridPane.add(GridHeaderCell("Contest"), 3, 0)
        gridPane.add(GridHeaderCell("Exchange"), 4, 0)
        gridPane.add(GridHeaderCell("Our Call"), 5, 0)
        gridPane.add(GridHeaderCell("Operator"), 6, 0)
        // Data rows
        var row = 1
        observableBuffer.foreach { ncs =>
          gridPane.add(new Label(ncs.nodeIdentity.hostIp), 0, row)
          gridPane.add(new Label(ncs.nodeIdentity.hostName), 1, row)
          gridPane.add(new Label(ncs.nodeIdentity.port.toString), 2, row)
          gridPane.add(new Label(ncs.discoveryWire.contestConfig.contestType.toString), 3, row)
          gridPane.add(new Label(ncs.exchange), 4, row)
          gridPane.add(new Label(ncs.discoveryWire.contestConfig.ourCallsign.toString), 5, row)
          gridPane.add(new Label(ncs.discoveryWire.stationConfig.operator.toString), 6, row)
          row += 1
        }
      }

      observableBuffer.onChange {
        populateGrid()
      }

      populateGrid()

      val scrollPane: ScrollPane = new ScrollPane {
        content = gridPane
        prefWidth = 1000.0
        prefHeight = 500.0
      }

      dialogPane().content = scrollPane
      dialogPane().buttonTypes = Seq(ButtonType.OK)
      initOwner(window)
    }
    dialog.showAndWait()
