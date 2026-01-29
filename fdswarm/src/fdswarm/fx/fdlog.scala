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
import scalafx.application.JFXApp3
import scalafx.geometry.Insets
import scalafx.scene.{Node, Scene}
import scalafx.scene.control.*
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.scene.layout.*


object fdlog extends  JFXApp3 with LazyLogging:
  logger.info("fdlog ctor")
  val injector: Injector = Guice.createInjector(new ConfigModule())

  val qsoEntryPanel: QsoEntryPanel = injector.getInstance(classOf[QsoEntryPanel])

  override def start(): Unit =
    logger.info("start...")

    // Root layout + GLOBAL ENTER HANDLER
    stage = new JFXApp3.PrimaryStage {
      title = "FDLog (ScalaFX)"
      width = 1100
      height = 650

      scene = new Scene {

        private val initial: Station = Station()


        private val caseForm = MyCaseForm(initial)

//        val panestation: Node = StationCaseFormExample.pane(initial,
//          onSave = s =>
//            logger.info(s"Saving station case: $s")
//        )



        // Global ENTER → save, except when in Notes
        onKeyPressed = (e: KeyEvent) =>
          if e.code == KeyCode.Enter && !e.shiftDown then
//            if !notesArea.isFocused then
//              saveQso()
              e.consume()

/*
        val bottomBox = new VBox {
          spacing = 4
          children = Seq(
            qsoEntryPanel()
//            presetBar, // presets row
//            formGrid,
//            buttonBar,
//            statusLabel
          )
        }
*/

        root = new BorderPane {
          center = new VBox {
            private val stationPane: Pane = caseForm.pane
            top = stationPane
            padding = Insets(8)
            spacing = 4
            children = Seq(
              new Label("QSOs") {
                style = "-fx-font-size: 16px; -fx-font-weight: bold;"
              },
              qsoEntryPanel(),
//              submitBtn
            )
          }
//          bottom = bottomBox
        }
      }
    }


