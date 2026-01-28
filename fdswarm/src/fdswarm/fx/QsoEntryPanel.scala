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

import jakarta.inject.{Inject, Singleton}
import scalafx.scene.control.*
import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.layout.GridPane
import _root_.fdswarm.store.QsoStore
import com.typesafe.scalalogging.LazyLogging
import _root_.fdswarm.model.{BandMode, Qso, QsoMetadata}
@Singleton
class QsoEntryPanel @Inject()(qsoStore: QsoStore) extends LazyLogging:
  val bandMode = BandMode()//todo 
  val qsoMetadata = QsoMetadata()//todo
  def apply()=
    val callSign:     TextField = new TextField { text = "" }
    val contestClass: TextField = new TextField { text = "" }
    val section:      TextField = new TextField { text = "" }

    section.onAction = handleSubmit()
    def handleSubmit() = () =>
      logger.debug(s"Submitting: $callSign $contestClass $section")
      val qso = Qso(callSign.text.value, contestClass.text.value, section.text.value, bandMode, qsoMetadata)
      qsoStore.add(qso)
      callSign.text.value = ""

    new GridPane:
        hgap = 8
        vgap = 8
        padding = Insets(10)

        add(new Label("Call sign:"), 0, 0)
        add(callSign, 1, 0)

        add(new Label("Class:"), 0, 1)
        add(contestClass, 1, 1)

        add(new Label("Section:"), 0, 2)
        add(section, 1, 2)

case class QsoEntry(
                     callSign:     TextField = new TextField { text = "" },
                     contestClass: TextField = new TextField { text = "" },
                     section:      TextField = new TextField { text = "" }
                   )