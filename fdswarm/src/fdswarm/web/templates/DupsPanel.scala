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

package fdswarm.web.templates

import fdswarm.util.TimeHelpers.localFrom
import java.time.Instant
import scalatags.Text.all.*

case class DupEntry(callsign: String, timestamp: String)

object DupsPanel {
  def apply(dups: Seq[DupEntry], totalCount: Int): Modifier =
    if dups.isEmpty then div()
    else
      div(cls := "d-flex flex-wrap")(
        dups.map { q =>
          span(cls := "dup-callsign", title := q.timestamp)(q.callsign)
        },
        if totalCount > 45 then
          span(cls := "dup-callsign-more")(s"${totalCount - 45} more possible dups")
        else
          modifier()
      )
}
