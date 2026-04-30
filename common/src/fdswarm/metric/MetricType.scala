package fdswarm.metric

import io.circe.{Decoder, Encoder}
import io.dropwizard.metrics5.*
import sttp.tapir.Schema

enum MetricType[T <: MetricStat, D <: Metric]:
  type Snapshot = T
  type DropwizardMetric = D
  case Gauge extends MetricType[GaugeSnapshot, io.dropwizard.metrics5.Gauge[?]]
  case Counter extends MetricType[CounterSnapshot, io.dropwizard.metrics5.Counter]
  case Meter extends MetricType[MeterSnapshot, io.dropwizard.metrics5.Meter]
  case Histogram extends MetricType[HistogramSnapshot, io.dropwizard.metrics5.Histogram]
  case Timer extends MetricType[TimerSnapshot, io.dropwizard.metrics5.Timer]

object MetricType:
  given [T <: MetricStat, D <: Metric]: Schema[MetricType[T, D]] =
    Schema.string

  given [T <: MetricStat, D <: Metric]: Encoder[MetricType[T, D]] =
    Encoder.encodeString.contramap(
      _.toString
    )

  given [T <: MetricStat, D <: Metric]: Decoder[MetricType[T, D]] =
    Decoder.decodeString.emap { metricType =>
      MetricType.values
        .find(
          _.toString == metricType
        )
        .map(
          _.asInstanceOf[MetricType[T, D]]
        )
        .toRight(
          s"Unknown metric type: $metricType"
        )
    }
