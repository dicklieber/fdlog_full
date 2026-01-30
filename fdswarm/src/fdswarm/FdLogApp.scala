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

package fdswarm.fx

import com.google.inject.{Guice, Injector}
import com.typesafe.scalalogging.LazyLogging
import scalafx.application.JFXApp3

/** Minimal app bootstrap:
  *   - builds the Guice injector
  *   - runs startup validation checks
  *   - delegates all UI construction to [[FdLogUi]]
  */
object fdlog extends JFXApp3 with LazyLogging:

  logger.info("fdlog ctor")

  private lazy val injector: Injector =
    Guice.createInjector(new ConfigModule())

  override def start(): Unit =
    validateHamBands(injector)

    val ui = new FdLogUi(injector)
    stage = ui.primaryStage()

  private def validateHamBands(injector: Injector): Unit =
    val catalog = injector.getInstance(classOf[fdswarm.fx.bands.HamBandCatalog])
    val issues  = fdswarm.fx.bands.HamBandValidator.validate(catalog.all)
    issues.foreach { i =>
      logger.error(s"[HamBands/${i.kind}] ${i.message}")
    }
