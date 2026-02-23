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
import scalatags.Text.tags2.{style as styleTag}

object Layout {
  def apply(titleStr: String)(content: Modifier*): String =
    "<!DOCTYPE html>" + html(
      head(
        tag("title")(titleStr),
        link(rel := "stylesheet", href := "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css"),
        styleTag(
          """
            body { font-size: 0.85rem; }
            h2, h3, h4, h5, h6 { font-size: 1rem; font-weight: bold; margin-bottom: 0.25rem; }
            .section-group { margin-bottom: 0.25rem; padding: 0.25rem; background-color: #f4f4f4; border: 1px solid #ccc; border-radius: 3px; }
            .section-badge { margin-right: 0.1rem; cursor: pointer; padding: 0.1rem 0.3rem; font-size: 0.75rem; }
            .band-mode-matrix table { width: auto; margin-bottom: 0; }
            .band-mode-matrix td, .band-mode-matrix th { padding: 0.1rem; text-align: center; font-size: 0.75rem; }
            .selected-band-mode { font-weight: bold; background-color: #0d6efd; color: white; }
            .contest-timer { font-size: 0.9rem; font-weight: bold; padding: 0.25rem; border-radius: 3px; }
            .contest-before { background-color: #ffc107; }
            .contest-during { background-color: #198754; color: white; }
            .contest-after { background-color: #6c757d; color: white; }
            .card-header { padding: 0.25rem 0.5rem; font-size: 0.85rem; font-weight: bold; }
            .card-body { padding: 0.25rem 0.5rem; font-size: 0.8rem; }
            .form-label { margin-bottom: 0.1rem; font-size: 0.8rem; }
            .form-control { padding: 0.2rem 0.4rem; font-size: 0.8rem; }
            .btn-sm { padding: 0.1rem 0.3rem; font-size: 0.75rem; }
            .table-sm :not(caption) > * > * { padding: 0.1rem 0.25rem; }
            .dup-callsign { color: red; margin-right: 0.5rem; font-weight: bold; cursor: help; }
            .dup-callsign-more { color: #666; font-style: italic; font-size: 0.75rem; margin-top: 0.25rem; width: 100%; }
            .dup-error-msg { color: #dc3545; font-weight: bold; }
          """.stripMargin
        )
      ),
      body(
        div(cls := "container-fluid mt-1")(
          content
        ),
        script(src := "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"),
        script(src := "/web.js"),
        script(
          """
            function setSection(code) {
              document.getElementById('sectionInput').value = code;
            }
          """.stripMargin
        )
      )
    ).render
}
