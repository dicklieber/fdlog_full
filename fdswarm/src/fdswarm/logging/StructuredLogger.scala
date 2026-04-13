package logging

import org.apache.logging.log4j.{LogManager, ThreadContext, Logger as JLogger}

import scala.util.control.NonFatal

final class StructuredLogger private (underlying: JLogger):

  private def put(entries: Seq[(String, Any)]): Unit =
    entries.foreach:
      case (name, value) =>
        ThreadContext.put(
          name,
          String.valueOf(value)
        )

  private def remove(entries: Seq[(String, Any)]): Unit =
    entries.foreach:
      case (name, _) =>
        ThreadContext.remove(
          name
        )

  def withFields[T](entries: (String, Any)*)(f: => T): T =
    try
      put(entries)
      f
    finally
      remove(entries)

  def info(
      message: String,
      entries: (String, Any)*
  ): Unit =
    withFields(entries*):
      underlying.info(
        message
      )

  def debug(
      message: String,
      entries: (String, Any)*
  ): Unit =
    withFields(entries*):
      underlying.debug(
        message
      )

  def warn(
      message: String,
      entries: (String, Any)*
  ): Unit =
    withFields(entries*):
      underlying.warn(
        message
      )

  def error(
      message: String,
      entries: (String, Any)*
  ): Unit =
    withFields(entries*):
      underlying.error(
        message
      )

  def error(
      message: String,
      throwable: Throwable,
      entries: (String, Any)*
  ): Unit =
    withFields(entries*):
      underlying.error(
        message,
        throwable
      )

  def timed[T](
      operation: String,
      entries: (String, Any)*
  )(
      f: => T
  ): T =
    val startNs = System.nanoTime()
    try
      val result = f
      val durationMs = (System.nanoTime() - startNs) / 1_000_000L
      info(
        s"$operation completed",
        (entries :+ (LogFields.durationMs.name, durationMs))*
      )
      result
    catch
      case NonFatal(e) =>
        val durationMs = (System.nanoTime() - startNs) / 1_000_000L
        error(
          s"$operation failed",
          e,
          (entries :+ (LogFields.durationMs.name, durationMs))*
        )
        throw e

object StructuredLogger:
  def apply(name: String): StructuredLogger =
    new StructuredLogger(LogManager.getLogger(name))

  def apply(clazz: Class[?]): StructuredLogger =
    new StructuredLogger(LogManager.getLogger(clazz))
