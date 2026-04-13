package fdswarm.logging

import org.apache.logging.log4j.{CloseableThreadContext, LogManager, Logger}

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID

import scala.jdk.CollectionConverters.*

final class StructuredLogger private(private val logger: Logger):

  def whenTraceEnabled[T](
    body: => T,
    orElse: => T = null.asInstanceOf[T]
  ): T =
    if logger.isTraceEnabled then body
    else orElse

  def whenDebugEnabled[T](
    body: => T,
    orElse: => T = null.asInstanceOf[T]
  ): T =
    if logger.isDebugEnabled then body
    else orElse

  def whenInfoEnabled[T](
    body: => T,
    orElse: => T = null.asInstanceOf[T]
  ): T =
    if logger.isInfoEnabled then body
    else orElse

  def whenWarnEnabled[T](
    body: => T,
    orElse: => T = null.asInstanceOf[T]
  ): T =
    if logger.isWarnEnabled then body
    else orElse

  def whenErrorEnabled[T](
    body: => T,
    orElse: => T = null.asInstanceOf[T]
  ): T =
    if logger.isErrorEnabled then body
    else orElse

  def whenFatalEnabled[T](
    body: => T,
    orElse: => T = null.asInstanceOf[T]
  ): T =
    if logger.isFatalEnabled then body
    else orElse

  def trace(message: String, args: (String, Any)*): Unit =
    if logger.isTraceEnabled then
      withFields(args):
        logger.trace(message)

  def debug(message: String, args: (String, Any)*): Unit =
    if logger.isDebugEnabled then
      withFields(args):
        logger.debug(message)

  def info(message: String, args: (String, Any)*): Unit =
    if logger.isInfoEnabled then
      withFields(args):
        logger.info(message)

  def warn(message: String, args: (String, Any)*): Unit =
    if logger.isWarnEnabled then
      withFields(args):
        logger.warn(message)

  def error(message: String, args: (String, Any)*): Unit =
    if logger.isErrorEnabled then
      withFields(args):
        logger.error(message)

  def error(message: String, throwable: Throwable, args: (String, Any)*): Unit =
    if logger.isErrorEnabled then
      withFields(args):
        logger.error(message, throwable)

  def fatal(message: String, args: (String, Any)*): Unit =
    if logger.isFatalEnabled then
      withFields(args):
        logger.fatal(message)

  def fatal(message: String, throwable: Throwable, args: (String, Any)*): Unit =
    if logger.isFatalEnabled then
      withFields(args):
        logger.fatal(message, throwable)

  private inline def withFields(args: Seq[(String, Any)])(body: => Unit): Unit =
    val fields = normalizeArgs(args)
    val closeable =
      if fields.isEmpty then null
      else CloseableThreadContext.putAll(fields.asJava)
    try
      body
    finally
      if closeable != null then closeable.close()

  private def normalizeArgs(args: Seq[(String, Any)]): Map[String, String] =
    val flattened = args.flatMap(expandField)
    flattened.iterator.map: (key, value) =>
      key -> stringify(value)
    .toMap

  private def expandField(field: (String, Any)): Seq[(String, Any)] =
    field match
      case (name, null) =>
        Seq(name -> null)

      case (_, fields: LogFields) =>
        fields.logFields.flatMap(expandField)

      case (name, opt: Option[?]) =>
        opt match
          case Some(value) => expandField(name -> value)
          case None        => Seq(name -> null)

      case (name, array: Array[?]) =>
        Seq(name -> array.iterator.map(stringify).mkString("[", ", ", "]"))

      case (name, iterable: Iterable[?]) =>
        Seq(name -> iterable.iterator.map(stringify).mkString("[", ", ", "]"))

      case (name, map: scala.collection.Map[?, ?]) =>
        val rendered =
          map.iterator
            .map: (k, v) =>
              s"${stringify(k)}=${stringify(v)}"
            .mkString("{", ", ", "}")
        Seq(name -> rendered)

      case other =>
        Seq(other)

  private def stringify(value: Any): String =
    value match
      case null                => "null"
      case s: String           => s
      case c: Char             => c.toString
      case b: Boolean          => b.toString
      case n: Byte             => n.toString
      case n: Short            => n.toString
      case n: Int              => n.toString
      case n: Long             => n.toString
      case n: Float            => n.toString
      case n: Double           => n.toString
      case n: BigInt           => n.toString
      case n: BigDecimal       => n.toString
      case uuid: UUID          => uuid.toString

      case instant: Instant         => DateTimeFormatter.ISO_INSTANT.format(instant)
      case date: LocalDate          => DateTimeFormatter.ISO_LOCAL_DATE.format(date)
      case time: LocalTime          => DateTimeFormatter.ISO_LOCAL_TIME.format(time)
      case dt: LocalDateTime        => DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dt)
      case odt: OffsetDateTime      => DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(odt)
      case zdt: ZonedDateTime       => DateTimeFormatter.ISO_ZONED_DATE_TIME.format(zdt)

      case duration: Duration       => duration.toString
      case period: Period           => period.toString

      case enumValue: Enum[?]       => enumValue.name()

      case product: Product =>
        val fields =
          product.productElementNames.zip(product.productIterator).map: (name, value) =>
            s"$name=${stringify(value)}"
          .mkString(", ")
        s"${product.productPrefix}($fields)"

      case other =>
        other.toString

object StructuredLogger:

  def apply(clazz: Class[?]): StructuredLogger =
    new StructuredLogger(LogManager.getLogger(clazz))

  def apply(name: String): StructuredLogger =
    new StructuredLogger(LogManager.getLogger(name))
