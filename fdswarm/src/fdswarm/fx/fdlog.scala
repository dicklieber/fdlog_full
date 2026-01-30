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
import fdswarm.StationManager
import fdswarm.fx.bands.AvailableBandsStore
import scalafx.Includes.*
import scalafx.application.{JFXApp3, Platform}
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.input.{KeyCode, KeyCombination, KeyEvent}
import scalafx.scene.layout.*

object fdlog extends JFXApp3 with LazyLogging:
  logger.info("fdlog ctor")
  private val injector: Injector = Guice.createInjector(new ConfigModule())

  private val qsoEntryPanel: QsoEntryPanel = injector.getInstance(classOf[QsoEntryPanel])

  override def start(): Unit =
    val catalog = injector.getInstance(classOf[fdswarm.fx.bands.HamBandCatalog])
    val issues  = fdswarm.fx.bands.HamBandValidator.validate(catalog.all)
    issues.foreach { i =>
      // swap for your logger if you prefer
//      System.err.println(s"[HamBands/${i.kind}] ${i.message}")
      logger.error(s"[HamBands/${i.kind}] ${i.message}")
    }
        
    
    stage = new JFXApp3.PrimaryStage {
      title = "FDLog (ScalaFX)"
      width = 1100
      height = 650

      scene = new Scene {
        private val stationManager: StationManager =
          injector.getInstance(classOf[StationManager])
        private val availableBandsStore = injector.getInstance(classOf[AvailableBandsStore])

        // Load station from disk (or defaults if missing)
        //        private val initial: Station =
        //          stationManager.load()

        //        private val caseForm = MyCaseForm(initial)
        // --- Build the two "views" we want to switch between ---


        private val qsoEntryPane: Pane =
          new VBox {
            padding = Insets(8)
            spacing = 4
            children = Seq(
              new Label("QSOs") {
                style = "-fx-font-size: 16px; -fx-font-weight: bold;"
              },
              qsoEntryPanel()
            )
          }

        // --- Root layout we can swap the center of ---
        private val rootPane = new BorderPane

        // --- MenuBar with proper toggle behavior ---
        private val viewToggles = new ToggleGroup

        private val stationItem = new RadioMenuItem("Station") {
          toggleGroup = viewToggles
          selected = true
          accelerator = KeyCombination.keyCombination("Shortcut+1")
          onAction = _ => rootPane.center = stationManager.pane()
        }

        private val availableBands = new RadioMenuItem("Available Bands") {
          toggleGroup = viewToggles
          accelerator = KeyCombination.keyCombination("Shortcut+2")
          onAction = _ => rootPane.center = availableBandsStore.availableBandsPane
        }
        private val qsoEntryItem = new RadioMenuItem("QSO Entry") {
          toggleGroup = viewToggles
          accelerator = KeyCombination.keyCombination("Shortcut+2")
          onAction = _ => rootPane.center = qsoEntryPane
        }

        private val menuBar = new MenuBar {
          menus = List(
            new Menu("File") {
              items = List(
                new MenuItem("Exit") {
                  accelerator = KeyCombination.keyCombination("Shortcut+Q")
                  onAction = _ => Platform.exit()
                }
              )
            },
            new Menu("View") {
              items = List(
                stationItem,
                qsoEntryItem,
                availableBands
              )
            }
          )
        }

        // Global ENTER handler (left as you had it; currently consumes Enter everywhere)
        onKeyPressed = (e: KeyEvent) =>
          if e.code == KeyCode.Enter && !e.shiftDown then
            e.consume()

        // --- Wire it together ---
        rootPane.top = menuBar
        rootPane.center = stationManager.pane() // default view

        root = rootPane
      }
    }