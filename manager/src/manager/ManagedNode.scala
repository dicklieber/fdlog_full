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

package manager

import com.typesafe.scalalogging.LazyLogging
import fdswarm.StartupConfig
import fdswarm.io.DirectoryProvider
import _root_.io.circe.Printer
import _root_.io.circe.generic.auto.*
import _root_.io.circe.syntax.*

class ManagedNode(config: StartupConfig,
                  directoryProvider: DirectoryProvider,
                  jarPath: os.Path) extends LazyLogging:
  private var process: Option[os.SubProcess] = None
  private val printer = Printer.spaces2.copy(dropNullValues = true)

  def start(): Unit =
    logger.info(s"Starting node: ${config.id}")
    val nodesDir = directoryProvider() / "nodes"
    if !os.exists(nodesDir) then os.makeDir.all(nodesDir)
    
    val configFile = nodesDir / s"${config.id}.json"
    val json = printer.print(config.asJson)
    os.write.over(configFile, json)
    
    if !os.exists(jarPath) then
      logger.error(s"fdswarm.jar not found at $jarPath")
      return

    val proc = os.proc("java", "-jar", jarPath.toString(), s"startupConfig=$configFile")
      .spawn(stdout = os.Inherit, stderr = os.Inherit)
    
    process = Some(proc)
    logger.info(s"Started node: ${config.id} with PID: ${proc.wrapped.pid()}")

  def stop(): Unit =
    logger.info(s"Stopping node: ${config.id}")
    process.foreach { proc =>
      if proc.isAlive() then
        logger.info(s"Killing process for node: ${config.id} (PID: ${proc.wrapped.pid()})")
        proc.destroy()
        proc.waitFor(5000)
        if proc.isAlive() then
          logger.warn(s"Process for node: ${config.id} still alive, destroying forcibly")
          proc.destroy(shutdownGracePeriod = 0)
      process = None
    }
  
