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

import com.typesafe.scalalogging.LazyLogging
import fdswarm.io.DirectoryProvider
import fdswarm.util.Ids.Id
import io.circe.parser.decode
import io.circe.syntax.*
import jakarta.inject.{Inject, Singleton}
import scalafx.collections.ObservableMap
import java.time.ZonedDateTime

import scala.collection.concurrent.TrieMap

@Singleton
class WebSessionStore @Inject()(directoryProvider: DirectoryProvider) extends LazyLogging:
  private val sessionsFile = directoryProvider() / "web-sessions.json"
  private val statsFile = directoryProvider() / "web-session-stats.json"

  // We use ObservableMap for easy integration with JavaFX UI if needed
  val sessions: ObservableMap[Id, WebSession] = ObservableMap.from(Map.empty[Id, WebSession])
  val stats: ObservableMap[Id, SessionStats] = ObservableMap.from(Map.empty[Id, SessionStats])

  // Load existing sessions and stats from disk
  load()

  def getSession(id: Id): Option[WebSession] = sessions.get(id)

  def saveSession(session: WebSession): Unit =
    sessions.update(session.sessionId, session)
    // Update last touched in stats
    val currentStats = stats.getOrElse(session.sessionId, SessionStats())
    stats.update(session.sessionId, currentStats.copy(lastTouched = ZonedDateTime.now()))
    persist()

  def incrementQsoCount(id: Id): Unit =
    stats.get(id) match
      case Some(s) => 
        stats.update(id, s.copy(qsosEntered = s.qsosEntered + 1, lastTouched = ZonedDateTime.now()))
        persist()
      case None =>
        stats.update(id, SessionStats(qsosEntered = 1))
        persist()

  def deleteSession(id: Id): Unit =
    sessions.remove(id)
    stats.remove(id)
    persist()

  def allSessions: Seq[WebSession] = sessions.values.toSeq
  
  def getStats(id: Id): Option[SessionStats] = stats.get(id)

  private def persist(): Unit =
    try
      os.write.over(sessionsFile, sessions.toMap.asJson.spaces2, createFolders = true)
      os.write.over(statsFile, stats.toMap.asJson.spaces2, createFolders = true)
    catch
      case e: Exception => logger.error("Failed to persist web sessions or stats", e)

  private def load(): Unit =
    try
      if os.exists(sessionsFile) then
        decode[Map[Id, WebSession]](os.read(sessionsFile)) match
          case Right(m) => m.foreach { (id, session) => sessions.update(id, session) }
          case Left(e) => logger.error(s"Failed to decode web sessions: $e")
      
      if os.exists(statsFile) then
        decode[Map[Id, SessionStats]](os.read(statsFile)) match
          case Right(m) => m.foreach { (id, s) => stats.update(id, s) }
          case Left(e) => logger.error(s"Failed to decode web session stats: $e")
    catch
      case e: Exception => logger.error("Failed to load web sessions or stats", e)
