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

package fdswarm.util

object HamPhonetic:

  enum Style:
    case Standard   // ICAO: Nine, Zero
    case Ham        // Ham-style: Niner, Zero (you can tweak more)

  private val standard: Map[Char, String] =
    Map(
      'A' -> "Alpha",   'B' -> "Bravo",    'C' -> "Charlie",
      'D' -> "Delta",   'E' -> "Echo",     'F' -> "Foxtrot",
      'G' -> "Golf",    'H' -> "Hotel",    'I' -> "India",
      'J' -> "Juliett", 'K' -> "Kilo",     'L' -> "Lima",
      'M' -> "Mike",    'N' -> "November", 'O' -> "Oscar",
      'P' -> "Papa",    'Q' -> "Quebec",   'R' -> "Romeo",
      'S' -> "Sierra",  'T' -> "Tango",    'U' -> "Uniform",
      'V' -> "Victor",  'W' -> "Whiskey",  'X' -> "X-ray",
      'Y' -> "Yankee",  'Z' -> "Zulu",
      '0' -> "Zero",    '1' -> "One",      '2' -> "Two",
      '3' -> "Three",   '4' -> "Four",     '5' -> "Five",
      '6' -> "Six",     '7' -> "Seven",    '8' -> "Eight",
      '9' -> "Nine"
    )

  private val hamOverrides: Map[Char, String] =
    Map(
      '9' -> "Niner"
      // you can add more if you want:
      // '0' -> "Zero", 'Z' -> "Zed", etc.
    )

  def fromChar(ch: Char, style: Style = Style.Ham): String =
    val c = ch.toUpper
    style match
      case Style.Standard =>
        standard.getOrElse(c, ch.toString)
      case Style.Ham =>
        hamOverrides.getOrElse(c, standard.getOrElse(c, ch.toString))

  def fromString(s: String, style: Style = Style.Standard): String =
    s.map(ch => fromChar(ch, style)).mkString(" ")