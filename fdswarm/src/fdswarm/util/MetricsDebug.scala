package fdswarm.util

import fdswarm.telemetry.Metrics

object MetricsDebug:

  def dumpMetrics(
    otelMetrics: Metrics
  ): String =
    otelMetrics.scrape()
