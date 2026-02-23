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
import scalatags.Text.all.*

object QsoEntryForm {
  def apply(selectedBandMode: BandMode): Modifier =
    form(method := "POST", action := "/web/qso")(
      div(cls := "row g-2 align-items-end")(
        div(cls := "col-md-3")(
          label(cls := "form-label")("Their Callsign"),
          input(tpe := "text", name := "callsign", id := "callsignInput", cls := "form-control", autofocus := true, required := true, oninput := "toUpperCase(this); fetchDups(this)")
        ),
        div(cls := "col-md-2")(
          label(cls := "form-label")("Class"),
          input(tpe := "text", name := "contestClass", cls := "form-control", required := true, oninput := "toUpperCase(this)")
        ),
        div(cls := "col-md-3")(
          label(cls := "form-label")("Section"),
          input(tpe := "text", name := "section", id := "sectionInput", cls := "form-control", required := true, oninput := "toUpperCase(this)")
        ),
        div(cls := "col-md-4")(
          button(tpe := "submit", cls := "btn btn-primary btn-sm")("Submit QSO"),
          span(cls := "ms-2", style := "font-size: 0.75rem")(s"Current: ${selectedBandMode.band} ${selectedBandMode.mode}")
        )
      )
    )
}
