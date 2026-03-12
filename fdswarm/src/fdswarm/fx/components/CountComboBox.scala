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

package fdswarm.fx.components

import fdswarm.model.Choice

/**
 * Used for nTransmitters in Qso Search.
 */
class CountComboBox
    extends AnyComboBox[String](
      (1 to 10).map(i =>
        new Choice[String]:
          override val value: String = i.toString
          override val label: String = i.toString
      ) :+ (new Choice[String]:
        override val value: String = " >10"
        override val label: String = " >10")
    ):

  def check(candidate: Int): Boolean =
    value.value match
      case None => true
      case Some(v) =>
        if v == " >10" then candidate > 10
        else v.toInt == candidate
