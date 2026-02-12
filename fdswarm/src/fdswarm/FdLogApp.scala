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
import fdswarm.replication.NetworkConfig
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
    val networkConfig = injector.getInstance(classOf[NetworkConfig])
    val ui = injector.getInstance(classOf[FdLogUi])
    val apiService = injector.getInstance(classOf[ApiService])
    // Start API service in a separate thread
    val apiThread = new Thread(() => apiService.start())
    apiThread.setDaemon(true)
    apiThread.start()

    // Create the primary stage, let the UI configure it, then publish it
    val s = new JFXApp3.PrimaryStage
    ui.start(s)
    stage = s
//todo do we need this?
//  private def validateHamBands(injector: Injector): Unit =
//    val hamBandCatalogx = injector.getInstance(classOf[fdswarm.fx.bands.HamBandCatalog])
//    val issues  = fdswarm.fx.bands.HamBandValidator.validate(hamBandCatalog.all)
//    issues.foreach { i =>
//      logger.error(s"[HamBands/${i.kind}] ${i.message}")
//    }

  /** Keep this validation non-invasive:
    * just verify we can construct the store (which implies config + dir wiring is OK).
    * Do NOT call any store API here (names differ between iterations).
    */
  private def validateBandModes(injector: Injector): Unit =
    try
      injector.getInstance(classOf[fdswarm.fx.bandmodes.BandModeStore])
      logger.info("BandModeStore constructed OK")
    catch
      case t: Throwable =>
        logger.warn("BandModeStore not available at startup", t)