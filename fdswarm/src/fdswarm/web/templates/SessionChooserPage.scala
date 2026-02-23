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

import scalatags.Text.all.*

object SessionChooserPage {
  def apply(sessions: Seq[(String, String)], message: String = ""): String =
    Layout("Choose or Create Web Session")(
      div(cls := "row mb-2")(
        div(cls := "col-12")(
          h6("Web Sessions"),
          if message.nonEmpty then div(cls := "alert alert-info py-1 my-1")(message) else div(),
          form(method := "POST", action := "/session/select")(
            div(cls := "mb-2")(
              label(cls := "form-label me-2")("Existing Sessions:"),
              select(name := "sessionId", cls := "form-select form-select-sm d-inline w-auto me-2")(
                option(value := "")("-- select --"),
                sessions.map { case (id, label) => option(value := id)(label) }
              ),
              button(tpe := "submit", cls := "btn btn-primary btn-sm ms-1")("Use Session")
            )
          ),
          hr(),
          h6("Create New Session"),
          form(method := "POST", action := "/session/create")(
            div(cls := "row g-2 align-items-end")(
              div(cls := "col-md-3")(
                label(cls := "form-label")("Rig"),
                input(tpe := "text", name := "rig", cls := "form-control")
              ),
              div(cls := "col-md-3")(
                label(cls := "form-label")("Antenna"),
                input(tpe := "text", name := "antenna", cls := "form-control")
              ),
              div(cls := "col-md-3")(
                label(cls := "form-label")("Operator"),
                input(tpe := "text", name := "operator", cls := "form-control", oninput := "toUpperCase(this)")
              ),
              div(cls := "col-md-3")(
                label(cls := "form-label")("QSO lines"),
                input(tpe := "number", min := 1, max := 200, name := "qsoLines", value := 10, cls := "form-control")
              ),
            ),
            div(cls := "row mt-2")(
              div(cls := "col-12")(
                button(tpe := "submit", cls := "btn btn-success btn-sm")("Create Session")
              )
            )
          )
        )
      )
    )
}
