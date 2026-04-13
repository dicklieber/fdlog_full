package fdswarm.logging

import fdswarm.logging.StructuredLogger

trait LazyStructuredLogging:
  protected lazy val logger: StructuredLogger =
    StructuredLogger(
      getClass.getName.stripSuffix("$")
    )
