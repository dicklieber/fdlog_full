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

import fdswarm.fx.FdLogUi
import scalafx.application.JFXApp3

/** Minimal app bootstrap:
  *   - applies process startup settings
  *   - delegates all UI construction to [[FdLogUi]]
  */

object FdLogApp extends JFXApp3:
  private var ui: Option[FdLogUi] = None

  override def main(
    args: Array[String]
  ): Unit =
    println(
      s"Starting FdSwarm with args: ${args.mkString(
        " "
      )}"
    )
    FdLogUi.initialize(
      args
    )
    super.main(
      args
    )

  private val log = org.slf4j.LoggerFactory.getLogger(getClass)

  override def start(): Unit =
    val builtUi = FdLogUi.build()
    ui = Some(builtUi)

    // Create the primary stage, let the UI configure it, then publish it
    val s = new JFXApp3.PrimaryStage
    builtUi.start(s)
    stage = s

  override def stopApp(): Unit =
    log.debug("stopApp")
    ui.foreach(
      _.stopApp()
    )
