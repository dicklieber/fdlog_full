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

package fdswarm.metric

import scalafx.Includes.*
import scalafx.animation.{KeyFrame, Timeline}
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.chart.{LineChart, NumberAxis, XYChart}
import scalafx.scene.control.*
import scalafx.scene.layout.{HBox, VBox}
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.stage.Window
import scalafx.util.Duration as FxDuration
import javafx.scene.chart.XYChart as JfxXYChart

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import scala.collection.mutable

final class StatsDialog(
    ownerWindow: Window,
    snapshotRows: () => Seq[MetricStatRow]
):
  private val timestampFormatter = DateTimeFormatter
    .ofPattern(
      "yyyy-MM-dd HH:mm:ss"
    )
    .withZone(
      ZoneId.systemDefault()
    )

  private val allRows = ObservableBuffer.empty[MetricStatRow]
  private val filteredRows = ObservableBuffer.empty[MetricStatRow]
  private val maxPlotSamples = 180
  private val plotHistory = mutable.Map.empty[String, Vector[MetricStatSample]]
  private val plotColors = IndexedSeq(
    "#1f77b4",
    "#d62728",
    "#2ca02c",
    "#9467bd",
    "#ff7f0e",
    "#17becf",
    "#8c564b",
    "#7f7f7f"
  )
  private val lastUpdated = new Label("Last updated: -")
  private val nodeFilterField = new TextField:
    promptText = "Filter node names..."
  private val allMetricNamesOption = "-all-"
  private val metricNameFilter = new ComboBox[String](
    ObservableBuffer(
      allMetricNamesOption
    )
  ):
    value = allMetricNamesOption
  private val showMetricPlotsButton = new Button(
    "Plots..."
  ):
    disable = true
    onAction = _ => showSelectedMetricPlots()
  private val showMetricGraphButton = new Button(
    "Graph..."
  ):
    disable = true
    onAction = _ => showMetricGraphDialog()
  private val allTypesOption = "-all-"
  private val typeFilter = new ComboBox[String](
    ObservableBuffer(
      allTypesOption
    )
  ):
    value = allTypesOption

  private val table = new TableView[MetricStatRow](
    filteredRows
  ):
    columns ++= Seq(
      new TableColumn[MetricStatRow, String]:
        text = "Node"
        cellValueFactory = _.value.nodeProp
        prefWidth = 260
      ,
      new TableColumn[MetricStatRow, String]:
        text = "Name"
        cellValueFactory = _.value.nameProp
        prefWidth = 360
      ,
      new TableColumn[MetricStatRow, String]:
        text = "Type"
        cellValueFactory = _.value.typeProp
        prefWidth = 120
      ,
      new TableColumn[MetricStatRow, String]:
        text = "Value"
        cellValueFactory = _.value.valueProp
        prefWidth = 300
    )
    columnResizePolicy = TableView.ConstrainedResizePolicy

  private val refreshTimeline = new Timeline:
    cycleCount = Timeline.Indefinite
    keyFrames = Seq(
      KeyFrame(
        FxDuration(
          5000
        ),
        onFinished = _ => refreshRows()
      )
    )

  private val dialog = new Dialog[Unit]:
    title = "Swarm Stats"
    headerText = "Latest metrics received from status messages"
    initOwner(
      ownerWindow
    )

  nodeFilterField.text.onChange {
    (_, _, _) =>
      applyFilter()
  }
  metricNameFilter.value.onChange {
    (_, _, _) =>
      refreshPlotButton()
      applyFilter()
  }
  typeFilter.value.onChange {
    (_, _, _) =>
      applyFilter()
  }

  dialog.dialogPane().buttonTypes = Seq(
    ButtonType.Close
  )
  dialog.dialogPane().content = new VBox:
    spacing = 8
    padding = Insets(
      10
    )
    prefWidth = 1040
    prefHeight = 600
    children = Seq(
      new HBox:
        spacing = 8
        children = Seq(
          nodeFilterField,
          metricNameFilter,
          showMetricPlotsButton,
          showMetricGraphButton,
          typeFilter
        )
      ,
      lastUpdated,
      table
    )

  dialog.onHidden = _ => refreshTimeline.stop()

  def showAndWait(): Unit =
    refreshRows()
    refreshTimeline.play()
    dialog.showAndWait()

  private def applyFilter(): Unit =
    val nodeFilter = Option(
      nodeFilterField.text.value
    ).map(_.trim.toLowerCase).getOrElse("")
    val selectedMetricName = Option(
      metricNameFilter.value.value
    ).map(_.trim.toLowerCase).getOrElse("")
    val selectedType = Option(
      typeFilter.value.value
    ).getOrElse(
      allTypesOption
    )
    filteredRows.setAll(
      allRows.toSeq.filter(row =>
        row.nodeProp.value.toLowerCase.contains(
          nodeFilter
        ) && (
          selectedMetricName == allMetricNamesOption || row.nameProp.value.toLowerCase == selectedMetricName
        ) && (selectedType == allTypesOption || row.typeProp.value == selectedType)
      )*
    )
    refreshPlotButton()

  private def refreshPlotButton(): Unit =
    val noMetricSelected = Option(
      metricNameFilter.value.value
    ).forall(_ == allMetricNamesOption)
    showMetricPlotsButton.disable = noMetricSelected
    showMetricGraphButton.disable = noMetricSelected

  private def refreshMetricNameOptions(): Unit =
    val selectedMetricName = Option(
      metricNameFilter.value.value
    ).getOrElse(
      allMetricNamesOption
    )
    val metricNames = allRows.toSeq
      .map(
        _.nameProp.value
      )
      .distinct
      .sorted
    metricNameFilter.items = ObservableBuffer(
      allMetricNamesOption +: metricNames *
    )
    metricNameFilter.value =
      if metricNames.contains(
          selectedMetricName
        )
      then selectedMetricName
      else allMetricNamesOption

  private def refreshTypeOptions(): Unit =
    val selectedType = Option(
      typeFilter.value.value
    ).getOrElse(
      allTypesOption
    )
    val types = allRows.toSeq
      .map(
        _.typeProp.value
      )
      .distinct
      .sorted
    typeFilter.items = ObservableBuffer(
      allTypesOption +: types *
    )
    typeFilter.value =
      if types.contains(
          selectedType
        )
      then selectedType
      else allTypesOption

  private def refreshRows(): Unit =
    allRows.setAll(
      snapshotRows()*
    )
    recordPlotSamples()
    refreshMetricNameOptions()
    refreshTypeOptions()
    applyFilter()
    lastUpdated.text = s"Last updated: ${timestampFormatter.format(Instant.now())}"

  private def recordPlotSamples(): Unit =
    val nowMillis = Instant.now().toEpochMilli
    val activeMetricNames = allRows.toSeq
      .map(
        _.nameProp.value
      )
      .toSet
    allRows.foreach(row =>
      val metricName = row.nameProp.value
      val prior = plotHistory.getOrElse(
        metricName,
        Vector.empty
      )
      plotHistory.update(
        metricName,
        (prior :+ MetricStatSample(
          epochMillis = nowMillis,
          node = row.nodeProp.value,
          stat = row.stat
        )).takeRight(
          maxPlotSamples
        )
      )
    )
    plotHistory.keys.toSeq
      .filterNot(
        activeMetricNames.contains
      )
      .foreach(
        plotHistory.remove
      )

  private def showSelectedMetricPlots(): Unit =
    Option(
      metricNameFilter.value.value
    ).filter(
      _ != allMetricNamesOption
    ).foreach(metricName =>
      val samples = plotHistory.getOrElse(
        metricName,
        Vector.empty
      )
      val currentType = allRows.toSeq
        .find(
          _.nameProp.value == metricName
        )
        .map(
          _.typeProp.value
        )
        .getOrElse(
          "Metric"
        )
      val charts = chartsForMetric(
        metricName,
        currentType,
        samples
      )
      val dialog = new Dialog[Unit]:
        title = s"$currentType Plots"
        headerText = metricName
        initOwner(
          ownerWindow
        )
      dialog.dialogPane().buttonTypes = Seq(
        ButtonType.Close
      )
      dialog.dialogPane().content = new VBox:
        spacing = 10
        padding = Insets(
          10
        )
        prefWidth = 980
        prefHeight = 760
        children = charts
      dialog.showAndWait()
    )

  private def showMetricGraphDialog(): Unit =
    val metricNames = allRows.toSeq
      .map(
        _.nameProp.value
      )
      .distinct
      .sorted
    if metricNames.nonEmpty then
      val selectedMetricName = Option(
        metricNameFilter.value.value
      ).filter(
        _ != allMetricNamesOption
      ).filter(
        metricNames.contains
      ).getOrElse(
        metricNames.head
      )
      val metricNameCombo = new ComboBox[String](
        ObservableBuffer(
          metricNames*
        )
      ):
        value = selectedMetricName
        prefWidth = 460
      val valueChoices = new VBox:
        spacing = 4
      val chartContainer = new VBox:
        spacing = 10
      val contentBox = new VBox:
        spacing = 10
        padding = Insets(
          10
        )
        prefWidth = 980
        prefHeight = 760
        children = Seq(
          new HBox:
            spacing = 8
            children = Seq(
              new Label(
                "Metric"
              ),
              metricNameCombo
            )
          ,
          new Label(
            "Values"
          ),
          valueChoices,
          new ScrollPane:
            fitToWidth = true
            content = chartContainer
        )
      def updateChoicesAndCharts(): Unit =
        val metricName = metricNameCombo.value.value
        val metricType = metricTypeForMetric(
          metricName
        ).getOrElse(
          "Metric"
        )
        val choices = plotValuesForMetricType(
          metricType
        )
        val checkBoxes = choices.map(choice =>
          new CheckBox(
            choice.name
          ):
            selected = true
        )
        valueChoices.children = checkBoxes
        def refreshCharts(): Unit =
          val selectedChoices = choices.zip(
            checkBoxes
          ).filter(
            _._2.selected.value
          ).map(
            _._1
          )
          chartContainer.children = selectedChoices.map(choice =>
            buildNodeValueChart(
              metricName = metricName,
              value = choice,
              samples = plotHistory.getOrElse(
                metricName,
                Vector.empty
              )
            )
          )
        checkBoxes.foreach(
          _.selected.onChange {
            (_, _, _) =>
              refreshCharts()
          }
        )
        refreshCharts()
      metricNameCombo.value.onChange {
        (_, _, _) =>
          updateChoicesAndCharts()
      }
      updateChoicesAndCharts()
      val dialog = new Dialog[Unit]:
        title = "Metric Graph"
        headerText = "Select a metric and values to graph"
        initOwner(
          ownerWindow
        )
      dialog.dialogPane().buttonTypes = Seq(
        ButtonType.Close
      )
      dialog.dialogPane().content = contentBox
      dialog.showAndWait()

  private def chartsForMetric(
      metricName: String,
      metricType: String,
      samples: Vector[MetricStatSample]
  ): Seq[Node] =
    metricType match
      case "Timer" =>
        Seq(
          buildChart(
            chartTitle = "Latency",
            yAxisLabel = "Milliseconds",
            samples = samples,
            values = Seq(
              plotValue("p50", "Milliseconds", sample => nanosToMillis(sample.stat.asInstanceOf[TimerSnapshot].p50)),
              plotValue("p95", "Milliseconds", sample => nanosToMillis(sample.stat.asInstanceOf[TimerSnapshot].p95)),
              plotValue("p99", "Milliseconds", sample => nanosToMillis(sample.stat.asInstanceOf[TimerSnapshot].p99)),
              plotValue("max", "Milliseconds", sample => nanosToMillis(sample.stat.asInstanceOf[TimerSnapshot].max.toDouble))
            )
          ),
          buildChart(
            chartTitle = "Throughput",
            yAxisLabel = "Events per minute / count",
            samples = samples,
            values = Seq(
              plotValue("m1_rate", "Events per minute", sample => sample.stat.asInstanceOf[TimerSnapshot].m1 * 60.0),
              plotValue("count", "Count", sample => sample.stat.asInstanceOf[TimerSnapshot].count.toDouble)
            )
          )
        )
      case "Meter" =>
        Seq(
          buildChart(
            chartTitle = "Throughput",
            yAxisLabel = "Events per minute / count",
            samples = samples,
            values = Seq(
              plotValue("m1_rate", "Events per minute", sample => sample.stat.asInstanceOf[MeterSnapshot].m1 * 60.0),
              plotValue("count", "Count", sample => sample.stat.asInstanceOf[MeterSnapshot].count.toDouble)
            )
          )
        )
      case "Histogram" =>
        Seq(
          buildChart(
            chartTitle = "Distribution",
            yAxisLabel = "Value",
            samples = samples,
            values = Seq(
              plotValue("p50", "Value", sample => sample.stat.asInstanceOf[HistogramSnapshot].p50),
              plotValue("p95", "Value", sample => sample.stat.asInstanceOf[HistogramSnapshot].p95),
              plotValue("p99", "Value", sample => sample.stat.asInstanceOf[HistogramSnapshot].p99),
              plotValue("max", "Value", sample => sample.stat.asInstanceOf[HistogramSnapshot].max.toDouble)
            )
          ),
          buildChart(
            chartTitle = "Count",
            yAxisLabel = "Count",
            samples = samples,
            values = Seq(
              plotValue("count", "Count", sample => sample.stat.asInstanceOf[HistogramSnapshot].count.toDouble)
            )
          )
        )
      case "Counter" =>
        Seq(
          buildChart(
            chartTitle = "Count",
            yAxisLabel = "Count",
            samples = samples,
            values = Seq(
              plotValue("count", "Count", sample => sample.stat.asInstanceOf[CounterSnapshot].count.toDouble)
            )
          )
        )
      case "Gauge" =>
        Seq(
          buildChart(
            chartTitle = "Value",
            yAxisLabel = "Value",
            samples = samples,
            values = Seq(
              plotValue("value", "Value", sample => sample.stat.asInstanceOf[GaugeSnapshot].value.toDoubleOption.getOrElse(Double.NaN))
            )
          )
        )
      case _ =>
        Seq(
          buildChart(
            chartTitle = "Count",
            yAxisLabel = "Count",
            samples = samples,
            values = Seq(
              plotValue("samples", "Count", _ => 1.0)
            )
          )
        )

  private def metricTypeForMetric(
      metricName: String
  ): Option[String] =
    allRows.toSeq
      .find(
        _.nameProp.value == metricName
      )
      .map(
        _.typeProp.value
      )

  private def plotValuesForMetricType(
      metricType: String
  ): Seq[MetricPlotValue] =
    metricType match
      case "Timer" =>
        Seq(
          plotValue("p50", "Milliseconds", sample => nanosToMillis(sample.stat.asInstanceOf[TimerSnapshot].p50)),
          plotValue("p95", "Milliseconds", sample => nanosToMillis(sample.stat.asInstanceOf[TimerSnapshot].p95)),
          plotValue("p99", "Milliseconds", sample => nanosToMillis(sample.stat.asInstanceOf[TimerSnapshot].p99)),
          plotValue("max", "Milliseconds", sample => nanosToMillis(sample.stat.asInstanceOf[TimerSnapshot].max.toDouble)),
          plotValue("m1_rate", "Events per minute", sample => sample.stat.asInstanceOf[TimerSnapshot].m1 * 60.0),
          plotValue("count", "Count", sample => sample.stat.asInstanceOf[TimerSnapshot].count.toDouble)
        )
      case "Meter" =>
        Seq(
          plotValue("m1_rate", "Events per minute", sample => sample.stat.asInstanceOf[MeterSnapshot].m1 * 60.0),
          plotValue("count", "Count", sample => sample.stat.asInstanceOf[MeterSnapshot].count.toDouble)
        )
      case "Histogram" =>
        Seq(
          plotValue("p50", "Value", sample => sample.stat.asInstanceOf[HistogramSnapshot].p50),
          plotValue("p95", "Value", sample => sample.stat.asInstanceOf[HistogramSnapshot].p95),
          plotValue("p99", "Value", sample => sample.stat.asInstanceOf[HistogramSnapshot].p99),
          plotValue("max", "Value", sample => sample.stat.asInstanceOf[HistogramSnapshot].max.toDouble),
          plotValue("count", "Count", sample => sample.stat.asInstanceOf[HistogramSnapshot].count.toDouble)
        )
      case "Counter" =>
        Seq(
          plotValue("count", "Count", sample => sample.stat.asInstanceOf[CounterSnapshot].count.toDouble)
        )
      case "Gauge" =>
        Seq(
          plotValue("value", "Value", sample => sample.stat.asInstanceOf[GaugeSnapshot].value.toDoubleOption.getOrElse(Double.NaN))
        )
      case _ =>
        Seq(
          plotValue("samples", "Count", _ => 1.0)
        )

  private def plotValue(
      name: String,
      yAxisLabel: String,
      value: MetricStatSample => Double
  ): MetricPlotValue =
    MetricPlotValue(
      name = name,
      yAxisLabel = yAxisLabel,
      value = value
    )

  private def buildNodeValueChart(
      metricName: String,
      value: MetricPlotValue,
      samples: Vector[MetricStatSample]
  ): Node =
    buildChart(
      chartTitle = s"$metricName ${value.name}",
      yAxisLabel = value.yAxisLabel,
      samples = samples,
      values = Seq(
        value
      )
    )

  private def buildChart(
      chartTitle: String,
      yAxisLabel: String,
      samples: Vector[MetricStatSample],
      values: Seq[MetricPlotValue]
  ): Node =
    val startedAtMillis = samples.headOption
      .map(
        _.epochMillis
      )
      .getOrElse(
        Instant.now().toEpochMilli
      )
    val xAxis = new NumberAxis:
      label = "Elapsed seconds"
      forceZeroInRange = true
    val yAxis = new NumberAxis:
      label = yAxisLabel
      forceZeroInRange = true
    val multipleNodes = samples.map(_.node).distinct.size > 1
    val seriesDefinitions = values.flatMap {
      case value =>
        samples
          .groupBy(
            _.node
          )
          .toSeq
          .sortBy(
            _._1
          )
          .map {
            case (nodeName, nodeSamples) =>
              val seriesName =
                if multipleNodes then s"$nodeName ${value.name}"
                else value.name
              PlotSeriesDefinition(
                name = seriesName,
                series = new XYChart.Series[Number, Number]:
                  name = seriesName
                  data = nodeSamples
                    .flatMap(sample =>
                      val y = value.value(
                        sample
                      )
                      Option.when(
                        !y.isNaN && !y.isInfinite
                      )(
                        new JfxXYChart.Data[Number, Number](
                          elapsedSeconds(
                            startedAtMillis,
                            sample.epochMillis
                          ),
                          y
                        )
                      )
                    )
              )
          }
    }
    val lineChart = new LineChart[Number, Number](
      xAxis,
      yAxis
    ):
      title = chartTitle
      createSymbols = false
      animated = false
      legendVisible = false
      prefHeight = 340
      data = seriesDefinitions.map(
        _.series
      ).map(
        _.delegate
      )
    Platform.runLater(
      applyPlotSeriesColors(
        seriesDefinitions
      )
    )
    new VBox:
      spacing = 4
      children = Seq(
        lineChart,
        new HBox:
          spacing = 12
          padding = Insets(
            0,
            0,
            6,
            8
          )
          children = seriesDefinitions.zipWithIndex.map {
            case (seriesDefinition, index) =>
              new HBox:
                spacing = 4
                children = Seq(
                  new Rectangle:
                    width = 18
                    height = 4
                    fill = Color.web(
                      plotColor(
                        index
                      )
                    )
                  ,
                  new Label(
                    seriesDefinition.name
                  )
                )
          }
      )

  private def applyPlotSeriesColors(
      seriesDefinitions: Seq[PlotSeriesDefinition]
  ): Unit =
    seriesDefinitions.zipWithIndex.foreach {
      case (seriesDefinition, index) =>
        Option(
          seriesDefinition.series.delegate.getNode
        ).foreach(
          _.setStyle(
            s"-fx-stroke: ${plotColor(index)}; -fx-stroke-width: 2.2px;"
          )
        )
    }

  private def plotColor(
      index: Int
  ): String =
    plotColors(
      index % plotColors.size
    )

  private def elapsedSeconds(
      startMillis: Long,
      epochMillis: Long
  ): Double =
    (epochMillis - startMillis).toDouble / 1000.0

  private def nanosToMillis(
      nanos: Double
  ): Double =
    if nanos.isNaN || nanos.isInfinite then Double.NaN
    else nanos / 1000000.0

  private final case class MetricStatSample(
      epochMillis: Long,
      node: String,
      stat: MetricStat
  )

  private final case class PlotSeriesDefinition(
      name: String,
      series: XYChart.Series[Number, Number]
  )

  private final case class MetricPlotValue(
      name: String,
      yAxisLabel: String,
      value: MetricStatSample => Double
  )
