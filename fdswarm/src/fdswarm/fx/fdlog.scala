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
import fdswarm.fx.caseForm.MyCaseForm
import scalafx.Includes.*
import scalafx.application.{JFXApp3, Platform}
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.*
import scalafx.scene.input.{KeyCode, KeyCombination, KeyEvent}
import scalafx.scene.layout.*

object fdlog extends JFXApp3 with LazyLogging:
  logger.info("fdlog ctor")
  val injector: Injector = Guice.createInjector(new ConfigModule())

  val qsoEntryPanel: QsoEntryPanel = injector.getInstance(classOf[QsoEntryPanel])

  override def start(): Unit =

    stage = new JFXApp3.PrimaryStage {
      title = "FDLog (ScalaFX)"
      width = 1100
      height = 650

      scene = new Scene {
        private val initial: Station = Station()
        private val caseForm = MyCaseForm(initial)

        // --- Build the two "views" we want to switch between ---

        private val stationPane: Pane =
          caseForm.pane

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

        // --- View switching helpers ---
        private def showStation(): Unit =
          rootPane.center = stationPane

        private def showQsoEntry(): Unit =
          rootPane.center = qsoEntryPane

        // --- MenuBar with proper toggle behavior ---
        private val viewToggles = new ToggleGroup

        private val stationItem = new RadioMenuItem("Station") {
          toggleGroup = viewToggles
          selected = true
          accelerator = KeyCombination.keyCombination("Shortcut+1")
          onAction = _ => showStation()
        }

        private val qsoEntryItem = new RadioMenuItem("QSO Entry") {
          toggleGroup = viewToggles
          accelerator = KeyCombination.keyCombination("Shortcut+2")
          onAction = _ => showQsoEntry()
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
                qsoEntryItem
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
        showStation() // default view

        root = rootPane
      }
    }