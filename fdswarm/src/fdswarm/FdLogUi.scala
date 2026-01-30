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

import com.google.inject.Injector
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

/** All UI wiring (menus, panes, scene graph).
  * App bootstrap lives in [[fdlog]] (FdLogApp.scala).
  */
final class FdLogUi(injector: Injector) extends LazyLogging:

  private val qsoEntryPanel: QsoEntryPanel =
    injector.getInstance(classOf[QsoEntryPanel])

  def primaryStage(): JFXApp3.PrimaryStage =
    new JFXApp3.PrimaryStage:

      title = "FDLog (ScalaFX)"
      width = 1100
      height = 650

      scene = buildScene()

  private def buildScene(): Scene =
    new Scene:

      private val stationManager: StationManager =
        injector.getInstance(classOf[StationManager])

      private val availableBandsStore: AvailableBandsStore =
        injector.getInstance(classOf[AvailableBandsStore])

      // --- Build the "views" we want to switch between ---

      private val qsoEntryPane: Pane =
        new VBox:
          padding = Insets(8)
          spacing = 4
          children = Seq(
            new Label("QSOs"):
              style = "-fx-font-size: 16px; -fx-font-weight: bold;"
            ,
            qsoEntryPanel()
          )

      // --- Root layout we can swap the center of ---
      private val rootPane = new BorderPane

      // --- MenuBar with proper toggle behavior ---
      private val viewToggles = new ToggleGroup

      private val stationItem = new RadioMenuItem("Station"):
        toggleGroup = viewToggles
        selected = true
        accelerator = KeyCombination.keyCombination("Shortcut+1")
        onAction = _ => rootPane.center = stationManager.pane()

      private val qsoEntryItem = new RadioMenuItem("QSO Entry"):
        toggleGroup = viewToggles
        accelerator = KeyCombination.keyCombination("Shortcut+2")
        onAction = _ => rootPane.center = qsoEntryPane

      private val availableBandsItem = new RadioMenuItem("Available Bands"):
        toggleGroup = viewToggles
        accelerator = KeyCombination.keyCombination("Shortcut+3")
        onAction = _ => rootPane.center = availableBandsStore.availableBandsPane

      private val menuBar = new MenuBar:
        menus = List(
          new Menu("File"):
            items = List(
              new MenuItem("Exit"):
                accelerator = KeyCombination.keyCombination("Shortcut+Q")
                onAction = _ => Platform.exit()
            )
          ,
          new Menu("View"):
            items = List(
              stationItem,
              qsoEntryItem,
              availableBandsItem
            )
        )

      // Global ENTER handler (left as you had it; currently consumes Enter everywhere)
      onKeyPressed = (e: KeyEvent) =>
        if e.code == KeyCode.Enter && !e.shiftDown then
          e.consume()

      // --- Wire it together ---
      rootPane.top = menuBar
      rootPane.center = stationManager.pane() // default view

      root = rootPane
