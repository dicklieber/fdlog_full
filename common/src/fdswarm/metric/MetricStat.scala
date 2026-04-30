package fdswarm.metric

import io.circe.Codec
import io.dropwizard.metrics5.*
import sttp.tapir.Schema

sealed trait MetricStat:
  val metricName: String
  val metricType: MetricType[?, ?]

case class GaugeSnapshot(
                          metricName: String = "",
                          value: String,
                          metricType: MetricType[GaugeSnapshot, Gauge[?]] = MetricType.Gauge
                        ) extends MetricStat derives Codec.AsObject, Schema:
  override def toString: String =
    s"value:$value"

object GaugeSnapshot :
  val empty: GaugeSnapshot =
    GaugeSnapshot(
      metricName = "",
      value = ""
    )

  /**
   * Build a snapshot of a gauge.
   */
  def apply(gauge: Gauge[?]): GaugeSnapshot =
    apply(
      gauge = gauge,
      metricName = ""
    )

  /**
   * Build a snapshot of a gauge.
   */
  def apply(gauge: Gauge[?], metricName: String): GaugeSnapshot =
    GaugeSnapshot(
      metricName = metricName,
      value = Option(gauge.getValue).map(_.toString).getOrElse("null")
    )

case class CounterSnapshot(
                            metricName: String = "",
                            count: Long,
                            metricType: MetricType[CounterSnapshot, Counter] = MetricType.Counter
                          ) extends MetricStat derives Codec.AsObject, Schema:
  override def toString: String =
    s"count:$count"

object CounterSnapshot :
  val empty: CounterSnapshot =
    CounterSnapshot(
      metricName = "",
      count = 0L
    )

  /**
   * Build a snapshot of a counter.
   */
  def apply(counter: Counter): CounterSnapshot =
    apply(
      counter = counter,
      metricName = ""
    )

  /**
   * Build a snapshot of a counter.
   */
  def apply(counter: Counter, metricName: String): CounterSnapshot =
    CounterSnapshot(
      metricName = metricName,
      count = counter.getCount
    )

/**
 * @param count how many since app started.
 * @param m1 perSecond
 * @param m5 perSecond
 * @param m15 perSecond
 */
case class MeterSnapshot(
                          metricName: String = "",
                          count: Long,
                          m1: Double,
                          m5: Double,
                          m15: Double,
                          metricType: MetricType[MeterSnapshot, Meter] = MetricType.Meter
                        ) extends MetricStat derives Codec.AsObject, Schema:
  override def toString: String =
    s"count:$count, m1=${m1 * 60}/min, m5=${m5 * 60}/min, m15=${m15 * 60}/min)"
object MeterSnapshot :
  val empty: MeterSnapshot =
    MeterSnapshot(
      metricName = "",
      count = 0L,
      m1 = 0.0,
      m5 = 0.0,
      m15 = 0.0
    )

  /**
   * Build a snapshot of a meter.
   * @return what the meter has now.
   */
  def apply(meter: Meter): MeterSnapshot =
    apply(
      meter = meter,
      metricName = ""
    )

  /**
   * Build a snapshot of a meter.
   * @return what the meter has now.
   */
  def apply(meter: Meter, metricName: String): MeterSnapshot =
    MeterSnapshot(
      metricName = metricName,
      count = meter.getCount,
      m1 = meter.getOneMinuteRate,
      m5 = meter.getFiveMinuteRate,
      m15 = meter.getFifteenMinuteRate
    )

case class HistogramSnapshot(metricName: String = "",
                             count: Long,
                             min: Long,
                             max: Long,
                             p50: Double,
                             p75: Double,
                             p95: Double,
                             p98: Double,
                             p99: Double,
                             p999: Double,
                             metricType: MetricType[HistogramSnapshot, Histogram] = MetricType.Histogram) extends MetricStat derives Codec.AsObject, Schema:
  override def toString: String =
    s"count:$count, min:$min, max:$max, p50:$p50, p95:$p95, p99:$p99"

object HistogramSnapshot :
  val empty: HistogramSnapshot =
    HistogramSnapshot(
      metricName = "",
      count = 0L,
      min = 0L,
      max = 0L,
      p50 = 0.0,
      p75 = 0.0,
      p95 = 0.0,
      p98 = 0.0,
      p99 = 0.0,
      p999 = 0.0
    )

  /**
   * Build a snapshot of a histogram.
   */
  def apply(histogram: Histogram): HistogramSnapshot =
    apply(
      histogram = histogram,
      metricName = ""
    )

  /**
   * Build a snapshot of a histogram.
   */
  def apply(histogram: Histogram, metricName: String): HistogramSnapshot =

    val snapshot: Snapshot = histogram.getSnapshot
    HistogramSnapshot(
      metricName = metricName,
      count = histogram.getCount,
      min = snapshot.getMin,
      max = snapshot.getMax,
      p50 = snapshot.getMedian,
      p75 = snapshot.get75thPercentile,
      p95 = snapshot.get95thPercentile,
      p98 = snapshot.get98thPercentile,
      p99 = snapshot.get99thPercentile,
      p999 = snapshot.get999thPercentile
    )

case class TimerSnapshot(
                          metricName: String = "",
                          count: Long,
                          m1: Double,
                          m5: Double,
                          m15: Double,
                          min: Long,
                          max: Long,
                          p50: Double,
                          p75: Double,
                          p95: Double,
                          p98: Double,
                          p99: Double,
                          p999: Double,
                          metricType: MetricType[TimerSnapshot, Timer] = MetricType.Timer
                        ) extends MetricStat derives Codec.AsObject, Schema:
  override def toString: String =
    s"count:$count, m1=${m1 * 60}/min, m5=${m5 * 60}/min, m15=${m15 * 60}/min, min:$min, max:$max, p50:$p50, p95:$p95, p99:$p99"

object TimerSnapshot :
  val empty: TimerSnapshot =
    TimerSnapshot(
      metricName = "",
      count = 0L,
      m1 = 0.0,
      m5 = 0.0,
      m15 = 0.0,
      min = 0L,
      max = 0L,
      p50 = 0.0,
      p75 = 0.0,
      p95 = 0.0,
      p98 = 0.0,
      p99 = 0.0,
      p999 = 0.0
    )

  /**
   * Build a snapshot of a timer.
   */
  def apply(timer: Timer): TimerSnapshot =
    apply(
      timer = timer,
      metricName = ""
    )

  /**
   * Build a snapshot of a timer.
   */
  def apply(timer: Timer, metricName: String): TimerSnapshot =
    val snapshot: Snapshot = timer.getSnapshot
    TimerSnapshot(
      metricName = metricName,
      count = timer.getCount,
      m1 = timer.getOneMinuteRate,
      m5 = timer.getFiveMinuteRate,
      m15 = timer.getFifteenMinuteRate,
      min = snapshot.getMin,
      max = snapshot.getMax,
      p50 = snapshot.getMedian,
      p75 = snapshot.get75thPercentile,
      p95 = snapshot.get95thPercentile,
      p98 = snapshot.get98thPercentile,
      p99 = snapshot.get99thPercentile,
      p999 = snapshot.get999thPercentile
    )

object MetricSnapshotFactory:
  /**
   * Build the matching fdswarm snapshot for a supported Dropwizard metric.
   */
  def apply(metric: Metric): MetricStat =
    apply(
      metric = metric,
      metricName = ""
    )

  /**
   * Build the matching fdswarm snapshot for a supported Dropwizard metric.
   */
  def apply(metric: Metric, metricName: String): MetricStat =
    fromMetric(
      metric = metric,
      metricName = metricName
    ).getOrElse(
      throw new IllegalArgumentException(
        s"Unsupported Dropwizard metric type: ${metric.getClass.getName}"
      )
    )

  /**
   * Build the matching fdswarm snapshot when the Dropwizard metric type is supported.
   */
  def fromMetric(metric: Metric): Option[MetricStat] =
    fromMetric(
      metric = metric,
      metricName = ""
    )

  /**
   * Build the matching fdswarm snapshot when the Dropwizard metric type is supported.
   */
  def fromMetric(metric: Metric, metricName: String): Option[MetricStat] =
    metric match
      case gauge: Gauge[?] =>
        Some(
          GaugeSnapshot(
            gauge,
            metricName
          )
        )
      case counter: Counter =>
        Some(
          CounterSnapshot(
            counter,
            metricName
          )
        )
      case histogram: Histogram =>
        Some(
          HistogramSnapshot(
            histogram,
            metricName
          )
        )
      case timer: Timer =>
        Some(
          TimerSnapshot(
            timer,
            metricName
          )
        )
      case meter: Meter =>
        Some(
          MeterSnapshot(
            meter,
            metricName
          )
        )
      case _ =>
        None

object MetricStat:
  given Schema[MetricStat] = Schema.derived
