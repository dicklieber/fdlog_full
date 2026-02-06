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

package fdswarm.fx.contest

import com.typesafe.scalalogging.LazyLogging
import fdswarm.{ContestDateCalculator, ContestDates}
import fdswarm.fx.caseForm.MyCaseForm
import fdswarm.io.{DirectoryProvider, ProductionDirectory}
import jakarta.inject.*
import scalafx.Includes.*
import scalafx.beans.property.ObjectProperty
import scalafx.scene.control.*
import scalafx.stage.Window
import upickle.default.*
import fdswarm.util.JavaTimePickle.given_ReadWriter_ZonedDateTime

import java.time.LocalDate

@Singleton
class ContestManager @Inject()(directoryProvider: DirectoryProvider) extends LazyLogging:

  private val file: os.Path =
    directoryProvider() / "contest.json"

  val currentDetailProperty: ObjectProperty[ContestDetail] =
    ObjectProperty(load())

  private def defaultDetail: ContestDetail =
    val year = LocalDate.now().getYear
    val contestDates = ContestDateCalculator.datesFor(Contest.WFD, year)
    ContestDetail(
      contentName = "WFD",
      classChars = "HIOM",
      start = contestDates.startUtc,
      end = contestDates.endUtc,
    )

  private def load(): ContestDetail =
    try {
      if os.exists(file) then
        val json = os.read(file)
        read[ContestDetail](json)
      else
        defaultDetail
    } catch {
      case e: Throwable =>
        logger.warn(s"Failed to load contest detail from $file: ${e.getMessage}")
        defaultDetail
    }

  private def save(): Unit =
    try {
      val json = write(currentDetailProperty.value, indent = 2)
      os.write.over(file, json, createFolders = true)
    } catch {
      case e: Throwable =>
        logger.error(s"Failed to save contest detail to $file: ${e.getMessage}")
    }

  def show(ownerWindow: Window): Unit =
    val form = MyCaseForm(currentDetailProperty.value, detail => {
      currentDetailProperty.value = detail
      save()
      println(s"Saved contest detail: $detail")
    })

    val dialog = new Dialog[ButtonType] {
      initOwner(ownerWindow)
      title = "Contest Detail"
    }

    dialog.dialogPane().content = form.pane()
    dialog.dialogPane().buttonTypes = Seq(ButtonType.Close)

    dialog.showAndWait()

  def menuItem(using owningWindow: Window): MenuItem =
    new MenuItem("Contest Detail"):
      onAction = _ => show(owningWindow)
  
