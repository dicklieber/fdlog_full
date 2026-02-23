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

package fdswarm.client

import com.google.inject.Guice
import fdswarm.fx.qso.ContestEntry
import javafx.application.Platform
import net.codingwell.scalaguice.InjectorExtensions.*
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.UserConfig

object FdSwarmClientApp extends JFXApp3 with LazyLogging:

  override def start(): Unit =
    val injector = Guice.createInjector(new ClientModule())
    
    // Set some default user config if needed, or just let it load
    val userConfig = injector.instance[UserConfig]
    
    val contestEntry = injector.instance[ContestEntry]
    val restQsoStore = injector.instance[RestQsoStore]

    // Initial fetch of last N QSOs as requested (configurable)
    val lastQsoCount = userConfig.get[Int]("lastQsoCount")
    restQsoStore.refreshLastQsos(lastQsoCount)

    stage = new JFXApp3.PrimaryStage {
      title = "Field Day Swarm Client"
      scene = new Scene {
        root = contestEntry.node.asInstanceOf[scalafx.scene.Parent]
      }
    }

    logger.info("FdSwarmClientApp started")

  override def stopApp(): Unit =
    logger.info("FdSwarmClientApp stopping")
