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
import com.typesafe.scalalogging.LazyLogging
import fdswarm.StartupConfig
import fdswarm.fx.bands.*
import fdswarm.model.{BandMode, Callsign}
import fdswarm.util.CallsignGenerator
import net.codingwell.scalaguice.InjectorExtensions.*
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.layout.{BorderPane, HBox}

import scala.collection.IndexedSeqView
import scala.util.Random

/**
 * A development tool tha runs a bunch of instances of the FDSwarm application
 * on this host.
 */
object ManagerApp extends JFXApp3 with LazyLogging :

  private lazy val injector: Injector =
    Guice.createInjector(new ManagerModule())


  override def start(): Unit = {

    val runner = injector.instance[Runner]

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
      //      onCloseRequest = _ => {
      //        injector.instance[Runner].stopAll()
      //      }
      scene = new Scene {
        root = new BorderPane {
          center = new NodeConfigGridPane(
            nodeConfigManager = nodeConfigManager,
            injector = injector,
            ownerWindow = stage
          )
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
              new Button("Save") {
                onAction = _ => {
                  nodeConfigManager.persist()
                  logger.info("Changes saved to nodes.json")
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
      }
    }

  }
