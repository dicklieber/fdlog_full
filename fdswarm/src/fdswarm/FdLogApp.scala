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
import com.typesafe.scalalogging.LazyLogging
import fdswarm.api.ApiService
import fdswarm.fx.{ConfigModule, FdLogUi}
import fdswarm.replication.NodeStatus
import net.codingwell.scalaguice.InjectorExtensions.*
import scalafx.application.JFXApp3

/** Minimal app bootstrap:
  *   - builds the Guice injector
  *   - runs startup validation checks
  *   - delegates all UI construction to [[FdLogUi]]
  */
object FdLogApp extends JFXApp3 with LazyLogging:

  logger.info("fdlog ctor")
  
  private lazy val injector: Injector =
    Guice.createInjector(new ConfigModule())

  override def start(): Unit =

    // IMPORTANT: FdLogUi is injected; ask Guice for it
    val nodeStatus = injector.instance[NodeStatus]
    val directoryProvider = injector.instance[fdswarm.io.DirectoryProvider]
    fdswarm.util.LoggingConfigurator.addFileAppender(directoryProvider)
    
    val ui = injector.instance[FdLogUi]
    val apiService = injector.instance[ApiService]

    // Start API service in a separate thread
    val apiThread = new Thread(() => apiService.start())
    apiThread.setDaemon(true)
    apiThread.start()

    // Create the primary stage, let the UI configure it, then publish it
    val s = new JFXApp3.PrimaryStage
    ui.start(s)
    stage = s

  override def stopApp(): Unit =
    logger.info("stopApp")
    val nodeStatus = injector.instance[NodeStatus]
    nodeStatus.stop()
  

  