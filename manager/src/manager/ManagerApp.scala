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

import com.google.inject.{Guice, Injector}
import fdswarm.logging.LazyStructuredLogging
import fdswarm.StartupConfig
import fdswarm.fx.bands.*
import fdswarm.model.{BandMode, Callsign}
import fdswarm.util.CallsignGenerator
import net.codingwell.scalaguice.InjectorExtensions.*
import scalafx.application.JFXApp3
import scalafx.application.Platform
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.layout.{BorderPane, HBox, StackPane}

import scala.collection.IndexedSeqView
import scala.util.Random

import scalafx.scene.paint.Color
import scalafx.scene.image.Image
import scalafx.stage.Stage
import javafx.embed.swing.SwingFXUtils

import java.util.concurrent.atomic.AtomicBoolean

/**
 * A development tool tha runs a bunch of instances of the FDSwarm application
 * on this host.
 */
object ManagerApp extends JFXApp3 with LazyStructuredLogging :

  private lazy val injector: Injector =
    Guice.createInjector(new ManagerModule())
  private lazy val runner: Runner = injector.instance[Runner]
  private val shutdownOnce = new AtomicBoolean(false)
  private val shutdownHook = new Thread("manager-shutdown-hook"):
    override def run(): Unit = stopManagedInstances()
  Runtime.getRuntime.addShutdownHook(shutdownHook)


  override def start(): Unit = {

    if !runner.verifyRequiredJar() then
      sys.exit(1)

    // Force all HF, VHF, and UHF bands and all modes to be available for selection in manager
    val bandCatalog = injector.instance[BandCatalog]
    val modeCatalog = injector.instance[ModeCatalog]
    val bandsManager = injector.instance[AvailableBandsManager]
    val modesManager = injector.instance[AvailableModesManager]

    val allRequiredBands = bandCatalog.hamBands
      .filter(b => b.bandClass == BandClass.HF || b.bandClass == BandClass.VHF || b.bandClass == BandClass.UHF)
      .map(_.bandName)
    bandsManager.bands.setAll(allRequiredBands*)
    modesManager.modes.setAll(modeCatalog.modes*)

    val nodeConfigManager = injector.instance[NodeConfigManager]

    stage = new JFXApp3.PrimaryStage {
      title = "Debug Configuration Manager"
      onCloseRequest = _ => shutdownAndExit()
      onHiding = _ => stopManagedInstances()
    }

    setAppIcon(stage)

    val nodeConfigGridPane = new NodeConfigGridPane(
      nodeConfigManager = nodeConfigManager,
      injector = injector,
      ownerStage = stage
    )

    val borderPane = new BorderPane {
      center = nodeConfigGridPane
      bottom = new HBox {
        spacing = 10
        children = Seq(
          new Button("Add") {
            onAction = _ => {
              val usedCallsigns = nodeConfigManager.observableBuffer.map(_.operator.value).toSet
              val generator = CallsignGenerator.callsignIterator("N0")
              val callsignStr = Iterator.continually(generator.next()).find(cs => !usedCallsigns.contains(cs)).get
              val callsign = Callsign(callsignStr)
              val bands = bandsManager.bands.toIndexedSeq
              val modes = modesManager.modes.toIndexedSeq
              val band = bands(Random.nextInt(bands.length))
              val mode = modes(Random.nextInt(modes.length))
              val bandModeStr = s"$band $mode"
              val bandMode = BandMode(bandModeStr)
              nodeConfigManager.add(StartupConfig(operator = callsign, bandMode = bandMode))
            }
          },
          new Button("Start All") {
            onAction = _ => {
              val view: IndexedSeqView[StartupConfig] = nodeConfigManager.observableBuffer.view
              runner.start(view)
            }
          },
          new Button("Stop All") {
            onAction = _ => runner.stop()
          }
        )
      }
    }

    stage.scene = new Scene {
      root = borderPane
    }
  }

  override def stopApp(): Unit =
    stopManagedInstances()

  private def shutdownAndExit(): Unit =
    stopManagedInstances()
    Platform.exit()
    System.exit(0)

  private def stopManagedInstances(): Unit =
    if shutdownOnce.compareAndSet(false, true) then
      try
        logger.info("Stopping managed instances")
        runner.stop()
      catch
        case e: Exception =>
          logger.warn("Failed to stop managed instances during shutdown")

  private def setAppIcon(stage: Stage): Unit = {
      try {
        val resource = getClass.getResource("/managerAppIcon.png")
        if resource != null then {
          val iconImage = new Image(resource.toExternalForm)
          stage.getIcons.add(iconImage)
          try {
            if java.awt.Taskbar.isTaskbarSupported then {
              val taskbar = java.awt.Taskbar.getTaskbar
              if taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE) then {
                val bufferedImage = SwingFXUtils.fromFXImage(iconImage, null)
                taskbar.setIconImage(bufferedImage)
                logger.debug("Successfully set manager app icon via Taskbar")
              }
            }
          } catch {
            case e: Exception =>
              logger.error("Could not set app icon via Taskbar (normal on some platforms)", e)
          }
        }
      } catch {
        case e: Exception => logger.error("Could not set manager application icon", e)
      }
    }
