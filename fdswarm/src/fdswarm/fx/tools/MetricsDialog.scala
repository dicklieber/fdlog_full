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

import com.codahale.metrics.{Counter, Gauge, Histogram, Meter, Metric, Timer}
import fdswarm.telemetry.Metrics
import fdswarm.util.DurationFormat
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.animation.{KeyFrame, Timeline}
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{ButtonType, Dialog, Label, TableColumn, TableView, TextField}
import scalafx.scene.layout.VBox
import scalafx.stage.Window
import scalafx.util.Duration as FxDuration

import java.time.{Duration as JDuration, Instant, ZoneId}
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters.*

@Singleton
final class MetricsDialog @Inject() (
    metrics: Metrics
):
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
      allRows.setAll(
        snapshotRows()*
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

  private def snapshotRows(): Seq[MetricRow] =
    metrics.registryRef.getMetrics.asScala.toSeq
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
