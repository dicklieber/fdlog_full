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
import fdswarm.replication.NodeStatus
import javafx.application.Platform
import net.codingwell.scalaguice.InjectorExtensions.*
import scalafx.application.JFXApp3
import scalafx.scene.control.{Button, Label}

import scala.concurrent.duration.*

/** Minimal app bootstrap:
  *   - builds the Guice injector
  *   - runs startup validation checks
  *   - delegates all UI construction to [[FdLogUi]]
  */
object FdLogApp extends JFXApp3 with LazyLogging:

  logger.debug("fdlog ctor")
  
  private lazy val injector: Injector =
    Guice.createInjector(new ConfigModule())

  override def start(): Unit =


    // Create a Dispatcher so UI callbacks can safely "launch" IO
    val dispatcher: Dispatcher[IO] =
      Dispatcher.parallel[IO].allocated.unsafeRunSync()._1
    val label = new Label("Idle")
    val button = new Button("Run IO task")

    button.onAction = _ => {
      dispatcher.unsafeRunAndForget {
        for {
          _ <- IO.sleep(500.millis)
          _ <- IO {
            // Any UI mutation must happen on the FX thread
            Platform.runLater(() => label.text = s"Updated at ${java.time.LocalTime.now}")
          }
        } yield ()
      }
    }

    // IMPORTANT: FdLogUi is injected; ask Guice for it
    val nodeStatus = injector.instance[NodeStatus]
    val directoryProvider = injector.instance[fdswarm.io.DirectoryProvider]
    fdswarm.util.LoggingConfigurator.addFileAppender(directoryProvider)
    
    val ui = injector.instance[FdLogUi]
    // Start HTTP API service (http4s + tapir) in a background daemon thread
    val apiService = injector.instance[fdswarm.api.HttpApi]
    apiService.start()

    // Create the primary stage, let the UI configure it, then publish it
    val s = new JFXApp3.PrimaryStage
    ui.start(s)
    stage = s

  override def stopApp(): Unit =
    logger.debug("stopApp")
    val nodeStatus = injector.instance[NodeStatus]
    nodeStatus.stop()
  

  