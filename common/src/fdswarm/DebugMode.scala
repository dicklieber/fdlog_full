package fdswarm
import _root_.io.circe.Codec

enum DebugMode derives Codec:
  case Off, Debug, Wait

  def javaOpt(port: Int): Option[String] =
    this match
      case Off   => None
      case Debug => Some(s"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$port")
      case Wait  => Some(s"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:$port")
