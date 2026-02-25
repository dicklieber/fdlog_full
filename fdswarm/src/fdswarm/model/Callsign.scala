/*
 * Copyright (c) 2026. Dick Lieber, WA9NNN
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or    
 * (at your option) any later version.                                  
 *                                                                      
 * This program is distributed in the hope that it will be useful,      
 * but WITHOUT ANY WARRANTY; without even the implied warranty of       
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        
 * GNU General Public License for more details.                         
 *                                                                      
 * You should have received a copy of the GNU General Public License    
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package fdswarm.model

import io.circe.{Decoder, Encoder}

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
  /**
   * regex that matches a valid callsign.
   *
   * Standard format: [A-Z0-9]{1,3}[0-9][A-Z0-9]{1,4}
   * Optional suffix: /[A-Z0-9]{1,4}
   * Total length restricted by lookahead: (?=.{3,12}$)
   */
  private val regex: scala.util.matching.Regex = """^(?=.{3,12}$)[A-Z0-9]{1,3}[0-9][A-Z0-9]{1,4}(?:/[A-Z0-9]{1,4})?$""".r

  def isValid(str: String): Boolean =
    regex.findFirstIn(str).isDefined

  given Conversion[String, Callsign] = Callsign.apply

  def apply(cs: String): Callsign =
    new Callsign(cs.toUpperCase)



  given Encoder[Callsign] = Encoder.encodeString.contramap(_.value)
  given Decoder[Callsign] = Decoder.decodeString.map(Callsign(_))
  given sttp.tapir.Schema[Callsign] = sttp.tapir.Schema.string