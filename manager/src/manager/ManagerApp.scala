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

import com.google.inject.{Guice, Injector}
import com.typesafe.scalalogging.LazyLogging
import fdswarm.DebugConfig
import fdswarm.model.{BandMode, Callsign}
import net.codingwell.scalaguice.InjectorExtensions.*
import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.beans.property.{BooleanProperty, ObjectProperty, StringProperty}
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.control.TableColumn.*
import scalafx.scene.control.cell.{CheckBoxTableCell, TextFieldTableCell}
import scalafx.scene.layout.{BorderPane, HBox}
import fdswarm.fx.bandmodes.{BandModeMatrixPane, SelectedBandModeStore}
import fdswarm.fx.bands.{AvailableBandsManager, AvailableModesManager, BandCatalog, BandClass, ModeCatalog}
import scalafx.stage.Window

object ManagerApp extends JFXApp3 with LazyLogging {

  private lazy val injector: Injector =
    Guice.createInjector(new ManagerModule())

  override def start(): Unit = {
    // Force all HF, VHF, and UHF bands and all modes to be available for selection in manager
    val bandCatalog = injector.instance[BandCatalog]
    val modeCatalog = injector.instance[ModeCatalog]
    val bandsManager = injector.instance[AvailableBandsManager]
    val modesManager = injector.instance[AvailableModesManager]

    val allRequiredBands = bandCatalog.hamBands
      .filter(b => b.bandClass == BandClass.HF || b.bandClass == BandClass.VHF || b.bandClass == BandClass.UHF)
      .map(_.bandName)
    bandsManager.bands.setAll(allRequiredBands*)
    modesManager.modes.setAll(modeCatalog.modes*)

    val nodeConfigManager = injector.instance[NodeConfigManager]

    stage = new JFXApp3.PrimaryStage {
      title = "Debug Configuration Manager"
//      onCloseRequest = _ => {
//        injector.instance[Runner].stopAll()
//      }
      scene = new Scene {
        root = new BorderPane {
          center = new TableView[DebugConfig](nodeConfigManager.observableBuffer) {
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
                cellFactory = (col: TableColumn[DebugConfig, String]) => {
                  new TextFieldTableCell[DebugConfig, String](new scalafx.util.StringConverter[String] {
                    override def toString(t: String): String = t
                    override def fromString(s: String): String = s
                  }) {
                    graphic.onChange { (_, _, newValue) =>
                      if (newValue != null && newValue.isInstanceOf[javafx.scene.control.TextField]) {
                        val textField = newValue.asInstanceOf[javafx.scene.control.TextField]
                        textField.setTextFormatter(new javafx.scene.control.TextFormatter[String](new java.util.function.UnaryOperator[javafx.scene.control.TextFormatter.Change] {
                          override def apply(change: javafx.scene.control.TextFormatter.Change): javafx.scene.control.TextFormatter.Change = {
                            if (change.isContentChange) {
                              change.setText(change.getText.toUpperCase)
                            }
                            change
                          }
                        }))
                      }
                    }
                  }
                }
                onEditCommit = (evt: TableColumn.CellEditEvent[DebugConfig, String]) => {
                  val index = evt.tablePosition.row
                  val old = nodeConfigManager.observableBuffer(index)
                  nodeConfigManager.observableBuffer(index) = old.copy(operator = Callsign(evt.newValue))
                }
                editable = true
              },
              new TableColumn[DebugConfig, String] {
                text = "BandMode"
                cellValueFactory = { cd => new StringProperty(cd.value, "bandMode", cd.value.bandMode.toString) }
                cellFactory = (col: TableColumn[DebugConfig, String]) => {
                  new TableCell[DebugConfig, String] {
                    item.onChange { (_, _, newValue) =>
                      text = newValue
                    }
                    onMouseClicked = _ => {
                      val index = tableRow.value.indexProperty().get()
                      if (index >= 0 && index < nodeConfigManager.observableBuffer.size) {
                        val oldConfig = nodeConfigManager.observableBuffer(index)
                        
                        // We need a BandModeMatrixPane. Since it's a singleton in this injector,
                        // we can just get it. But we must set its initial selection.
                        val matrixPane = injector.instance[BandModeMatrixPane]
                        val selectedStore = injector.instance[SelectedBandModeStore]
                        selectedStore.save(oldConfig.bandMode)
                        
                        val dialog = new Dialog[BandMode] {
                          initOwner(stage)
                          title = "Select BandMode"
                          headerText = s"Select BandMode for ${oldConfig.id}"
                        }
                        
                        matrixPane.showConfigButton.value = false
                        dialog.dialogPane().content = matrixPane.node
                        dialog.dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)
                        
                        dialog.resultConverter = {
                          case ButtonType.OK => selectedStore.selected.value
                          case _ => null
                        }
                        
                        val result = dialog.showAndWait()
                        result match {
                          case Some(bm: BandMode) =>
                            nodeConfigManager.observableBuffer(index) = oldConfig.copy(bandMode = bm)
                          case _ => // Cancelled
                        }
                      }
                    }
                  }
                }
                editable = false // Use modal instead of text field
              },
              new TableColumn[DebugConfig, java.lang.Boolean] {
                text = "Startup Config"
                cellValueFactory = { cd =>
                  val prop = BooleanProperty(cd.value.showStartupConfig)
                  prop.onChange { (_, _, newValue) =>
                    val index = nodeConfigManager.observableBuffer.indexOf(cd.value)
                    if (index >= 0) {
                      val old = nodeConfigManager.observableBuffer(index)
                      nodeConfigManager.observableBuffer(index) = old.copy(showStartupConfig = newValue)
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
                    val index = nodeConfigManager.observableBuffer.indexOf(cd.value)
                    if (index >= 0) {
                      val old = nodeConfigManager.observableBuffer(index)
                      nodeConfigManager.observableBuffer(index) = old.copy(clearQsos = newValue)
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
                          onAction = _ => nodeConfigManager.observableBuffer -= debugConfig
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
                  nodeConfigManager.add(DebugConfig(Callsign("N0CALL"), BandMode("20M PH")))
                }
              },
              new Button("Save") {
                onAction = _ => {
                  nodeConfigManager.persist()
                  logger.info("Changes saved to nodes.json")
                }
              },
              new Button("Start All") {
                onAction = _ => {
                  val runner = injector.instance[Runner]
                  nodeConfigManager.observableBuffer.foreach(runner.start)
                }
              },
              new Button("Stop All") {
                onAction = _ => {
//                  injector.instance[Runner].stopAll()
                }
              }
            )
          }
        }
      }
    }
  }
}
