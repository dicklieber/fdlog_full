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
            .section-group { margin-bottom: 1rem; padding: 0.5rem; background-color: #f4f4f4; border: 1px solid #ccc; border-radius: 5px; }
            .section-badge { margin-right: 0.2rem; cursor: pointer; }
            .band-mode-matrix table { width: auto; }
            .band-mode-matrix td, .band-mode-matrix th { padding: 0.2rem; text-align: center; }
            .selected-band-mode { font-weight: bold; background-color: #0d6efd; color: white; }
            .contest-timer { font-size: 1.2rem; font-weight: bold; padding: 0.5rem; border-radius: 5px; }
            .contest-before { background-color: #ffc107; }
            .contest-during { background-color: #198754; color: white; }
            .contest-after { background-color: #6c757d; color: white; }
          """.stripMargin
        )
      ),
      body(
        div(cls := "container-fluid mt-3")(
          content
        ),
        script(src := "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"),
        script(
          """
            function setSection(code) {
              document.getElementById('sectionInput').value = code;
            }
          """.stripMargin
        )
      )
    ).render

  def indexPage(
                 qsos: Seq[Qso],
                 bands: Seq[Band],
                 modes: Seq[Mode],
                 selectedBandMode: BandMode,
                 sectionGroups: Seq[SectionGroup],
                 contestTimerMsg: String,
                 contestTimerStyle: String,
                 stationInfo: String
               ): String =
    layout("Field Day Swarm Web Client")(
      div(cls := "row")(
        div(cls := "col-md-8")(
          h2("QSOs"),
          qsoTable(qsos)
        ),
        div(cls := "col-md-4")(
          div(cls := s"contest-timer $contestTimerStyle mb-3")(contestTimerMsg),
          div(cls := "card mb-3")(
            div(cls := "card-header")("Station Info"),
            div(cls := "card-body")(stationInfo)
          ),
          h3("QSO Entry"),
          qsoEntryForm(selectedBandMode),
          h3("Sections"),
          sectionsPanel(sectionGroups),
          h3("Band & Mode"),
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
      div(cls := "mb-3")(
        label(cls := "form-label")("Their Callsign"),
        input(tpe := "text", name := "callsign", cls := "form-control", autofocus := true, required := true)
      ),
      div(cls := "mb-3")(
        label(cls := "form-label")("Received Class"),
        input(tpe := "text", name := "contestClass", cls := "form-control", required := true)
      ),
      div(cls := "mb-3")(
        label(cls := "form-label")("Received Section"),
        input(tpe := "text", name := "section", id := "sectionInput", cls := "form-control", required := true)
      ),
      button(tpe := "submit", cls := "btn btn-primary")("Submit QSO"),
      span(cls := "ms-2")(s"Current: ${selectedBandMode.band} ${selectedBandMode.mode}")
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
