package fdswarm.logging

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

  private def asEntries(
      args: Seq[Any]
  ): Option[Seq[(String, Any)]] =
    if args.forall(_.isInstanceOf[Tuple2[?, ?]]) then
      val entries = args.collect:
        case (name: String, value) => (name, value)
      if entries.size == args.size then Some(entries)
      else None
    else
      None

  private def toRefs(
      args: Seq[Any]
  ): Seq[AnyRef] =
    args.map:
      case value: AnyRef => value
      case value => value.asInstanceOf[AnyRef]

  def withFields[T](entries: (String, Any)*)(f: => T): T =
    try
      put(entries)
      f
    finally
      remove(entries)

  def trace(
      message: String,
      args: Any*
  ): Unit =
    asEntries(args) match
      case Some(entries) =>
        withFields(entries*):
          underlying.trace(
            message
          )
      case None =>
        underlying.trace(
          message,
          toRefs(args)*
        )

  def info(
      message: String,
      args: Any*
  ): Unit =
    asEntries(args) match
      case Some(entries) =>
        withFields(entries*):
          underlying.info(
            message
          )
      case None =>
        underlying.info(
          message,
          toRefs(args)*
        )

  def debug(
      message: String,
      args: Any*
  ): Unit =
    asEntries(args) match
      case Some(entries) =>
        withFields(entries*):
          underlying.debug(
            message
          )
      case None =>
        underlying.debug(
          message,
          toRefs(args)*
        )

  def warn(
      message: String,
      args: Any*
  ): Unit =
    asEntries(args) match
      case Some(entries) =>
        withFields(entries*):
          underlying.warn(
            message
          )
      case None =>
        underlying.warn(
          message,
          toRefs(args)*
        )

  def error(
      message: String,
      args: Any*
  ): Unit =
    asEntries(args) match
      case Some(entries) =>
        withFields(entries*):
          underlying.error(
            message
          )
      case None =>
        underlying.error(
          message,
          toRefs(args)*
        )

  def error(
      message: String,
      throwable: Throwable
  ): Unit =
    underlying.error(
      message,
      throwable
    )

  def error(
      message: String,
      throwable: Throwable,
      args: Any*
  ): Unit =
    asEntries(args) match
      case Some(entries) =>
        withFields(entries*):
          underlying.error(
            message,
            throwable
          )
      case None =>
        underlying.error(
          message,
          (toRefs(args) :+ throwable.asInstanceOf[AnyRef])*
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

  def whenTraceEnabled(
      f: => Unit
  ): Unit =
    if underlying.isTraceEnabled then
      f

object StructuredLogger:
  def apply(name: String): StructuredLogger =
    new StructuredLogger(LogManager.getLogger(name))

  def apply(clazz: Class[?]): StructuredLogger =
    new StructuredLogger(LogManager.getLogger(clazz))
