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

import fdswarm.model.BandMode
import fdswarm.model.BandMode.{Band, Mode}
import scalatags.Text.all.*

object BandModeMatrix {
  def apply(bands: Seq[Band], modes: Seq[Mode], selected: BandMode): Modifier =
    div(cls := "band-mode-matrix")(
      table(cls := "table table-bordered table-sm")(
        thead(
          tr(
            th("Mode"),
            bands.map(th(_))
          )
        ),
        tbody(
          modes.map { m =>
            tr(
              td(strong(m)),
              bands.map { b =>
                val isSelected = selected.band == b && selected.mode == m
                td(
                  form(method := "POST", action := "/web/select-band-mode")(
                    input(tpe := "hidden", name := "band", value := b),
                    input(tpe := "hidden", name := "mode", value := m),
                    button(tpe := "submit", cls := s"btn btn-sm ${if isSelected then "btn-primary" else "btn-outline-secondary"}")(b)
                  )
                )
              }
            )
          }
        )
      )
    )
}
