package fdswarm.model

import upickle.default.*

/**
 * A callsign. always uppercase.
 *
 * @param value the actual callsign.
 */
final class Callsign private(val value: String) extends AnyRef:
  def startsWith(startofCallsign: String): Boolean = value.startsWith(startofCallsign)

  override def toString: String = value

  override def equals(other: Any): Boolean =
    other match
      case c: Callsign => this.value == c.value
      case _ => false

object Callsign:
  given Conversion[String, Callsign] = Callsign.apply

  def apply(cs: String): Callsign =
    new Callsign(cs.toUpperCase)

  // uPickle ReadWriter: represent Callsign as a JSON string
  given ReadWriter[Callsign] =
  readwriter[String].bimap[Callsign](
    _.value, // write Callsign -> String
    Callsign(_) // read String -> Callsign (will uppercase via apply)
  )