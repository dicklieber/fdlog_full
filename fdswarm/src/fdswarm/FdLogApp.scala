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

import com.google.inject.{Guice, Injector}
import fdswarm.fx.{ConfigModule, FdLogUi}
import fdswarm.replication.StatusBroadcastService
import mainargs.{ParserForClass, arg}
import net.codingwell.scalaguice.InjectorExtensions.*
import scalafx.application.JFXApp3
import fdswarm.DebugConfig

import java.time.{Duration, Instant}

/** Minimal app bootstrap:
  *   - builds the Guice injector
  *   - runs startup validation checks
  *   - delegates all UI construction to [[FdLogUi]]
  */ 

case class StartupArgs(
  @arg(name = "startupInfo")
  startupInfo: Option[String] = None
)

object FdLogApp extends JFXApp3:
  private val startTime = Instant.now()
  private var rawArgs: Array[String] = Array.empty

  override def main(args: Array[String]): Unit =
    rawArgs = args
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

  private lazy val injector: Injector = Guice.createInjector(new ConfigModule(rawArgs))
  

  var statusBroadcastService: Option[StatusBroadcastService] = None

  override def start(): Unit =
    val ui = injector.instance[FdLogUi]


    // Create the primary stage, let the UI configure it, then publish it
    val s = new JFXApp3.PrimaryStage
    ui.start(s)
    stage = s

  override def stopApp(): Unit =
    log.debug("stopApp")
    statusBroadcastService.foreach(_.stop())


