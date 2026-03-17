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

package manager

import fdswarm.DebugConfig
import fdswarm.model.{BandMode, Callsign}
import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.beans.property.{BooleanProperty, ObjectProperty, StringProperty}
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.control.TableColumn.*
import scalafx.scene.control.cell.{CheckBoxTableCell, TextFieldTableCell}
import scalafx.scene.layout.{BorderPane, HBox}

object ManagerApp extends JFXApp3 {
  override def start(): Unit = {
    stage = new JFXApp3.PrimaryStage {
      title = "Debug Configuration Manager"
      scene = new Scene {
        root = new BorderPane {
          center = new TableView[DebugConfig](NodeConfigManager.observableBuffer) {
            editable = true
            columns ++= Seq(
              new TableColumn[DebugConfig, String] {
                text = "Id"
                cellValueFactory = { cd => new StringProperty(cd.value, "id", cd.value.id) }
                editable = false
              },
              new TableColumn[DebugConfig, String] {
                text = "Operator"
                cellValueFactory = { cd => new StringProperty(cd.value, "operator", cd.value.operator.value) }
                cellFactory = TextFieldTableCell.forTableColumn[DebugConfig]()
                onEditCommit = (evt: TableColumn.CellEditEvent[DebugConfig, String]) => {
                  val index = evt.tablePosition.row
                  val old = NodeConfigManager.observableBuffer(index)
                  NodeConfigManager.observableBuffer(index) = old.copy(operator = Callsign(evt.newValue))
                }
                editable = true
              },
              new TableColumn[DebugConfig, String] {
                text = "BandMode"
                cellValueFactory = { cd => new StringProperty(cd.value, "bandMode", cd.value.bandMode.toString) }
                cellFactory = TextFieldTableCell.forTableColumn[DebugConfig]()
                onEditCommit = (evt: TableColumn.CellEditEvent[DebugConfig, String]) => {
                  val index = evt.tablePosition.row
                  val old = NodeConfigManager.observableBuffer(index)
                  try {
                    NodeConfigManager.observableBuffer(index) = old.copy(bandMode = BandMode(evt.newValue))
                  } catch {
                    case _: Exception => // Ignore invalid format
                  }
                }
                editable = true
              },
              new TableColumn[DebugConfig, java.lang.Boolean] {
                text = "Startup Config"
                cellValueFactory = { cd =>
                  val prop = BooleanProperty(cd.value.showStartupConfig)
                  prop.onChange { (_, _, newValue) =>
                    val index = NodeConfigManager.observableBuffer.indexOf(cd.value)
                    if (index >= 0) {
                      val old = NodeConfigManager.observableBuffer(index)
                      NodeConfigManager.observableBuffer(index) = old.copy(showStartupConfig = newValue)
                    }
                  }
                  prop.delegate
                }
                cellFactory = CheckBoxTableCell.forTableColumn[DebugConfig](this)
                editable = true
              },
              new TableColumn[DebugConfig, java.lang.Boolean] {
                text = "Clear QSOs"
                cellValueFactory = { cd =>
                  val prop = BooleanProperty(cd.value.clearQsos)
                  prop.onChange { (_, _, newValue) =>
                    val index = NodeConfigManager.observableBuffer.indexOf(cd.value)
                    if (index >= 0) {
                      val old = NodeConfigManager.observableBuffer(index)
                      NodeConfigManager.observableBuffer(index) = old.copy(clearQsos = newValue)
                    }
                  }
                  prop.delegate
                }
                cellFactory = CheckBoxTableCell.forTableColumn[DebugConfig](this)
                editable = true
              },
              new TableColumn[DebugConfig, DebugConfig] {
                text = "Delete"
                cellValueFactory = { cd => ObjectProperty(cd.value) }
                cellFactory = (col: TableColumn[DebugConfig, DebugConfig]) => {
                  new TableCell[DebugConfig, DebugConfig] {
                    item.onChange { (_, _, debugConfig) =>
                      graphic = if (debugConfig != null) {
                        new Button("Delete") {
                          onAction = _ => NodeConfigManager.observableBuffer -= debugConfig
                        }
                      } else null
                    }
                  }
                }
              }
            )
          }
          bottom = new HBox {
            spacing = 10
            children = Seq(
              new Button("Add") {
                onAction = _ => {
                  NodeConfigManager.add(DebugConfig(Callsign("N0CALL"), BandMode("20M PH")))
                }
              },
              new Button("Save") {
                onAction = _ => {
                  NodeConfigManager.persist()
                  println("Changes saved to nodes.json")
                }
              }
            )
          }
        }
      }
    }
  }
}
