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

import fdswarm.model.BandMode.{Band, Mode}
import fdswarm.model.{BandMode, Qso}
import fdswarm.fx.sections.{Section, SectionGroup}
import scalatags.Text.all.*
import scalatags.Text.tags2.{nav, style as styleTag}
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId, ZonedDateTime}
import fdswarm.util.DurationFormat
import java.time.{Duration => JDuration}

object WebTemplates:
  private val cookieName = "fdweb_session"

  private val timeFmt =
    DateTimeFormatter.ofPattern("MMM dd, h:mm a z")
      .withZone(ZoneId.systemDefault())

  def layout(titleStr: String)(content: Modifier*): String =
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

  def sessionChooserPage(sessions: Seq[(String, String)], message: String = ""): String =
    layout("Choose or Create Web Session")(
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

  def sessionEditPage(sessionId: String,
                      rig: String,
                      antenna: String,
                      operator: String,
                      qsoLines: Int,
                      stats: Option[(Int, String)]): String =
    layout(s"Edit Web Session $sessionId")(
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

  def indexPage(
                 qsos: Seq[Qso],
                 bands: Seq[Band],
                 modes: Seq[Mode],
                 selectedBandMode: BandMode,
                 sectionGroups: Seq[SectionGroup],
                 contestTimerMsg: String,
                 contestTimerStyle: String
               ): String =
    layout("Field Day Swarm Web Client")(
      div(cls := "row mb-2")(
        div(cls := "col-12 d-flex justify-content-between align-items-center")(
          h6("Field Day Swarm Web Client"),
          a(href := "/web/session/edit", cls := "btn btn-outline-primary btn-sm")("Edit Session")
        )
      ),
      // Row 0: QSOs table (full width)
      div(cls := "row mb-1")(
        div(cls := "col-12")(
          h6("QSOs"),
          qsoTable(qsos)
        )
      ),
      // Row 1: Left = QSO Entry; Right = Sections (spans alongside)
      div(cls := "row mb-1")(
        div(cls := "col-md-8 mb-1 mb-md-0")(
          h6("QSO Entry"),
          qsoEntryForm(selectedBandMode)
        ),
        div(cls := "col-md-4")(
          h6("Sections"),
          sectionsPanel(sectionGroups)
        )
      ),
      // Row 2: Contest timer under QSO Entry (left column in ScalaFX grid)
      div(cls := "row mb-1")(
        div(cls := "col-md-8 mb-1 mb-md-0")(
          div(cls := s"contest-timer $contestTimerStyle mb-1")(contestTimerMsg)
        ),
        // Keep right column empty here so Sections remain visually paired to the left
        div(cls := "col-md-4 d-none d-md-block")()
      ),
      // Row 3: Band & Mode matrix (full width)
      div(cls := "row")(
        div(cls := "col-12")(
          h6("Band & Mode"),
          bandModeMatrix(bands, modes, selectedBandMode)
        )
      )
    )

  def qsoTable(qsos: Seq[Qso]): Modifier =
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

  def qsoEntryForm(selectedBandMode: BandMode): Modifier =
    form(method := "POST", action := "/web/qso")(
      div(cls := "row g-2 align-items-end")(
        div(cls := "col-md-3")(
          label(cls := "form-label")("Their Callsign"),
          input(tpe := "text", name := "callsign", cls := "form-control", autofocus := true, required := true, oninput := "toUpperCase(this)")
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

  def sectionsPanel(groups: Seq[SectionGroup]): Modifier =
    div(
      groups.map { g =>
        div(cls := "section-group")(
          strong(g.name, ": "),
          g.sections.map { s =>
            span(cls := "badge bg-secondary section-badge", onclick := s"setSection('${s.code}')")(s.code)
          }
        )
      }
    )

  def bandModeMatrix(bands: Seq[Band], modes: Seq[Mode], selected: BandMode): Modifier =
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
