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
import fdswarm.fx.caseForm.{ChoiceField, MyCaseForm, SpinnerField}
import fdswarm.fx.sections.{Section, Sections}
import fdswarm.model.Callsign
import fdswarm.io.DirectoryProvider
import jakarta.inject.{Inject, Singleton}
import scalafx.Includes.*
import scalafx.beans.property.ObjectProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.stage.Window
import io.circe.parser.*
import io.circe.syntax.*
import fdswarm.util.JavaTimeCirce.given
import javafx.util.StringConverter
import java.time.*

@Singleton
final class ContestManager @Inject()(
                                      productionDirectory: DirectoryProvider,
                                      contestCatalog: ContestCatalog,
                                      sections: Sections,
                                      qsoStore: fdswarm.store.QsoStore
                                    ) extends LazyLogging:

  private val file: os.Path =
    productionDirectory() / "contest.json"

  val configProperty: ObjectProperty[ContestConfig] =
    ObjectProperty(load())

  // Keep persist in sync with config
  configProperty.onChange { (_, _, _) =>
    persist()
  }

  def config: ContestConfig = configProperty.value

  def setConfig(newConfig: ContestConfig): Unit =
    configProperty.value = newConfig

  def classChars: String =
    contestCatalog.getContest(config.contestType).map(_.classCharsString).getOrElse("")

  private case class ContestConfigProxy(
      contest: ContestType,
      start: ZonedDateTime,
      end: ZonedDateTime,
      ourCallsign: Callsign,
      transmitters: SpinnerField,
      ourClass: ChoiceField[String],
      ourSection: ChoiceField[String]
  )

  def show(ownerWindow: Window): Unit =
    def getClasses(contestType: ContestType): Seq[ClassChoice] =
      contestCatalog.contests.find(_.name == contestType).map(_.classChars).getOrElse(Seq.empty)

    var currentContestType: ContestType = config.contestType

    val sectionsList = sections.all.sortBy(_.code)

    def transmittersSpinnerField(currentTransmitters: Int): SpinnerField =
      SpinnerField(currentTransmitters, 1, 100)

    def classChoiceField(currentClassCode: String): ChoiceField[String] =
      ChoiceField(
        currentClassCode,
        currentVal =>
          new ComboBox[String](ObservableBuffer.from(getClasses(currentContestType).map(_.ch))) {
            editable = false
            currentVal.foreach(v => value = v)
            converter = new StringConverter[String] {
              override def toString(ch: String): String = if (ch == null) "" else ch
              override def fromString(s: String): String = if (s == null) "" else s
            }
            cellFactory = (lv: ListView[String]) =>
              new ListCell[String] {
                item.onChange { (_, _, it) =>
                  text = if (it == null) ""
                  else
                    getClasses(currentContestType)
                      .find(_.ch == it)
                      .map(c => s"${c.ch} - ${c.description}")
                      .getOrElse(it)
                }
              }
            buttonCell = new ListCell[String] {
              item.onChange { (_, _, it) =>
                text = if (it == null) "" else it
              }
            }
            prefWidth <== scalafx.beans.binding.Bindings.createDoubleBinding(
              () => {
                val strings = getClasses(currentContestType).map(_.ch)
                val textObj = new scalafx.scene.text.Text()
                val maxW = strings.map { s =>
                  textObj.text = s
                  textObj.getLayoutBounds.getWidth
                }.maxOption.getOrElse(0.0)
                maxW + 60.0
              },
              items
            )
            maxWidth = Region.USE_PREF_SIZE
            minWidth = Region.USE_PREF_SIZE
          }
      )

    def sectionChoiceField(currentSectionCode: String): ChoiceField[String] =
      ChoiceField(
        currentSectionCode,
        currentVal =>
          new ComboBox[String](ObservableBuffer.from(sectionsList.map(_.code))) {
            editable = false
            currentVal.foreach(v => value = v)
            converter = new StringConverter[String] {
              override def toString(code: String): String = if (code == null) "" else code
              override def fromString(s: String): String = if (s == null) "" else s
            }
            cellFactory = (lv: ListView[String]) =>
              new ListCell[String] {
                item.onChange { (_, _, it) =>
                  text = if (it == null) ""
                  else
                    sectionsList
                      .find(_.code == it)
                      .map(s => s"${s.code} - ${s.name}")
                      .getOrElse(it)
                }
              }
            buttonCell = new ListCell[String] {
              item.onChange { (_, _, it) =>
                text = if (it == null) "" else it
              }
            }
            prefWidth <== scalafx.beans.binding.Bindings.createDoubleBinding(
              () => {
                val strings = sectionsList.map(_.code)
                val textObj = new scalafx.scene.text.Text()
                val maxW = strings.map { s =>
                  textObj.text = s
                  textObj.getLayoutBounds.getWidth
                }.maxOption.getOrElse(0.0)
                maxW + 60.0
              },
              items
            )
            maxWidth = Region.USE_PREF_SIZE
            minWidth = Region.USE_PREF_SIZE
          }
      )

    val proxy = ContestConfigProxy(
      config.contestType,
      config.start,
      config.end,
      config.ourCallsign,
      transmittersSpinnerField(config.transmitters),
      classChoiceField(config.ourClass),
      sectionChoiceField(config.ourSection)
    )

    val myCaseForm = MyCaseForm[ContestConfigProxy](proxy)
    val pane       = myCaseForm.pane()

    // Setup listener for contest type change to prompt for year and update class choices
    val contestCombo = myCaseForm.control[ComboBox[ContestType]]("contest")
    val classCombo   = myCaseForm.control[ComboBox[String]]("ourClass")

    // Initialize classCombo items
    classCombo.items = ObservableBuffer.from(getClasses(config.contestType).map(_.ch))

    contestCombo.onAction = _ =>
      val newType = contestCombo.value.value
      currentContestType = newType
      promptForYear(ownerWindow).foreach { year =>
        val newDates = newType.dates(year)
        updateZonedDateTimeControl(myCaseForm, "start", newDates.startUtc)
        updateZonedDateTimeControl(myCaseForm, "end", newDates.endUtc)
      }
      val newClasses = getClasses(newType)
      classCombo.items = ObservableBuffer.from(newClasses.map(_.ch))
      // fully reset selection to the first valid option for the new contest (if any)
      if newClasses.nonEmpty then classCombo.value = newClasses.head.ch else classCombo.value = null

    val recalculateButton = new Button("Recalculate Dates"):
      onAction = (e: javafx.event.ActionEvent) =>
        val currentType = contestCombo.value.value
        promptForYear(ownerWindow).foreach { year =>
          val newDates = currentType.dates(year)
          updateZonedDateTimeControl(myCaseForm, "start", newDates.startUtc)
          updateZonedDateTimeControl(myCaseForm, "end", newDates.endUtc)
        }

    val hasQsos = qsoStore.all.nonEmpty

    val dialogContent = new VBox(10):
      padding = Insets(10)
      children = Seq(pane, recalculateButton)
      if hasQsos then
        val warningLabel = new Label("Logged QSOs exist. Some changes are not allowed.") {
          style = "-fx-text-fill: red; -fx-font-weight: bold;"
        }
        children.add(0, warningLabel)
        
        // Lock specific fields if QSOs exist
        myCaseForm.control[Control]("contest").disable = true
        myCaseForm.control[Control]("transmitters").disable = true
        myCaseForm.control[Control]("ourClass").disable = true
        myCaseForm.control[Control]("ourSection").disable = true

    val d = new Dialog[ContestConfigProxy]():
      initOwner(ownerWindow)
      title = "Contest Detail"
      headerText = "Edit Contest Configuration"
      dialogPane().content = dialogContent
      dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)
      
      val okButton = dialogPane().lookupButton(ButtonType.OK)
      
      resultConverter =
        case ButtonType.OK => myCaseForm.result
        case _             => null

    val result = d.showAndWait()
    result match
      case Some(p: ContestConfigProxy) =>
        setConfig(
          ContestConfig(p.contest, p.start, p.end, p.ourCallsign, p.transmitters.value, p.ourClass.value, p.ourSection.value)
        )
      case _ => ()

  private def promptForYear(ownerWindow: Window): Option[Int] =
    val d = new TextInputDialog(LocalDate.now().getYear.toString):
      initOwner(ownerWindow)
      title = "Select Year"
      headerText = "Enter the year for the contest"
      contentText = "Year:"

    d.showAndWait().flatMap(_.toIntOption)

  private def updateZonedDateTimeControl(form: MyCaseForm[?], fieldName: String, zdt: ZonedDateTime): Unit =
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
    try
      val json = configProperty.value.asJson.spaces2
      os.write.over(file, json, createFolders = true)
    catch
      case e: Throwable => logger.error(s"Failed to persist contest config to $file", e)

  private def load(): ContestConfig =
    try
      if os.exists(file) then
        val json = os.read(file)
        decode[ContestConfig](json).toTry.get
      else
        defaultConfig()
    catch
      case e: Throwable =>
        logger.warn(s"Failed to load contest config from $file: ${e.getMessage}. Using default.")
        defaultConfig()

  private def defaultConfig(): ContestConfig =
    val now = ZonedDateTime.now(ZoneOffset.UTC)
    ContestConfig(ContestType.WFD, now, now.plusHours(24), Callsign("W1AW"), 1, "O", "CT")
