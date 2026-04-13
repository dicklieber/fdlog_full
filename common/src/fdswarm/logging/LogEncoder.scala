package fdswarm.logging

import java.time.Instant

trait LogEncoder[-A]:
  def encode(value: A): String

object LogEncoder:
  given LogEncoder[String] with
    def encode(value: String): String = value

  given LogEncoder[Int] with
    def encode(value: Int): String = value.toString

  given LogEncoder[Long] with
    def encode(value: Long): String = value.toString

  given LogEncoder[Double] with
    def encode(value: Double): String = value.toString

  given LogEncoder[Float] with
    def encode(value: Float): String = value.toString

  given LogEncoder[Boolean] with
    def encode(value: Boolean): String = value.toString

  given LogEncoder[Instant] with
    def encode(value: Instant): String = value.toString

  given [A <: Enum[A]]: LogEncoder[A] with
    def encode(value: A): String = value.name()

  given [A](using enc: LogEncoder[A]): LogEncoder[Option[A]] with
    def encode(value: Option[A]): String =
      value match
        case Some(v) => enc.encode(v)
        case None    => "null"
