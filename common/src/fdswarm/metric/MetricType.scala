package fdswarm.metric

import io.circe.{Decoder, Encoder}

enum MetricType[T <: MetricStat]:
  type Snapshot = T
  case Gauge extends MetricType[GaugeSnapshot]
  case Counter extends MetricType[CounterSnapshot]
  case Meter extends MetricType[MeterSnapshot]
  case Histogram extends MetricType[HistogramSnapshot]
  case Timer extends MetricType[TimerSnapshot]

object MetricType:
  given [T <: MetricStat]: Encoder[MetricType[T]] =
    Encoder.encodeString.contramap(
      _.toString
    )

  given [T <: MetricStat]: Decoder[MetricType[T]] =
    Decoder.decodeString.emap { metricType =>
      MetricType.values
        .find(
          _.toString == metricType
        )
        .map(
          _.asInstanceOf[MetricType[T]]
        )
        .toRight(
          s"Unknown metric type: $metricType"
        )
    }
