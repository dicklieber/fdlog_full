package fdswarm.metric

import com.codahale.metrics
import com.codahale.metrics.MetricRegistry
import io.circe.Codec
import sttp.tapir.Schema

/**
  * @param count how many since app started.
  * @param m1 perSecond
  * @param m5 perSecond
  * @param m15 perSecond
  */
case class MeterSnapshot(
    count: Long,
    m1: Double,
    m5: Double,
    m15: Double
) derives Codec.AsObject, Schema:
  override def toString: String =
    s"count:$count, m1=${m1 * 60}/min, m5=${m5 * 60}/min, m15=${m15 * 60}/min)"
object MeterSnapshot:
  val empty: MeterSnapshot =
    MeterSnapshot(
      count = 0L,
      m1 = 0.0,
      m5 = 0.0,
      m15 = 0.0
    )

  def apply(name: String)(using registry: MetricRegistry): MeterSnapshot =
    val meter: metrics.Meter = registry.meter(name)
    MeterSnapshot(
      count = meter.getCount,
      m1 = meter.getOneMinuteRate,
      m5 = meter.getFiveMinuteRate,
      m15 = meter.getFifteenMinuteRate
    )
