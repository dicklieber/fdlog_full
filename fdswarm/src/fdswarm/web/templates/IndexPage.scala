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

import fdswarm.model.BandMode.{Band, Mode}
import fdswarm.model.{BandMode, Qso}
import fdswarm.fx.sections.SectionGroup
import scalatags.Text.all.*

object IndexPage {
  def apply(
             qsos: Seq[Qso],
             bands: Seq[Band],
             modes: Seq[Mode],
             selectedBandMode: BandMode,
             sectionGroups: Seq[SectionGroup],
             contestTimerMsg: String,
             contestTimerStyle: String
           ): String =
    Layout("Field Day Swarm Web Client")(
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
          QsoTable(qsos)
        )
      ),
      // Row 1: Left = QSO Entry; Right = Sections (spans alongside)
      div(cls := "row mb-1")(
        div(cls := "col-md-8 mb-1 mb-md-0")(
          h6("QSO Entry"),
          QsoEntryForm(selectedBandMode),
          // New row for Dup Detection (hidden by default)
          div(cls := "row mt-1", id := "dupsRow", style := "display: none;")(
            div(cls := "col-12")(
              div(cls := "card")(
                div(cls := "card-header bg-warning text-dark")("Possible Dups"),
                div(cls := "card-body", id := "dupsContainer")()
              )
            )
          )
        ),
        div(cls := "col-md-4")(
          h6("Sections"),
          SectionsPanel(sectionGroups)
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
          BandModeMatrix(bands, modes, selectedBandMode)
        )
      )
    )
}
