package fdswarm.logging

import fdswarm.logging.LazyStructuredLogging.{loggerNames, normalizeClassName}

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.CollectionHasAsScala

object LazyStructuredLogging:
  private val loggerNames = ConcurrentHashMap.newKeySet[String]()

  def selectableLoggerNames: Seq[String] =
    (loggerNames.asScala.toSeq).distinct
      .sorted(using Ordering.String)

  def normalizeClassName(className: String): String =
    val normalized = className.stripSuffix("$").stripPrefix("fdswarm.").trim
    loggerNames.add(normalized)
    normalized

trait LazyStructuredLogging():
  private val loggerName: String = LazyStructuredLogging.normalizeClassName(getClass.getName)
  protected lazy val logger: StructuredLogger = StructuredLogger(loggerName)
