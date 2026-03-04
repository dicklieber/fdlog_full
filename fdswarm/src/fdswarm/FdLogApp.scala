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

package fdswarm
import cats.effect.IO
import cats.effect.std.Dispatcher
import cats.effect.unsafe.implicits.global
import com.google.inject.{Guice, Injector}
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.{ConfigModule, FdLogUi}
import fdswarm.replication.{NodeStatusHandler, StatusBroadcastService}
import javafx.application.Platform
import net.codingwell.scalaguice.InjectorExtensions.*
import scalafx.application.JFXApp3
import scalafx.scene.control.{Button, Label}

import java.time.Instant
import java.time.Duration

/** Minimal app bootstrap:
  *   - builds the Guice injector
  *   - runs startup validation checks
  *   - delegates all UI construction to [[FdLogUi]]
  */
object FdLogApp extends JFXApp3:
  private val startTime = Instant.now()
  override def main(args: Array[String]): Unit =
    System.setProperty("apple.laf.useScreenMenuBar", "true")
    if (System.getProperty("os.name").toLowerCase.contains("mac")) {
      System.setProperty("apple.awt.application.name", "FdSwarm")
      System.setProperty("com.apple.mrj.application.apple.menu.about.name", "FdSwarm")
      // Some versions of Java/JavaFX also look for this
      System.setProperty("apple.awt.application.appearance", "system")
    }
    System.setProperty("javafx.embed.singleThread", "true")
    super.main(args)

  private val log = org.slf4j.LoggerFactory.getLogger(getClass)

  lazy val startupDuration: Duration = Duration.between(startTime, Instant.now())

  private lazy val injector: Injector =
    Guice.createInjector(new ConfigModule())
  var statusBroadcastService: Option[StatusBroadcastService] = None

  override def start(): Unit =
//    statusBroadcastService = Option(injector.instance[StatusBroadcastService])
//    injector.instance[NodeStatusHandler]

    log.debug("fdlog start")
    
    val ui = injector.instance[FdLogUi]
    // Start HTTP API service (http4s + tapir) in a background daemon thread
    val apiService = injector.instance[fdswarm.api.HttpApi]
    apiService.start()

    // Create the primary stage, let the UI configure it, then publish it
    val s = new JFXApp3.PrimaryStage
    ui.start(s)
    stage = s

  override def stopApp(): Unit =
    log.debug("stopApp")
    statusBroadcastService.foreach(_.stop())


  