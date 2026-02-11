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
import fdswarm.fx.caseForm.MyCaseForm
import fdswarm.io.DirectoryProvider
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.beans.property.ObjectProperty
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.stage.Window
import upickle.default.*
import fdswarm.util.JavaTimePickle.given_ReadWriter_ZonedDateTime
import java.time.*

@Singleton
final class ContestManager @Inject()(
                                      productionDirectory: DirectoryProvider,
                                      contestCatalog: ContestCatalog
                                    ) extends LazyLogging:

  private val file: os.Path =
    productionDirectory() / "contest.json"

  val configProperty: ObjectProperty[ContestConfig] =
    ObjectProperty(load())

  val currentDetailProperty: ObjectProperty[ContestDetail] =
    ObjectProperty(toDetail(configProperty.value))

  // Keep detail in sync with config
  configProperty.onChange { (_, _, newValue) =>
    currentDetailProperty.value = toDetail(newValue)
    persist()
  }

  def config: ContestConfig = configProperty.value

  def setConfig(newConfig: ContestConfig): Unit =
    configProperty.value = newConfig
    configProperty.value = newConfig

  private def toDetail(config: ContestConfig): ContestDetail =
    val catalogEntry = contestCatalog.contests.find(_.name == config.contest)
    ContestDetail(
      contest = config.contest,
      start = config.start,
      end = config.end,
      classChars = catalogEntry.map(_.classChars.map(_.ch).mkString).getOrElse("")
    )

  def show(ownerWindow: Window): Unit =
    val myCaseForm = MyCaseForm[ContestConfig](config)
    val pane = myCaseForm.pane()

    // Setup listener for contest type change to prompt for year
    val contestCombo = myCaseForm.control[ComboBox[ContestType]]("contest")
    contestCombo.onAction = _ => {
      val newType = contestCombo.value.value
      promptForYear(ownerWindow).foreach { year =>
        val newDates = newType.dates(year)
        updateZonedDateTimeControl(myCaseForm, "start", newDates.startUtc)
        updateZonedDateTimeControl(myCaseForm, "end", newDates.endUtc)
      }
    }

    val recalculateButton = new Button("Recalculate Dates") {
      onAction = _ => {
        val currentType = contestCombo.value.value
        promptForYear(ownerWindow).foreach { year =>
          val newDates = currentType.dates(year)
          updateZonedDateTimeControl(myCaseForm, "start", newDates.startUtc)
          updateZonedDateTimeControl(myCaseForm, "end", newDates.endUtc)
        }
      }
    }

    val dialogContent = new VBox(10, pane, recalculateButton)

    val d = new Dialog[ContestConfig]() {
      initOwner(ownerWindow)
      title = "Contest Detail"
      headerText = "Edit Contest Configuration"
      dialogPane().content = dialogContent
      dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)
      resultConverter = {
        case ButtonType.OK => myCaseForm.result
        case _ => null
      }
    }

    val result = d.showAndWait()
    result match
      case Some(c: ContestConfig) => setConfig(c)
      case _ => ()

  private def promptForYear(ownerWindow: Window): Option[Int] =
    val d = new TextInputDialog(LocalDate.now().getYear.toString) {
      initOwner(ownerWindow)
      title = "Select Year"
      headerText = "Enter the year for the contest"
      contentText = "Year:"
    }
    d.showAndWait().flatMap(_.toIntOption)

  private def updateZonedDateTimeControl(form: MyCaseForm[ContestConfig], fieldName: String, zdt: ZonedDateTime): Unit =
    // MyCaseForm uses an HBox for ZonedDateTime with DatePicker and two Spinners
    val field = form.fields.find(_.name == fieldName).get
    val hb = field.control.asInstanceOf[scalafx.scene.layout.HBox]
    val datePicker = hb.children(0).asInstanceOf[javafx.scene.control.DatePicker]
    val hourSpinner = hb.children(2).asInstanceOf[javafx.scene.control.Spinner[Int]]
    val minSpinner = hb.children(4).asInstanceOf[javafx.scene.control.Spinner[Int]]

    datePicker.value = zdt.toLocalDate
    hourSpinner.getValueFactory.setValue(zdt.getHour)
    minSpinner.getValueFactory.setValue(zdt.getMinute)

  // ---- persistence ----------------------------------------------------------

  private def persist(): Unit =
    try {
      val json = write(configProperty.value, indent = 2)
      os.write.over(file, json, createFolders = true)
    } catch {
      case e: Throwable => logger.error(s"Failed to persist contest config to $file", e)
    }

  private def load(): ContestConfig =
    try {
      if (os.exists(file)) {
        val json = os.read(file)
        read[ContestConfig](json)
      } else {
        defaultConfig()
      }
    } catch {
      case e: Throwable =>
        logger.warn(s"Failed to load contest config from $file: ${e.getMessage}. Using default.")
        defaultConfig()
    }

  private def defaultConfig(): ContestConfig =
    val now = ZonedDateTime.now(ZoneOffset.UTC)
    ContestConfig(ContestType.WFD, now, now.plusHours(24))
