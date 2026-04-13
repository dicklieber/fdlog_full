package fdswarm.logging

final case class LogEntry(name: String, value: String)

sealed trait Field[A]:
  def name: String
  def encode(value: A): String

  final def :=(value: A): LogEntry =
    LogEntry(name, encode(value))

final case class LogField[A](name: String)(using encoder: LogEncoder[A]) extends Field[A]:
  override def encode(value: A): String =
    encoder.encode(value)
