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

import fdswarm.model.Qso
import scalatags.Text.all.*
import java.time.format.DateTimeFormatter
import java.time.ZoneId

object QsoTable {
  private val timeFmt =
    DateTimeFormatter.ofPattern("MMM dd, h:mm a z")
      .withZone(ZoneId.systemDefault())

  def apply(qsos: Seq[Qso]): Modifier =
    div(cls := "table-responsive")(
      table(cls := "table table-striped table-sm")(
        thead(
          tr(
            th("Time"), th("Their Call"), th("Band"), th("Mode"), th("Rcvd"), th("Op")
          )
        ),
        tbody(
          qsos.map { q =>
            tr(
              td(timeFmt.format(q.stamp)),
              td(q.callsign.value),
              td(q.bandMode.band),
              td(q.bandMode.mode),
              td(s"${q.contestClass} ${q.section}"),
              td(q.qsoMetadata.station.operator.value)
            )
          }
        )
      )
    )
}
