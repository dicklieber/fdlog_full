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
import fdswarm.fx.UserConfig
import fdswarm.fx.bands.AvailableModesManager
import fdswarm.fx.bands.BandCatalog
import fdswarm.fx.bands.ModeCatalog
import fdswarm.fx.components.{AnyComboBox, CountComboBox, OptionTextField}
import fdswarm.fx.contest.*
import fdswarm.fx.utils.BootstrapIcons
import fdswarm.fx.utils.IconButton
import fdswarm.fx.utils.MultiChangeWatcher
import fdswarm.model.BandMode.*
import fdswarm.model.Qso
import fdswarm.store.QsoStore
import fdswarm.util.{DurationFormat, MetricsHelpers}
import io.circe.syntax.*
import io.micrometer.core.instrument.MeterRegistry
import jakarta.inject.Inject
import scalafx.Includes.*
import scalafx.beans.property.BooleanProperty
import scalafx.scene.Node
import scalafx.scene.control.*
import scalafx.scene.layout.{HBox, VBox}
import scalafx.scene.paint.Color
import scalafx.stage.FileChooser

import java.io.PrintWriter
import java.time.Duration

class QsoSearchPane @Inject()(
                               contestManager: ContestConfigManager,
                               contestCatalog: ContestCatalog,
                               modeCatalog: ModeCatalog,
                               modesManager: AvailableModesManager,
                               bandCatalog:BandCatalog,
                               userConfig: UserConfig,
                               meterRegistry: MeterRegistry,
                               qsoStore: QsoStore,
                               qsoTablePane: QsoTablePane
) extends LazyLogging:
  val callsignFilter = new OptionTextField {
    promptText = "Callsign"
  }
  val contestConfig: ContestConfig = contestManager.contestConfig
  val contestDefinition: ContestDefinition = contestCatalog.getContest(contestConfig.contestType).get
  val bandFilter = new AnyComboBox[Band](bandCatalog.hamBands)
  val modeFilter = new AnyComboBox[Mode](modeCatalog.choices)
  val classChoices: Seq[ClassChoice] = contestDefinition.classChars
  val classFilter = new AnyComboBox[Char](classChoices)
  val operatorFilter = new OptionTextField() {
    promptText = "Operator"
  }
  val transmittersFilter = new CountComboBox()
  /**
   * is the pane expanded?
   */
  val expandedProperty = scalafx.beans.property.BooleanProperty(false)

  val anyChange: BooleanProperty = MultiChangeWatcher(callsignFilter.optionValueProperty,
    bandFilter.value,
    modeFilter.value,
    transmittersFilter.value,
    classFilter.value,
    operatorFilter.optionValueProperty,
    expandedProperty)

  anyChange.onChange((_, _, newVal) =>
    logger.debug("anyChange: {}", newVal)

    val startTime = System.nanoTime()
    val searchResult = qsoStore.qsoCollection.filter (qso =>
      filter(qso)
    )
    val durationNanos = System.nanoTime() - startTime
    val sDuration: String = DurationFormat(Duration.ofNanos(durationNanos))
    logger.debug("filteredQsos: {} of {} in {}", searchResult.size, qsoStore.qsoCollection.size, sDuration)
    MetricsHelpers.recordTimerNanos(meterRegistry, "fdswarm_qso_filter_duration", durationNanos)


    val isSearching = expandedProperty.value && (callsignFilter.value.isDefined ||
      bandFilter.value.value.isDefined ||
      modeFilter.value.value.isDefined ||
      transmittersFilter.value.value.isDefined ||
      classFilter.value.value.isDefined ||
      operatorFilter.optionValueProperty.value.isDefined)

    if isSearching then
      qsoTablePane.showSearchResults(searchResult)
    else
      qsoTablePane.restoreQsoCollection()
  )

  def filter(qso: Qso): Boolean = {
    val callSignFilterVal = callsignFilter.value.getOrElse("").toUpperCase
    val bandFilterVal = bandFilter.value.value
    val modeFilterVal = modeFilter.value.value
    val classFilterVal = classFilter.value.value
    val operatorFilterVal = Option(operatorFilter.text.value).getOrElse("").toUpperCase

    val matchesCallsign = callSignFilterVal.isEmpty || qso.callsign.value.toUpperCase.contains(callSignFilterVal)
    val matchesBand = bandFilterVal.isEmpty || qso.bandMode.band.toUpperCase == bandFilterVal.get.toString.toUpperCase
    val matchesMode = modeFilterVal.isEmpty || qso.bandMode.mode.toUpperCase == modeFilterVal.get.toString.toUpperCase
    val matchesClass = classFilterVal.isEmpty || qso.exchange.fdClass.classLetter.toString.toUpperCase == classFilterVal.get.toString.toUpperCase
    val matchesTransmitters = transmittersFilter.check(qso.exchange.fdClass.transmitters)
    val matchesOperator = operatorFilterVal.isEmpty || qso.qsoMetadata.station.operator.value.toUpperCase.contains(operatorFilterVal)

    matchesCallsign && matchesBand && matchesMode && matchesClass && matchesTransmitters && matchesOperator
  }
  private val exportButton = new Button("Export..."):
    onAction = _ => showExportMenu()

  private val resetButton = {
    val btn = IconButton("x-octagon", size = 20, tooltipText = "Reset Filters", color = Color.Red)
    btn.onAction = _ =>
      callsignFilter.text = ""
      bandFilter.value = None
      modeFilter.value = None
      classFilter.value = None
      transmittersFilter.value = None
      operatorFilter.text = ""
    btn
  }

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
              new VBox { children = Seq(new Label("Transmitters"), transmittersFilter) },
              new VBox { children = Seq(new Label("Operator"), operatorFilter) },
              new VBox { 
                alignment = scalafx.geometry.Pos.BottomLeft
                spacing = 10
                children = Seq(
                  new HBox {
                    spacing = 10
                    children = Seq(resetButton, exportButton)
                  }
                ) 
              }
            )
          }
        )
      }
      expanded <==> expandedProperty
      collapsible = true
    }
    titledPane
