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

import fdswarm.fx.bands.{AvailableModesManager, BandCatalog, ModeCatalog}
import fdswarm.fx.components.{AnyComboBox, OptionTextField}
import fdswarm.fx.contest.{ContestDefinition, ContestCatalog, ClassChoice, ContestConfig, ContestManager, ContestType}
import fdswarm.fx.{GridColumns, UserConfig}
import fdswarm.model.BandMode.*
import fdswarm.model.{BandMode, Qso}
import fdswarm.util.JavaTimeCirce.given
import io.circe.syntax.*
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.{HBox, Priority, VBox}
import scalafx.stage.FileChooser

import java.io.PrintWriter

class QsoSearchPane @Inject()(
    contestManager: ContestManager,
    contestCatalog: ContestCatalog,
    modeCatalog: ModeCatalog,
    modesManager: AvailableModesManager,
    bandCatalog:BandCatalog,
    userConfig: UserConfig
):
  val callsignFilter = new OptionTextField {
    promptText = "Callsign"
  }
  val contestConfig: ContestConfig = contestManager.config
  val contestDefinition: ContestDefinition = contestCatalog.getContest(contestConfig.contestType).get

  val items = contestDefinition.classChars.map(contestClassChar => (contestClassChar.ch, contestClassChar.description))

  callsignFilter.text.onChange { (_, _, newValue) =>
    val up = Option(newValue).getOrElse("").toUpperCase
    if up != newValue then callsignFilter.text = up
  }

  val bandFilter = new AnyComboBox[Band](bandCatalog.hamBands) 
  val modeFilter = new AnyComboBox[Mode](modeCatalog.choices)
  private val classChoices: Seq[ClassChoice] = contestDefinition.classChars
  val classFilter = new AnyComboBox[Char](classChoices)
  val operatorFilter = new TextField {
    promptText = "Operator"
  }
  operatorFilter.text.onChange { (_, _, newValue) =>
    val up = Option(newValue).getOrElse("").toUpperCase
    if up != newValue then operatorFilter.text = up
  }


  /**
   * is the pane expanded?
   */
  val expandedProperty = scalafx.beans.property.BooleanProperty(true)

  def filter(qso: Qso): Boolean =
//    if !expandedProperty.value then return true
    val callSign: Option[String] = callsignFilter.value
    val band: Option[Band] = bandFilter.value.value
    val mode: Option[Mode] = modeFilter.value.value
    val transmitters:Int = 0 //todo: get a field
    throw new NotImplementedError("") //todo
  private val exportButton = new Button("Export..."):
    onAction = _ => showExportMenu()

  def showExportMenu(): Unit =
    val menu = new ContextMenu()
    val jsonItem = new MenuItem("Export as JSON")
    jsonItem.onAction = _ => exportData(true)
    val csvItem = new MenuItem("Export as CSV")
    csvItem.onAction = _ => exportData(false)
    menu.items ++= Seq(jsonItem, csvItem)
    val bounds = exportButton.localToScreen(exportButton.boundsInLocal.value)
    if bounds != null then
      menu.show(exportButton, bounds.getMinX, bounds.getMaxY)
    else
      // Fallback if not visible or similar
      menu.show(node.getScene.getWindow)

  def focusSearch(): Unit =
    callsignFilter.requestFocus()

  private def exportData(asJson: Boolean): Unit =
    val fileChooser = new FileChooser()
    fileChooser.title = if asJson then "Export QSOs as JSON" else "Export QSOs as CSV"
    fileChooser.extensionFilters.add(
      if asJson then new FileChooser.ExtensionFilter("JSON Files", "*.json")
      else new FileChooser.ExtensionFilter("CSV Files", "*.csv")
    )
    val file = fileChooser.showSaveDialog(node.getScene.getWindow)
    if file != null then
      val filteredQsos = filteredQsosSupplier()
      val writer = new PrintWriter(file)
      try
        if asJson then
          writer.print(filteredQsos.asJson.spaces2)
        else
          writer.println(Qso.csvHeader)
          filteredQsos.foreach { qso =>
            val flat = qso.flatten
            val row = Seq("Time", "Their Call", "Class", "Section", "Band", "Mode", "Operator", "Rig", "Antenna", "Node", "Version")
              .map(k => flat.getOrElse(k, ""))
              .map(v => if v.contains(",") then s"\"$v\"" else v)
              .mkString(",")
            writer.println(row)
          }
      finally
        writer.close()

  var filteredQsosSupplier: () => Seq[Qso] = () => Seq.empty

  val node: Node = 
    val titledPane = new TitledPane {
      text = "Search"
      content = new VBox {
        spacing = 10
        children = Seq(
          new HBox {
            spacing = 10
            alignment = scalafx.geometry.Pos.CenterLeft
            children = Seq(
              new VBox { children = Seq(new Label("Callsign"), callsignFilter) },
              new VBox { children = Seq(new Label("Band"), bandFilter) },
              new VBox { children = Seq(new Label("Mode"), modeFilter) },
              new VBox { children = Seq(new Label("Class"), classFilter) },
              new VBox { children = Seq(new Label("Operator"), operatorFilter) },
              new VBox { 
                alignment = scalafx.geometry.Pos.BottomLeft
                children = Seq(exportButton) 
              }
            )
          }
        )
      }
      expanded <==> expandedProperty
      collapsible = true
    }
    titledPane
