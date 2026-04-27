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

package fdswarm.fx.tools

import com.codahale.metrics.*
import fdswarm.telemetry.Metrics
import fdswarm.util.DurationFormat
import jakarta.inject.{Inject, Singleton}
import javafx.scene.chart.XYChart as JfxXYChart
import scalafx.Includes.*
import scalafx.animation.{KeyFrame, Timeline}
import scalafx.application.Platform
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.chart.{LineChart, NumberAxis, XYChart}
import scalafx.scene.control.*
import scalafx.scene.input.MouseButton
import scalafx.scene.layout.{HBox, VBox}
import scalafx.stage.Window
import scalafx.util.Duration as FxDuration

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId, Duration as JDuration}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

@Singleton
final class MetricsDialog @Inject() (
    metrics: Metrics
):
  private final case class MetricGraphPoint(
      epochMillis: Long,
      p50: Double,
      p75: Double,
      p90: Double,
      p95: Double,
      p99: Double
  )

  private val maxGraphSamples = 180
  private val metricGraphHistory = mutable.Map.empty[String, Vector[MetricGraphPoint]]

  private val timestampFormatter = DateTimeFormatter
    .ofPattern(
      "yyyy-MM-dd HH:mm:ss"
    )
    .withZone(
      ZoneId.systemDefault()
    )


  final class MetricRow(
      name: String,
      metricType: String,
      value: String,
      p50: String,
      p95: String
  ):
    val nameProp = new StringProperty(
      this,
      "name",
      name
    )
    val typeProp = new StringProperty(
      this,
      "type",
      metricType
    )
    val valueProp = new StringProperty(
      this,
      "value",
      value
    )
    val p50Prop = new StringProperty(
      this,
      "p50",
      p50
    )
    val p95Prop = new StringProperty(
      this,
      "p95",
      p95
    )

  def show(
      ownerWindow: Window
  ): Unit =
    metricGraphHistory.clear()

    val allRows = ObservableBuffer.empty[MetricRow]
    val filteredRows = ObservableBuffer.empty[MetricRow]
    val lastUpdated = new Label("Last updated: -")
    val filterField = new TextField:
      promptText = "Filter metric names..."

    val table = new TableView[MetricRow](
      filteredRows
    ):
      columns ++= Seq(
        new TableColumn[MetricRow, String]:
          text = "Name"
          cellValueFactory = { _.value.nameProp }
          prefWidth = 420
        ,
        new TableColumn[MetricRow, String]:
          text = "Type"
          cellValueFactory = { _.value.typeProp }
          prefWidth = 140
        ,
        new TableColumn[MetricRow, String]:
          text = "Value"
          cellValueFactory = { _.value.valueProp }
          prefWidth = 240
        ,
        new TableColumn[MetricRow, String]:
          text = "P50"
          cellValueFactory = { _.value.p50Prop }
          prefWidth = 160
        ,
        new TableColumn[MetricRow, String]:
          text = "P95"
          cellValueFactory = { _.value.p95Prop }
          prefWidth = 160
      )
      columnResizePolicy = TableView.ConstrainedResizePolicy
      rowFactory = { _ =>
        val row = new TableRow[MetricRow]()
        row.onMouseClicked = event =>
          if event.button == MouseButton.Primary && event.clickCount == 2 && !row.empty.value then
            Option(
              row.item.value
            ).foreach(
              rowItem => showGraphDialogForRow(
                ownerWindow,
                rowItem
              )
            )
        row
      }

    def applyFilter(): Unit =
      val filter = Option(
        filterField.text.value
      ).map(_.trim.toLowerCase).getOrElse("")
      if filter.isEmpty then
        filteredRows.setAll(
          allRows.toSeq*
        )
      else
        filteredRows.setAll(
          allRows.toSeq.filter(
            row => row.nameProp.value.toLowerCase.contains(filter)
          )*
        )

    def refreshRows(): Unit =
      val currentMetrics = metrics.registryRef.getMetrics.asScala.toMap
      recordGraphSample(
        currentMetrics
      )
      allRows.setAll(
        snapshotRows(
          currentMetrics
        )*
      )
      applyFilter()
      lastUpdated.text = s"Last updated: ${timestampFormatter.format(Instant.now())}"

    filterField.text.onChange {
      (_, _, _) =>
        applyFilter()
    }

    val refreshTimeline = new Timeline:
      cycleCount = Timeline.Indefinite
      keyFrames = Seq(
        KeyFrame(
          FxDuration(
            5000
          ),
          onFinished = _ => refreshRows()
        )
      )

    val dialog = new Dialog[Unit]:
      title = "Metrics"
      headerText = "Dropwizard Metrics Registry"
      initOwner(
        ownerWindow
      )

    dialog.dialogPane().buttonTypes = Seq(
      ButtonType.Close
    )
    dialog.dialogPane().content = new VBox:
      spacing = 8
      padding = Insets(
        10
      )
      prefWidth = 900
      prefHeight = 600
      children = Seq(
        filterField,
        lastUpdated,
        table
      )

    dialog.onHidden = _ => refreshTimeline.stop()

    refreshRows()
    refreshTimeline.play()
    dialog.showAndWait()

  private def snapshotRows(
      currentMetrics: Map[String, Metric]
  ): Seq[MetricRow] =
    currentMetrics.toSeq
      .sortBy(_._1)
      .map {
        case (name, metric) =>
          val (metricType, value, p50, p95) = formatMetric(
            metric
          )
          new MetricRow(
            name,
            metricType,
            value,
            p50,
            p95
          )
      }

  private def showGraphDialogForRow(
      ownerWindow: Window,
      row: MetricRow
  ): Unit =
    if !isGraphableMetric(
      row.typeProp.value
    ) then
      ()
    else
      showGraphDialog(
        ownerWindow,
        row.nameProp.value,
        row.typeProp.value
      )

  private def isGraphableMetric(
      metricType: String
  ): Boolean =
    metricType == "Timer" || metricType == "Histogram"

  private def showGraphDialog(
      ownerWindow: Window,
      metricName: String,
      metricType: String
  ): Unit =
    val history = metricGraphHistory.getOrElse(
      metricName,
      Vector.empty
    )
    val yLabel =
      if metricType == "Timer" then "Duration (ms)"
      else "Value"

    val xAxis = new NumberAxis:
      label = "Elapsed seconds"
      forceZeroInRange = true

    val yAxis = new NumberAxis:
      label = yLabel
      forceZeroInRange = true

    val lineChart = new LineChart[Number, Number](
      xAxis,
      yAxis
    ):
      title = metricName
      createSymbols = false
      animated = false
      legendVisible = true

    val startedAtMillis = history.headOption
      .map(
        _.epochMillis
      )
      .getOrElse(
        Instant.now().toEpochMilli
      )

    val p50Series = buildPercentileSeries(
      label = "P50",
      history = history,
      startedAtMillis = startedAtMillis,
      percentile = _.p50
    )
    val p75Series = buildPercentileSeries(
      label = "P75",
      history = history,
      startedAtMillis = startedAtMillis,
      percentile = _.p75
    )
    val p90Series = buildPercentileSeries(
      label = "P90",
      history = history,
      startedAtMillis = startedAtMillis,
      percentile = _.p90
    )
    val p95Series = buildPercentileSeries(
      label = "P95",
      history = history,
      startedAtMillis = startedAtMillis,
      percentile = _.p95
    )
    val p99Series = buildPercentileSeries(
      label = "P99",
      history = history,
      startedAtMillis = startedAtMillis,
      percentile = _.p99
    )
    val seriesByName = Seq(
      "P50" -> p50Series,
      "P75" -> p75Series,
      "P90" -> p90Series,
      "P95" -> p95Series,
      "P99" -> p99Series
    )

    val p50Check = new CheckBox("P50"):
      selected = true
    val p75Check = new CheckBox("P75"):
      selected = true
    val p90Check = new CheckBox("P90"):
      selected = true
    val p95Check = new CheckBox("P95"):
      selected = true
    val p99Check = new CheckBox("P99"):
      selected = true
    val toggles = Seq(
      p50Check,
      p75Check,
      p90Check,
      p95Check,
      p99Check
    )

    def refreshVisibleSeries(): Unit =
      val selectedNames = toggles
        .filter(
          _.selected.value
        )
        .map(
          _.text.value
        )
        .toSet
      lineChart.data = seriesByName.collect {
        case (name, series) if selectedNames.contains(
              name
            ) =>
          series.delegate
      }
      Platform.runLater(
        applySeriesStyles(
          lineChart
        )
      )

    toggles.foreach(
      _.selected.onChange {
        (_, _, _) => refreshVisibleSeries()
      }
    )
    refreshVisibleSeries()

    val content = new VBox:
      spacing = 8
      padding = Insets(
        10
      )
      prefWidth = 860
      prefHeight = 520
      children = Seq(
        new Label(
          s"$metricType metric history for $metricName"
        ),
        new HBox:
          spacing = 12
          children = toggles
        ,
        lineChart
      )

    val dialog = new Dialog[Unit]:
      title = s"$metricType Graph"
      headerText = metricName
      initOwner(
        ownerWindow
      )
    dialog.onShown = _ =>
      Platform.runLater(
        applySeriesStyles(
          lineChart
        )
      )
    dialog.dialogPane().buttonTypes = Seq(
      ButtonType.Close
    )
    dialog.dialogPane().content = content
    dialog.showAndWait()

  private def elapsedSeconds(
      startMillis: Long,
      epochMillis: Long
  ): Double =
    (epochMillis - startMillis).toDouble / 1000.0

  private def recordGraphSample(
      currentMetrics: Map[String, Metric]
  ): Unit =
    val nowMillis = Instant.now().toEpochMilli
    currentMetrics.foreach {
      case (name, histogram: Histogram) =>
        val snapshot = histogram.getSnapshot
        appendGraphPoint(
          metricName = name,
          nowMillis = nowMillis,
          p50 = snapshot.getMedian,
          p75 = snapshot.get75thPercentile,
          p90 = snapshot.getValue(
            0.90
          ),
          p95 = snapshot.get95thPercentile,
          p99 = snapshot.get99thPercentile
        )
      case (name, timer: Timer) =>
        val snapshot = timer.getSnapshot
        appendGraphPoint(
          metricName = name,
          nowMillis = nowMillis,
          p50 = nanosToMillis(
            snapshot.getMedian
          ),
          p75 = nanosToMillis(
            snapshot.get75thPercentile
          ),
          p90 = nanosToMillis(
            snapshot.getValue(
              0.90
            )
          ),
          p95 = nanosToMillis(
            snapshot.get95thPercentile
          ),
          p99 = nanosToMillis(
            snapshot.get99thPercentile
          )
        )
      case _ =>
        ()
    }

    val activeGraphMetrics = currentMetrics.collect {
      case (name, _: Histogram) => name
      case (name, _: Timer) => name
    }.toSet
    metricGraphHistory.keys.toSeq
      .filterNot(
        activeGraphMetrics.contains
      )
      .foreach(
        metricGraphHistory.remove
      )

  private def buildPercentileSeries(
      label: String,
      history: Vector[MetricGraphPoint],
      startedAtMillis: Long,
      percentile: MetricGraphPoint => Double
  ): XYChart.Series[Number, Number] =
    new XYChart.Series[Number, Number]:
      name = label
      data = Seq(
        history.map(
          point =>
            new JfxXYChart.Data[Number, Number](
              elapsedSeconds(
                startedAtMillis,
                point.epochMillis
              ),
              percentile(
                point
              )
            )
        )*
      )

  private def applySeriesStyles(
      lineChart: LineChart[Number, Number]
  ): Unit =
    val styles = Map(
      "P50" -> "#1f77b4",
      "P75" -> "#2ca02c",
      "P90" -> "#ff7f0e",
      "P95" -> "#d62728",
      "P99" -> "#9467bd"
    )
    lineChart.data.value.foreach(
      series =>
        val color = styles.getOrElse(
          series.getName,
          "#666666"
        )
        Option(
          series.getNode
        ).foreach(
          _.setStyle(
            s"-fx-stroke: $color; -fx-stroke-width: 2.4px;"
          )
        )
    )

  private def appendGraphPoint(
      metricName: String,
      nowMillis: Long,
      p50: Double,
      p75: Double,
      p90: Double,
      p95: Double,
      p99: Double
  ): Unit =
    val prior = metricGraphHistory.getOrElse(
      metricName,
      Vector.empty
    )
    metricGraphHistory.update(
      metricName,
      (prior :+ MetricGraphPoint(
        epochMillis = nowMillis,
        p50 = p50,
        p75 = p75,
        p90 = p90,
        p95 = p95,
        p99 = p99
      )).takeRight(
        maxGraphSamples
      )
    )

  private def nanosToMillis(
      nanos: Double
  ): Double =
    if nanos.isNaN || nanos.isInfinite then 0.0
    else nanos / 1000000.0

  private def formatMetric(
      metric: Metric
  ): (String, String, String, String) =
    metric match
      case gauge: Gauge[?] =>
        (
          "Gauge",
          Option(
            gauge.getValue
          ).map(_.toString).getOrElse("null"),
          "",
          ""
        )
      case counter: Counter =>
        (
          "Counter",
          counter.getCount.toString,
          "",
          ""
        )
      case histogram: Histogram =>
        val snapshot = histogram.getSnapshot
        (
          "Histogram",
          s"count=${histogram.getCount}",
          snapshot.getMedian.toString,
          snapshot.get95thPercentile.toString
        )
      case timer: Timer =>
        val snapshot = timer.getSnapshot
        (
          "Timer",
          s"count=${timer.getCount}",
          formatNanosDuration(
            snapshot.getMedian
          ),
          formatNanosDuration(
            snapshot.get95thPercentile
          )
        )
      case meter: Meter =>
        (
          "Meter",
          s"count=${meter.getCount}, m1=${meter.getOneMinuteRate}, m5=${meter.getFiveMinuteRate}, m15=${meter.getFifteenMinuteRate}",
          "",
          ""
        )
      case other =>
        (
          other.getClass.getSimpleName,
          other.toString,
          "",
          ""
        )

  private def formatNanosDuration(
      nanos: Double
  ): String =
    if nanos.isNaN || nanos.isInfinite then
      "n/a"
    else
      DurationFormat(
        JDuration.ofNanos(
          math.max(
            0L,
            math.round(
              nanos
            )
          )
        )
      )
