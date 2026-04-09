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
import fdswarm.fx.FdLogUi
import scalafx.application.JFXApp3
import scalafx.stage.Stage

/** Minimal app bootstrap:
  *   - applies process startup settings
  *   - delegates all UI construction to [[FdLogUi]]
  */

object FdLogApp extends JFXApp3:
  private var ui: Option[FdLogUi] = None
  def primaryStage: Stage = stage
  private var rawArgs: Array[String] = Array.empty
  private lazy val injector: Injector = Guice.createInjector(
    new fdswarm.fx.ConfigModule(
      rawArgs
    )
  )

  override def main(
    args: Array[String]
  ): Unit =
    rawArgs = args
    println(
      s"Starting FdSwarm with args: ${args.mkString(
        " "
      )}"
    )
    System.setProperty(
      "apple.laf.useScreenMenuBar",
      "true"
    )
    if FdLogUi.isMac then
      System.setProperty(
        "apple.awt.application.name",
        "FdSwarm"
      )
      System.setProperty(
        "com.apple.mrj.application.apple.menu.about.name",
        "FdSwarm"
      )
      System.setProperty(
        "apple.awt.application.appearance",
        "system"
      )
    System.setProperty(
      "javafx.embed.singleThread",
      "true"
    )
    super.main(
      args
    )

  private val log = org.slf4j.LoggerFactory.getLogger(getClass)

  override def start(): Unit =
    val builtUi = injector.getInstance(
      classOf[FdLogUi]
    )
    ui = Some(builtUi)

    // Create and publish the stage before UI startup so cross-cutting code can read it.
    val s = new JFXApp3.PrimaryStage
    stage = s
    builtUi.start(s)

  override def stopApp(): Unit =
    log.debug("stopApp")
    ui.foreach(
      _.stopApp()
    )
