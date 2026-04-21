package fdswarm.logging

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.CollectionHasAsScala

object LazyStructuredLogging:
  private val classNameLoggers = ConcurrentHashMap.newKeySet[String]()
  private val explicitLocusLoggers: Seq[String] =
    Locus.values
      .filter(
        _ != Locus.ClassName
      )
      .map(
        _.value
      )
      .toSeq

  private def normalizeClassName(
                                  className: String
                                ): String =
    className
      .stripSuffix(
        "$"
      )
      .trim

  def registerClassNameLogger(
                               className: String
                             ): Unit =
    val normalized = normalizeClassName(
      className
    )
    if normalized.nonEmpty then
      classNameLoggers.add(
        normalized
      )

  def selectableLoggerNames: Seq[String] =
    (classNameLoggers.asScala.toSeq ++ explicitLocusLoggers)
      .distinct
      .sorted(using Ordering.String)

trait LazyStructuredLogging(
                             locus: Locus = Locus.ClassName
                           ):
  if locus == Locus.ClassName then
    LazyStructuredLogging.registerClassNameLogger(
      getClass.getName
    )

  protected lazy val logger: StructuredLogger =
    val loggerName = locus match
      case Locus.ClassName =>
        getClass.getName.stripSuffix("$")
      case _ =>
        locus.value
    StructuredLogger(
      loggerName
    )
