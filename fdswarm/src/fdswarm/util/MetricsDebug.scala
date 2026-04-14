package fdswarm.util

object MetricsDebug:

  def dumpMetrics(
    otelMetrics: OtelMetrics
  ): String =
    otelMetrics.scrape()
