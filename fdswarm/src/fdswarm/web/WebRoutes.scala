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
import fdswarm.fx.UserConfig
import fdswarm.fx.bandmodes.SelectedBandModeStore
import fdswarm.fx.bands.{AvailableBandsManager, AvailableModesManager, BandModeBuilder}
import fdswarm.fx.contest.ContestManager
import fdswarm.fx.qso.ContestTimerPanel
import fdswarm.fx.sections.SectionsProvider
import fdswarm.model.{Callsign, Qso, QsoMetadata}
import fdswarm.replication.{MulticastTransport, Service}
import fdswarm.store.QsoStore
import fdswarm.web.templates.*
import io.circe.syntax.*
import jakarta.inject.{Inject, Singleton}
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.{Cookie as HCookie, `Content-Type`, Location}
import org.http4s.server.Router
import org.http4s.ResponseCookie
import fdswarm.api.ApiEndpoints
import sttp.tapir.server.ServerEndpoint
import com.typesafe.scalalogging.LazyLogging
import java.time.ZonedDateTime
import fdswarm.util.DurationFormat
import java.time.{Duration => JDuration}
import cats.syntax.all.*

@Singleton
class WebRoutes @Inject()(
                           qsoStore: QsoStore,
                           availableBandsManager: AvailableBandsManager,
                           availableModesManager: AvailableModesManager,
                           selectedBandModeStore: SelectedBandModeStore,
                           sectionsProvider: SectionsProvider,
                           contestManager: ContestManager,
                           stationManager: StationManager,
                           userConfig: UserConfig,
                           multicastTransport: MulticastTransport,
                           bandModeBuilder: BandModeBuilder,
                           webSessionStore: WebSessionStore
                         ) extends ApiEndpoints with LazyLogging:

  override def endpoints: List[ServerEndpoint[Any, IO]] = Nil

  private val cookieName = "fdweb_session"

  def routes: HttpRoutes[IO] = rootRoutes <+> Router("/web" -> webAppRoutes) <+> staticRoutes

  private def staticRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root / "web.js" =>
      val path = "/fdswarm/web/web.js"
      val inputStream = getClass.getResourceAsStream(path)
      if (inputStream != null) {
        val content = scala.io.Source.fromInputStream(inputStream).mkString
        Ok(content).map(_.withContentType(`Content-Type`(MediaType.application.javascript)))
      } else {
        NotFound()
      }
  }

  private def getSessionIdCookie(req: Request[IO]): Option[String] =
    req.headers.get[HCookie].flatMap(_.values.find(_.name == cookieName).map(_.content))

  private def requireSession(req: Request[IO]): IO[Either[Response[IO], (String, WebSession)]] =
    getSessionIdCookie(req) match
      case Some(id) =>
        webSessionStore.getSession(id) match
          case Some(ws) => IO.pure(Right((id, ws)))
          case None =>
            val chooser = SessionChooserPage(webSessionStore.allSessions.map(s => s.sessionId -> s.station.operator.value),
              message = "Session not found. Please select or create one.")
            Ok(chooser).map(_.withContentType(`Content-Type`(MediaType.text.html))).map(Left(_))
      case None =>
        val chooser = SessionChooserPage(webSessionStore.allSessions.map(s => s.sessionId -> s.station.operator.value))
        Ok(chooser).map(_.withContentType(`Content-Type`(MediaType.text.html))).map(Left(_))

  private def rootRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root =>
      // Always show chooser on root as per requirement
      val chooser = SessionChooserPage(webSessionStore.allSessions.map(s => s.sessionId -> s.station.operator.value))
      Ok(chooser).map(_.withContentType(`Content-Type`(MediaType.text.html)))

    case req @ POST -> Root / "session" / "create" =>
      req.decode[UrlForm] { form =>
        val rig = form.getFirstOrElse("rig", "")
        val antenna = form.getFirstOrElse("antenna", "")
        val operator = form.getFirstOrElse("operator", "")
        val qsoLines = form.getFirstOrElse("qsoLines", "10").toIntOption.getOrElse(10)
        val defaultBm = selectedBandModeStore.selected.value
        val id = fdswarm.util.Ids.generateId()
        val ws = WebSession(
          station = fdswarm.model.Station(rig, antenna, fdswarm.model.Callsign(operator.toUpperCase)),
          bandMode = defaultBm,
          qsoLines = qsoLines,
          sessionId = id
        )
        webSessionStore.saveSession(ws)
        val cookie = ResponseCookie(name = cookieName, content = id, path = Some("/"))
        SeeOther(Location(Uri.unsafeFromString("/web"))).map(_.addCookie(cookie))
      }

    case req @ POST -> Root / "session" / "select" =>
      req.decode[UrlForm] { form =>
        val id = form.getFirstOrElse("sessionId", "")
        if id.nonEmpty && webSessionStore.getSession(id).nonEmpty then
          val cookie = ResponseCookie(name = cookieName, content = id, path = Some("/"))
          SeeOther(Location(Uri.unsafeFromString("/web"))).map(_.addCookie(cookie))
        else
          BadRequest("Invalid session selected")
      }

    case req @ GET -> Root / "web" / "session" / "edit" =>
      requireSession(req).flatMap {
        case Left(r) => IO.pure(r)
        case Right((id, ws)) =>
          val stats = webSessionStore.getStats(id).map(s => (s.qsosEntered, s.lastTouched.toString))
          val page = SessionEditPage(id, ws.station.rig, ws.station.antenna, ws.station.operator.value, ws.qsoLines, stats)
          Ok(page).map(_.withContentType(`Content-Type`(MediaType.text.html)))
      }

    case req @ POST -> Root / "web" / "session" / "save" =>
      req.decode[UrlForm] { form =>
        val sessionId = form.getFirstOrElse("sessionId", "")
        webSessionStore.getSession(sessionId) match
          case Some(ws) =>
            val rig = form.getFirstOrElse("rig", ws.station.rig)
            val antenna = form.getFirstOrElse("antenna", ws.station.antenna)
            val operator = form.getFirstOrElse("operator", ws.station.operator.value)
            val qsoLines = form.getFirstOrElse("qsoLines", ws.qsoLines.toString).toIntOption.getOrElse(ws.qsoLines)
            val updated = ws.copy(station = ws.station.copy(rig = rig, antenna = antenna, operator = fdswarm.model.Callsign(operator.toUpperCase)), qsoLines = qsoLines)
            webSessionStore.saveSession(updated)
            SeeOther(Location(Uri.unsafeFromString("/web/session/edit")))
          case None => BadRequest("Session not found")
      }
  }

  private def webAppRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root =>
      requireSession(req).flatMap {
        case Left(r) => IO.pure(r)
        case Right((id, ws)) =>
          val errorMsg = req.params.get("error")
          val limit = ws.qsoLines
          val qsos = qsoStore.all.reverse.take(limit)
          val bands = availableBandsManager.bands.toSeq
          val modes = availableModesManager.modes.toSeq
          val selected = ws.bandMode
          val groups = sectionsProvider.sectionGroups

          val now = ZonedDateTime.now()
          val config = contestManager.config
          val (msg, style) = if now.isBefore(config.start) then
            (s"${config.contest.name} ${config.start.getYear} starts in ${DurationFormat(JDuration.between(now, config.start))}", "contest-before")
          else if now.isAfter(config.end) then
            (s"${config.contest.name} ${config.start.getYear} ended ${DurationFormat(JDuration.between(config.end, now))} ago.", "contest-after")
          else
            (s"${config.contest.name} ${config.start.getYear} ends in ${DurationFormat(JDuration.between(now, config.end))}", "contest-during")

          val html = IndexPage(
            qsos, bands, modes, selected, groups, msg, style, errorMsg
          )
          Ok(html).map(_.withContentType(`Content-Type`(MediaType.text.html)))
      }

    case req @ GET -> Root / "dups" =>
      requireSession(req).flatMap {
        case Left(r) => IO.pure(r)
        case Right((id, ws)) =>
          val qsoPart = req.params.getOrElse("qsoPart", "").toUpperCase
          if (qsoPart.length >= 2) {
            val dupInfo = qsoStore.potentialDups(qsoPart, ws.bandMode)
            val displayedDups = dupInfo.firstNDups.map { callsign =>
              DupEntry(callsign.value, "")
            }
            val html = DupsPanel(displayedDups, dupInfo.totalDups).toString()
            Ok(html).map(_.withContentType(`Content-Type`(MediaType.text.html)))
          } else {
            Ok("<div></div>").map(_.withContentType(`Content-Type`(MediaType.text.html)))
          }
      }

    case req @ POST -> Root / "qso" =>
      requireSession(req).flatMap {
        case Left(r) => IO.pure(r)
        case Right((id, ws)) =>
          req.decode[UrlForm] { form =>
            val callsign = form.getFirstOrElse("callsign", "")
            val contestClass = form.getFirstOrElse("contestClass", "")
            val section = form.getFirstOrElse("section", "")

            if callsign.nonEmpty && contestClass.nonEmpty && section.nonEmpty then
              val metadata = QsoMetadata(
                station = ws.station,
                node = "local-web",
                contest = contestManager.config.contest
              )
              val qso = Qso(
                callsign = Callsign(callsign.toUpperCase),
                contestClass = contestClass.toUpperCase,
                section = section.toUpperCase,
                bandMode = ws.bandMode,
                qsoMetadata = metadata
              )
              val styledMessage = qsoStore.add(qso)
              if styledMessage.css == "duplicate-qso" then
                val msg = styledMessage.text
                SeeOther(Location(Uri.unsafeFromString(s"/web?error=${java.net.URLEncoder.encode(msg, "UTF-8")}")))
              else
                webSessionStore.incrementQsoCount(id)
                SeeOther(Location(Uri.unsafeFromString("/web")))
            else
              BadRequest("Missing fields")
          }
      }

    case req @ POST -> Root / "select-band-mode" =>
      requireSession(req).flatMap {
        case Left(r) => IO.pure(r)
        case Right((id, ws)) =>
          req.decode[UrlForm] { form =>
            val band = form.getFirstOrElse("band", "")
            val mode = form.getFirstOrElse("mode", "")
            if band.nonEmpty && mode.nonEmpty then
              val bm = bandModeBuilder(band, mode)
              webSessionStore.saveSession(ws.copy(bandMode = bm))
              SeeOther(Location(Uri.unsafeFromString("/web")))
            else
              BadRequest("Missing band or mode")
          }
      }
  }
