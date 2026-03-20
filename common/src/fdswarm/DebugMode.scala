package fdswarm

import _root_.io.circe.{Decoder, Encoder}

enum DebugMode(val javaOpt: Option[String]):
  case Off extends DebugMode(None)
  case Debug extends DebugMode(Some("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"))
  case Wait extends DebugMode(Some("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"))

object DebugMode:
  private def fromString(s: String): Option[DebugMode] =
    values.find(_.productPrefix == s)

  given Encoder[DebugMode] =
    Encoder.encodeString.contramap(_.productPrefix)

  given Decoder[DebugMode] =
    Decoder.decodeString.emap { s =>
      fromString(s).toRight(s"Invalid DebugMode: $s")
    }