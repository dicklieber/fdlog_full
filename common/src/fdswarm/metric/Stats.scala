package fdswarm.metric

import com.codahale.metrics.{MetricRegistry, SharedMetricRegistries}
import io.circe.Codec

case class Stats (    udpSent: MeterSnapshot,
                      udpReceived: MeterSnapshot,
                      httpSent: MeterSnapshot,
                      httpReceived: MeterSnapshot
                 ) derives Codec.AsObject

object Stats:
  given registry: MetricRegistry = SharedMetricRegistries.getOrCreate("default")

  def apply(): Stats =
    Stats(MeterSnapshot.empty, MeterSnapshot.empty, MeterSnapshot.empty, MeterSnapshot.empty)
