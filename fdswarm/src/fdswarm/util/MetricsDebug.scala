package fdswarm.util

import io.micrometer.core.instrument.*
import scala.jdk.CollectionConverters.*

object MetricsDebug:

  def dumpMetrics(registry: MeterRegistry): String =
    val meters =
      registry.getMeters.asScala
        .sortBy(m => (m.getId.getName, m.getId.getType.name()))

    val sb = new StringBuilder()

    for meter <- meters do
      val id = meter.getId

      sb.append(s"${id.getName}")
      sb.append(s" [${id.getType}]")

      if !id.getTags.isEmpty then
        val tags =
          id.getTags.asScala
            .map(t => s"${t.getKey}=${t.getValue}")
            .sorted
            .mkString(", ")

        sb.append(s" { $tags }")

      sb.append("\n")

      for m <- meter.measure().asScala do
        sb.append(f"  ${m.getStatistic.name()}%-15s ${m.getValue}%f\n")

      sb.append("\n")

    sb.toString