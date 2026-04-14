package fdswarm.logging

trait LazyStructuredLogging(
                             locus: Locus = Locus.ClassName
                           ):
  protected lazy val logger: StructuredLogger =
    val loggerName = locus match
      case Locus.ClassName =>
        getClass.getName.stripSuffix("$")
      case _ =>
        locus.value
    StructuredLogger(
      loggerName
    )
