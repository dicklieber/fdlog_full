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

import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging
import fdswarm.fx.caseForm.{ChoiceField, MyCaseForm, SpinnerField}
import fdswarm.fx.tools.ZonedDateTimeEditor
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
import fdswarm.replication.{Transport, Service}
import io.circe.syntax.*
import fdswarm.util.JavaTimeCirce.given
import javafx.util.StringConverter
import fdswarm.fx.utils.{BootstrapIcons, IconButton}
import fdswarm.util.NodeIdentity
import scalafx.animation.{KeyFrame, Timeline}
import scalafx.application.Platform
import scalafx.beans.property.ReadOnlyStringWrapper
import scalafx.event.ActionEvent
import scalafx.util.Duration
import java.time.*

@Singleton
final class ContestManager @Inject()(
                                      productionDirectory: DirectoryProvider,
                                      contestCatalog: ContestCatalog,
                                      sections: Sections,
                                      qsoStore: fdswarm.store.QsoStore,
                                      filenameStamp: fdswarm.util.FilenameStamp,
                                      transport: fdswarm.replication.Transport,
                                      contestDiscovery: ContestDiscovery,
                                      @Named("fdswarm.contestChangeIgnoreStatusSec") ignoreStatusSec: Int
                                    ) extends LazyLogging:

  private var lastRestartTime: Long = 0L

  def shouldIgnoreStatus: Boolean =
    val now = System.currentTimeMillis()
    (now - lastRestartTime) < (ignoreStatusSec * 1000L)

  private val file: os.Path =
    productionDirectory() / "contest.json"

  val configProperty: ObjectProperty[ContestConfig] =
    val loaded = load()
    ObjectProperty(loaded)

  val contestTimesProperty: ObjectProperty[ContestTimes] =
    val now = ZonedDateTime.now()
    val initialDates = configProperty.value.contestType.dates(now.getYear)
    ObjectProperty(ContestTimes(initialDates.startUtc, initialDates.endUtc))

  // Keep persist in sync with config
  configProperty.onChange { (prop, oldConfig, newConfig) =>
    if (oldConfig == null || oldConfig.contestType != newConfig.contestType) {
      val now = ZonedDateTime.now()
      val newDates = newConfig.contestType.dates(now.getYear)
      contestTimesProperty.value = ContestTimes(newDates.startUtc, newDates.endUtc)
    }
    archiveAndPersist()
  }

  def config: ContestConfig = configProperty.value

  def setConfig(newConfig: ContestConfig): Unit =
    configProperty.value = newConfig

  def classChars: String =
    contestCatalog.getContest(config.contestType).map(_.classCharsString).getOrElse("")

  private case class ContestConfigProxy(
      contest: ContestType,
      ourCallsign: Callsign,
      transmitters: SpinnerField,
      ourClass: ChoiceField[String],
      ourSection: ChoiceField[String]
  )

  private case class DiscoveryResult(node: NodeIdentity, config: ContestConfig)

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
      val newClasses = getClasses(newType)
      classCombo.items = ObservableBuffer.from(newClasses.map(_.ch))
      // fully reset selection to the first valid option for the new contest (if any)
      if newClasses.nonEmpty then classCombo.value = newClasses.head.ch else classCombo.value = null

    val hasQsos = qsoStore.all.nonEmpty

    // --- Discovery Table ---
    val resultsBuffer = ObservableBuffer.empty[DiscoveryResult]
    val table = new TableView[DiscoveryResult](resultsBuffer) {
      columns ++= List(
        new TableColumn[DiscoveryResult, String] {
          text = "Node"
          cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.node.toString) }
          prefWidth = 120
        },
        new TableColumn[DiscoveryResult, String] {
          text = "Callsign"
          cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.config.ourCallsign.toString) }
          prefWidth = 80
        },
        new TableColumn[DiscoveryResult, String] {
          text = "Contest"
          cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.config.contestType.name) }
          prefWidth = 120
        },
        new TableColumn[DiscoveryResult, String] {
          text = "Class"
          cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.config.ourClass) }
          prefWidth = 40
        },
        new TableColumn[DiscoveryResult, String] {
          text = "Section"
          cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.config.ourSection) }
          prefWidth = 60
        },
        new TableColumn[DiscoveryResult, String] {
          text = "Tx"
          cellValueFactory = { cellData => ReadOnlyStringWrapper(cellData.value.config.transmitters.toString) }
          prefWidth = 40
        }
      )
      prefHeight = 200
      
      selectionModel.value.selectedItem.onChange { (_, _, result) =>
        if (result != null) {
          val selected = result.config
          contestCombo.value = selected.contestType
          myCaseForm.control[TextField]("ourCallsign").text = selected.ourCallsign.value
          myCaseForm.control[Spinner[Int]]("transmitters").valueFactory.value.value = selected.transmitters
          classCombo.value = selected.ourClass
          myCaseForm.control[ComboBox[String]]("ourSection").value = selected.ourSection
        }
      }
    }

    val progressBar = new ProgressBar {
      prefWidth = 150
      progress = 0
      visible = false
      managed = false
    }

    val statusLabel = new Label {
      visible = false
      managed = false
    }

    val discoverButton = IconButton("search", 16, "Search for other nodes")
    discoverButton.text = "Discover"
    discoverButton.minWidth = Region.USE_PREF_SIZE
    val doDiscover: () => Unit = () => {
        discoverButton.disable = true
        resultsBuffer.clear()
        progressBar.visible = true
        progressBar.managed = true
        progressBar.progress = 0
        statusLabel.visible = true
        statusLabel.managed = true
        statusLabel.text = "Searching..."

        val totalMs = contestDiscovery.timeoutSec * 1000.0
        val updateIntervalMs = 50.0

        val timeline = new Timeline {
          cycleCount = (totalMs / updateIntervalMs).toInt
          keyFrames = Seq(
            KeyFrame(
              Duration(updateIntervalMs),
              onFinished = (_: ActionEvent) => {
                progressBar.progress.value =
                  progressBar.progress.value + (updateIntervalMs / totalMs)
              }
            )
          )
        }
        timeline.play()

        new Thread(() => {
          val results = contestDiscovery.discoverContest((_, _) => {
            Platform.runLater {
              statusLabel.text = s"Received ${resultsBuffer.size + 1} responses"
            }
          })
          Platform.runLater {
            resultsBuffer.setAll(results.map { case (node, config) =>
              DiscoveryResult(node, config)
            }.toSeq*)
            discoverButton.disable = false
            progressBar.visible = false
            progressBar.managed = false
            timeline.stop()
            statusLabel.text = if (results.nonEmpty) s"Discovered ${results.size} nodes" else "No nodes discovered"
          }
        }).start()
    }

    discoverButton.onAction = _ => doDiscover()

    val discoveryHeader = new HBox(10) {
      alignment = scalafx.geometry.Pos.CenterLeft
      children = Seq(new Label("Discovery Results:"), discoverButton, progressBar, statusLabel)
    }

    val dialogContent: VBox = new VBox(10):
      padding = Insets(10)
      children = Seq(pane, new Separator(), discoveryHeader, table)
      prefWidth = 600
      prefHeight = 500

    val d = new Dialog[ContestConfigProxy]():
      initOwner(ownerWindow)
      title = "Contest Detail"
      headerText = "Edit Contest Configuration"
      dialogPane().content = dialogContent
      dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)
      resizable = true
      
      val okButton = dialogPane().lookupButton(ButtonType.OK)
      
      resultConverter =
        case ButtonType.OK => myCaseForm.result
        case _             => null

    if hasQsos then
      val warningLabel = new Label("QSOs exist, this will delete any existing QSOs, do not do this once contest has begun.") {
        style = "-fx-text-fill: red; -fx-font-weight: bold;"
        minWidth = Region.USE_PREF_SIZE
        wrapText = true
      }
      dialogContent.children.add(0, warningLabel)

    // Start discovery automatically when the dialog opens
    doDiscover()

    val result = d.showAndWait()
    result match
      case Some(p: ContestConfigProxy) =>
        val newConfig = ContestConfig(p.contest, p.ourCallsign, p.transmitters.value, p.ourClass.value, p.ourSection.value)
        val changed = newConfig != config

        if hasQsos && changed then
          val confirm = new Alert(Alert.AlertType.Confirmation) {
            initOwner(ownerWindow)
            title = "Confirm Changes"
            headerText = "QSOs exist"
            contentText = "Changing contest configuration will delete all existing QSOs. Are you sure?"
          }
          val res = confirm.showAndWait()
          if (res.contains(ButtonType.OK)) {
            handleRestartContest(newConfig)
            val configBytes = newConfig.asJson.noSpaces.getBytes("UTF-8")
            transport.send(Service.RestartContest, configBytes)
          }
        else if changed then
          handleRestartContest(newConfig)
          val configBytes = newConfig.asJson.noSpaces.getBytes("UTF-8")
          transport.send(Service.RestartContest, configBytes)
      case _ => ()

  def handleRestartContest(newConfig: ContestConfig): Unit =
    // 1. Rename qsosJournal.json
    // 2. Clear memory stores
    // 3. Save new config to contest.json and timestamped file
    // 4. Update the local property
    lastRestartTime = System.currentTimeMillis()
    archiveAndClear()
    setConfig(newConfig)

  private def promptForYear(ownerWindow: Window): Option[Int] =
    val d = new TextInputDialog(LocalDate.now().getYear.toString):
      initOwner(ownerWindow)
      title = "Select Year"
      headerText = "Enter the year for the contest"
      contentText = "Year:"

    d.showAndWait().flatMap(_.toIntOption)

  private def updateZonedDateTimeControl(form: MyCaseForm[?], fieldName: String, zdt: ZonedDateTime): Unit =
    val field = form.fields.find(_.name == fieldName).get
    field.control match {
      case editor: ZonedDateTimeEditor => editor.value = zdt
      case _ =>
        // Fallback for any other control types if necessary, though currently it should be ZonedDateTimeEditor
        logger.warn(s"Control for $fieldName is not a ZonedDateTimeEditor")
    }

  // ---- persistence ----------------------------------------------------------

  private def archiveAndPersist(): Unit =
    try
      val timestampedFile = productionDirectory() / s"${filenameStamp.build()}.contest.json"
      val json = configProperty.value.asJson.spaces2
      os.write.over(file, json, createFolders = true)
      os.copy.over(file, timestampedFile)
      logger.info(s"Persisted contest config to $file and archived to $timestampedFile")
    catch
      case e: Throwable => logger.error(s"Failed to persist contest config to $file", e)

  def archiveAndClear(): Unit =
    qsoStore.archiveAndClear()
    archiveAndPersist()

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
    ContestConfig(ContestType.WFD, Callsign("W1AW"), 1, "O", "CT")
