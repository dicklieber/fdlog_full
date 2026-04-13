package logging

trait LazyStructuredLogging:
  protected lazy val logger: StructuredLogger =
    StructuredLogger(
      getClass.getName.stripSuffix("$")
    )
