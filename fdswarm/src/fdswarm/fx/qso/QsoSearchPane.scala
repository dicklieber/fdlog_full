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

import fdswarm.fx.bands.{AvailableModesManager, ModeCatalog}
import fdswarm.fx.{GridColumns, UserConfig}
import fdswarm.fx.contest.{ContestCatalog, ContestClassChar, ContestManager}
import fdswarm.model.{BandMode, Qso}
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.{HBox, Priority, VBox}
import scalafx.stage.FileChooser

import java.io.PrintWriter
import io.circe.syntax.*
import fdswarm.util.JavaTimeCirce.given

@Singleton
class QsoSearchPane @Inject()(
    contestManager: ContestManager,
    contestCatalog: ContestCatalog,
    modeCatalog: ModeCatalog,
    modesManager: AvailableModesManager,
    userConfig: UserConfig
):
  private val ANY = "Any"
  private val ANY_CLASS = ContestClassChar(ANY, "")

  val callsignFilter = new TextField {
    promptText = "Callsign"
  }
  callsignFilter.text.onChange { (_, _, newValue) =>
    val up = Option(newValue).getOrElse("").toUpperCase
    if up != newValue then callsignFilter.text = up
  }
  val bandFilter = new ComboBox[String](ANY +: BandMode.bandFreqMap.keys.toSeq.sorted) { value = ANY }
  val modeFilter = new ComboBox[String](ANY +: modeCatalog.modes) { value = ANY }
  val classFilter = new ComboBox[ContestClassChar]() {
    converter = new scalafx.util.StringConverter[ContestClassChar] {
      def toString(ccc: ContestClassChar): String =
        if ccc == null || ccc.ch == ANY then ANY
        else if ccc.description.nonEmpty then s"${ccc.ch} - ${ccc.description}"
        else ccc.ch
      def fromString(s: String): ContestClassChar = ??? // Not needed for non-editable ComboBox
    }
  }
  val operatorFilter = new TextField {
    promptText = "Operator"
  }
  operatorFilter.text.onChange { (_, _, newValue) =>
    val up = Option(newValue).getOrElse("").toUpperCase
    if up != newValue then operatorFilter.text = up
  }

  // Update classFilter when contest changes
  contestManager.configProperty.onChange { (_, _, config) =>
    val classes = contestCatalog.getContest(config.contest).map(_.classChars).getOrElse(Seq.empty)
    val currentCh = Option(classFilter.value.value).map(_.ch).getOrElse(ANY)
    classFilter.items = ObservableBuffer.from(ANY_CLASS +: classes)
    val nextValue = classes.find(_.ch == currentCh).getOrElse(ANY_CLASS)
    classFilter.value = nextValue
  }
  // Trigger initial population
  private val initialClasses = contestCatalog.getContest(contestManager.config.contest).map(_.classChars).getOrElse(Seq.empty)
  classFilter.items = ObservableBuffer.from(ANY_CLASS +: initialClasses)
  classFilter.value = ANY_CLASS

  val expandedProperty = scalafx.beans.property.BooleanProperty(true)

  def filter(qso: Qso): Boolean =
    if !expandedProperty.value then return true
    val cs = callsignFilter.text.value.toUpperCase
    val band = bandFilter.value.value
    val mode = modeFilter.value.value
    val transmitters = 1 //todo: get a field
    val classLetter: Char = classFilter.value.value.ch.head //todo use new combo box
    val op = operatorFilter.text.value.toUpperCase

    val matches = (cs.isEmpty || qso.callsign.value.contains(cs)) &&
    (band == ANY || qso.bandMode.band == band) &&
    (mode == ANY || qso.bandMode.mode == mode) &&
    (transmitters == -1 || qso.exchange.fdClass.transmitters == transmitters) &&
    (classLetter == '-' || qso.exchange.fdClass.classLetter == classLetter) &&
    (op.isEmpty || qso.qsoMetadata.station.operator.value.toUpperCase.contains(op))
    matches

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
