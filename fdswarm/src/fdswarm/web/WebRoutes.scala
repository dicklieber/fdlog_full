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

import cats.effect.IO
import fdswarm.StationManager
import fdswarm.fx.bandmodes.SelectedBandModeStore
import fdswarm.fx.bands.{AvailableBandsManager, AvailableModesManager, BandModeBuilder}
import fdswarm.fx.contest.ContestManager
import fdswarm.fx.qso.ContestTimerPanel
import fdswarm.fx.sections.SectionsProvider
import fdswarm.model.{Callsign, Qso, QsoMetadata}
import fdswarm.replication.{MulticastTransport, Service}
import fdswarm.store.QsoStore
import io.circe.syntax.*
import jakarta.inject.{Inject, Singleton}
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.{`Content-Type`, Location}
import org.http4s.server.Router
import fdswarm.api.ApiEndpoints
import sttp.tapir.server.ServerEndpoint
import com.typesafe.scalalogging.LazyLogging
import java.time.ZonedDateTime
import fdswarm.util.DurationFormat
import java.time.{Duration => JDuration}

@Singleton
class WebRoutes @Inject()(
                           qsoStore: QsoStore,
                           availableBandsManager: AvailableBandsManager,
                           availableModesManager: AvailableModesManager,
                           selectedBandModeStore: SelectedBandModeStore,
                           sectionsProvider: SectionsProvider,
                           contestManager: ContestManager,
                           stationManager: StationManager,
                           multicastTransport: MulticastTransport,
                           bandModeBuilder: BandModeBuilder
                         ) extends ApiEndpoints with LazyLogging:

  override def endpoints: List[ServerEndpoint[Any, IO]] = Nil

  def routes: HttpRoutes[IO] = Router("/web" -> httpRoutes)

  private def httpRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root =>
      val qsos = qsoStore.all.reverse // Latest first
      val bands = availableBandsManager.bands.toSeq
      val modes = availableModesManager.modes.toSeq
      val selected = selectedBandModeStore.selected.value
      val groups = sectionsProvider.sectionGroups
      
      val now = ZonedDateTime.now()
      val config = contestManager.config
      val (msg, style) = if now.isBefore(config.start) then
        (s"${config.contest.name} ${config.start.getYear} starts in ${DurationFormat(JDuration.between(now, config.start))}", "contest-before")
      else if now.isAfter(config.end) then
        (s"${config.contest.name} ${config.start.getYear} ended ${DurationFormat(JDuration.between(config.end, now))} ago.", "contest-after")
      else
        (s"${config.contest.name} ${config.start.getYear} ends in ${DurationFormat(JDuration.between(now, config.end))}", "contest-during")

      val stationInfo = s"Rig: ${stationManager.station.rig}, Ant: ${stationManager.station.antenna}, Op: ${stationManager.station.operator.value}"

      val html = WebTemplates.indexPage(
        qsos, bands, modes, selected, groups, msg, style, stationInfo
      )
      Ok(html).map(_.withContentType(`Content-Type`(MediaType.text.html)))

    case req @ POST -> Root / "qso" =>
      req.decode[UrlForm] { form =>
        val callsign = form.getFirstOrElse("callsign", "")
        val contestClass = form.getFirstOrElse("contestClass", "")
        val section = form.getFirstOrElse("section", "")

        if callsign.nonEmpty && contestClass.nonEmpty && section.nonEmpty then
          val metadata = QsoMetadata(
            station = stationManager.station,
            node = "local-web",
            contest = contestManager.config.contest
          )
          val qso = Qso(
            Callsign(callsign.toUpperCase),
            contestClass.toUpperCase,
            section.toUpperCase,
            selectedBandModeStore.selected.value,
            metadata
          )
          qsoStore.add(qso)
          multicastTransport.send(Service.QSO, qso.asJson.noSpaces.getBytes("UTF-8"))
          SeeOther(Location(Uri.unsafeFromString("/web")))
        else
          BadRequest("Missing fields")
      }

    case req @ POST -> Root / "select-band-mode" =>
      req.decode[UrlForm] { form =>
        val band = form.getFirstOrElse("band", "")
        val mode = form.getFirstOrElse("mode", "")
        if band.nonEmpty && mode.nonEmpty then
          val bm = bandModeBuilder(band, mode)
          selectedBandModeStore.save(bm)
          SeeOther(Location(Uri.unsafeFromString("/web")))
        else
          BadRequest("Missing band or mode")
      }
  }
