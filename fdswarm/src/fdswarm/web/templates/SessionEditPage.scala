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

object SessionEditPage {
  def apply(sessionId: String,
            rig: String,
            antenna: String,
            operator: String,
            qsoLines: Int,
            stats: Option[(Int, String)]): String =
    Layout(s"Edit Web Session $sessionId")(
      div(cls := "row mb-2")(
        div(cls := "col-12 d-flex justify-content-between align-items-center")(
          h6(s"Editing Session: $sessionId"),
          a(href := "/web", cls := "btn btn-outline-success btn-sm")("Go to QSO Entry")
        )
      ),
      div(cls := "row mb-1")(
        div(cls := "col-12")(
          stats.map { case (n, last) =>
            div(cls := "small text-muted")(
              span(cls := "me-3")(s"QSOs entered: $n"),
              span(cls := "me-3")(s"Last touched: $last")
            )
          }.getOrElse(div())
        )
      ),
      div(cls := "row mb-1")(
        div(cls := "col-12")(
          h6("Station Settings"),
          form(method := "POST", action := "/web/session/save")(
            input(tpe := "hidden", name := "sessionId", value := sessionId),
            table(cls := "table table-borderless table-sm w-auto")(
              tbody(
                tr(
                  td(label(cls := "form-label")("Operator")),
                  td(input(tpe := "text", name := "operator", value := operator, cls := "form-control", oninput := "toUpperCase(this)"))
                ),
                tr(
                  td(label(cls := "form-label")("Rig")),
                  td(input(tpe := "text", name := "rig", value := rig, cls := "form-control"))
                ),
                tr(
                  td(label(cls := "form-label")("Antenna")),
                  td(input(tpe := "text", name := "antenna", value := antenna, cls := "form-control"))
                ),
                tr(
                  td(label(cls := "form-label")("QSO lines")),
                  td(input(tpe := "number", min := 1, max := 200, name := "qsoLines", value := qsoLines, cls := "form-control"))
                )
              )
            ),
            div(cls := "row mt-2")(
              div(cls := "col-12")(
                button(tpe := "submit", cls := "btn btn-primary btn-sm")("Save Session")
              )
            )
          )
        )
      )
    )
}
