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

package fdswarm.web

import fdswarm.model.{BandMode, StationConfig}
import fdswarm.util.Ids.Id
import io.circe.Codec
import java.time.ZonedDateTime

/**
 * Statistics for a web session.
 *
 * @param qsosEntered total number of QSOs entered in this session.
 * @param lastTouched when this session was last used.
 */
case class SessionStats(
                         qsosEntered: Int = 0,
                         lastTouched: ZonedDateTime = ZonedDateTime.now()
                       ) derives Codec.AsObject

/**
 * A server-side session for a web client.
 *
 * @param station the station configuration for this session.
 * @param bandMode the current band and mode for this session.
 * @param qsoLines number of QSO lines to display.
 * @param sessionId unique session identifier.
 */
case class WebSession(
                       station: StationConfig,
                       bandMode: BandMode,
                       qsoLines: Int,
                       sessionId: Id
                     ) derives Codec.AsObject
