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

import fdswarm.replication.{NodeStatusDispatcher, Service}
import fdswarm.util.{DurationFormat, NodeIdentity}
import jakarta.inject.{Inject, Singleton}
import scalafx.beans.property.StringProperty
import scalafx.stage.Window

import java.time.Duration as JDuration
import scala.collection.concurrent.TrieMap

@Singleton
final class StatsManager @Inject() (nodeStatusDispatcher: NodeStatusDispatcher):
  val statsByNode: TrieMap[NodeIdentity, Seq[MetricStat]] =
    TrieMap.empty[NodeIdentity, Seq[MetricStat]]

  nodeStatusDispatcher.addListener(
    service = Service.Status,
    singleListener = false
  )((nodeIdentity, statusMessage) =>
    statsByNode.update(
      nodeIdentity,
      statusMessage.metrics
    )
  )

  def show(
      ownerWindow: Window
  ): Unit =
    new StatsDialog(
      ownerWindow = ownerWindow,
      snapshotRows = () => snapshotRows()
    ).showAndWait()

  private def snapshotRows(): Seq[MetricStatRow] =
    statsByNode.toSeq
      .sortBy(_._1.toString)
      .flatMap {
        case (nodeIdentity, stats) =>
          stats
            .sortBy(
              _.metricName
            )
            .map(stat =>
              new MetricStatRow(
                node = nodeIdentity.toString,
                name = stat.metricName,
                metricType = stat.metricType.toString,
                value = formatMetricStat(
                  stat
                ),
                stat = stat
              )
            )
      }

  private def formatMetricStat(
      stat: MetricStat
  ): String =
    stat match
      case gauge: GaugeSnapshot =>
        gauge.value
      case counter: CounterSnapshot =>
        s"count=${counter.count}"
      case meter: MeterSnapshot =>
        s"count=${meter.count}, m1=${formatRatePerMinute(meter.m1)}, m5=${formatRatePerMinute(meter.m5)}, m15=${formatRatePerMinute(meter.m15)}"
      case histogram: HistogramSnapshot =>
        s"count=${histogram.count}, min=${histogram.min}, max=${histogram.max}, p50=${histogram.p50}, p95=${histogram.p95}, p99=${histogram.p99}"
      case timer: TimerSnapshot =>
        s"count=${timer.count}, m1=${formatRatePerMinute(timer.m1)}, m5=${formatRatePerMinute(timer.m5)}, m15=${formatRatePerMinute(timer.m15)}, p50=${formatNanosDuration(timer.p50)}, p95=${formatNanosDuration(timer.p95)}, p99=${formatNanosDuration(timer.p99)}"

  private def formatRatePerMinute(
      perSecond: Double
  ): String =
    f"${perSecond * 60.0}%.2f/min"

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

final class MetricStatRow(
    node: String,
    name: String,
    metricType: String,
    value: String,
    val stat: MetricStat
):
  val nodeProp: StringProperty = StringProperty(
    node
  )
  val nameProp: StringProperty = StringProperty(
    name
  )
  val typeProp: StringProperty = StringProperty(
    metricType
  )
  val valueProp: StringProperty = StringProperty(
    value
  )
