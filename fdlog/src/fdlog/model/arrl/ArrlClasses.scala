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

package fdlog.model.arrl

import fdlog.model.StationClass

object ArrlClasses:

  val stationClasses: Seq[StationClass] = Seq(
    StationClass('A', "Club or group portable"),
    StationClass('B', "One or two person portable"),
    StationClass('C', "Mobile"),
    StationClass('D', "Home station on commercial power"),
    StationClass('E', "Home station on emergency power (non-commercial)"),
  )