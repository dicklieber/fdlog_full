package fdswarm.fx.qso

import com.typesafe.config.ConfigFactory
import fdswarm.bandmodes.SelectedBandModeManager
import fdswarm.bands.{BandCatalog, BandModeBuilder, ModeCatalog}
import fdswarm.{StartupInfo, StationConfigManager}
import fdswarm.fx.CallSignField
import fdswarm.fx.UserConfig
import fdswarm.fx.contest.{ContestCatalog, ContestConfig, ContestConfigManager, ContestType}
import fdswarm.fx.sections.{SectionField, SectionsProvider}
import fdswarm.fx.station.StationConfig
import fdswarm.model.{BandMode, Callsign, Qso}
import fdswarm.replication.Transport
import fdswarm.replication.status.ContestConfigMismatchUi
import fdswarm.store.{DupInfo, QsoStore, StyledMessage}
import fdswarm.support.TempDirFileHelperSuite
import fdswarm.util.{FilenameStamp, InstanceIdManager, NodeIdentityManager}
import munit.AfterEach
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyDouble, anyString}
import org.mockito.Mockito.{mock, verify, when}
import org.testfx.api.{FxRobot, FxToolkit}
import org.testfx.util.WaitForAsyncUtils
import scalafx.beans.property.BooleanProperty

class QsoEntryPanelTestFxTest extends TempDirFileHelperSuite:

  override def afterEach(
      context: AfterEach
    ): Unit =
    FxToolkit.cleanupStages()

  test("enters QSO WA9ZZZ 1H IL"):
    FxToolkit.registerPrimaryStage()
    FxToolkit.setupFixture(
      new Runnable:
        override def run(): Unit = ()
    )

    val startupInfo = new StartupInfo(
      Array.empty
    )
    val config = ConfigFactory.load()
    val bandCatalog = new BandCatalog(
      config
    )
    val modeCatalog = new ModeCatalog(
      config
    )
    val bandModeBuilder = new BandModeBuilder(
      bandCatalog = bandCatalog,
      modeCatalog = modeCatalog
    )
    val selectedBandModeManager = new SelectedBandModeManager(
      fileHelper = fileHelper,
      bandModeBuilder = bandModeBuilder,
      startupInfo = startupInfo
    )

    val stationManager = new StationConfigManager(
      fileHelper = fileHelper,
      startupInfo = startupInfo
    )
    stationManager.setStation(
      StationConfig(
        operator = Callsign("W9ABC"),
        rig = "",
        antenna = ""
      )
    )

    val contestManager = new ContestConfigManager(
      fileHelper = fileHelper,
      filenameStamp = new FilenameStamp(),
      nodeStatusDispatcher = null.asInstanceOf[fdswarm.replication.NodeStatusDispatcher],
      ignoreStatusSec = 0
    )
    contestManager.setConfig(
      ContestConfig(
        contestType = ContestType.WFD,
        ourCallsign = Callsign("W9ABC"),
        transmitters = 1,
        ourClass = "1D",
        ourSection = "IL"
      )
    )

    val uiConfig = ConfigFactory
      .parseString(
        """fdswarm {
          |  contests = [
          |    {
          |      name = "WFD"
          |      classChoices = [
          |        { ch = "H", description = "Home" }
          |      ]
          |    }
          |  ]
          |  sections = [
          |    {
          |      name = "US"
          |      sections = [
          |        { code = "IL", name = "Illinois" }
          |      ]
          |    }
          |  ]
          |}""".stripMargin
      )
      .withFallback(
        config
      )

    val contestCatalog = new ContestCatalog(
      uiConfig
    )
    val sectionsProvider = new SectionsProvider(
      uiConfig
    )

    val userConfig = new UserConfig(
      fileHelper
    )
    userConfig
      .getProperty[BooleanProperty](
        "useNextField"
      )
      .value = false

    val qsoStore = mock(
      classOf[QsoStore]
    )
    val transport = mock(
      classOf[Transport]
    )
    val dupPanel = mock(
      classOf[DupPanel]
    )
    val contestConfigMismatchUi = mock(
      classOf[ContestConfigMismatchUi]
    )

    when(
      dupPanel.pane()
    ).thenReturn(
      new scalafx.scene.layout.Pane()
    )
    when(
      contestConfigMismatchUi.warningButton(
        anyDouble()
      )
    ).thenReturn(
      new scalafx.scene.control.Button("!")
    )
    when(
      qsoStore.potentialDups(
        anyString(),
        any(
          classOf[BandMode]
        )
      )
    ).thenReturn(
      DupInfo(
        firstNDups = Seq.empty,
        totalDups = 0
      )
    )
    when(
      qsoStore.add(
        any(
          classOf[Qso]
        )
      )
    ).thenReturn(
      StyledMessage(
        text = "ok",
        css = "addQsoOk"
      )
    )

    val callsignField = new CallSignField(
      qsoStore = qsoStore,
      selectedBsndModeStore = selectedBandModeManager,
      userConfig = userConfig
    )
    val contestClassField = new ContestClassField(
      contestManager = contestManager,
      contestCatalog = contestCatalog,
      dupPanel = dupPanel,
      userConfig = userConfig
    )
    val sectionField = new SectionField(
      sectionsProvider = sectionsProvider,
      userConfig = userConfig
    )

    val nodeIdentityManager = new NodeIdentityManager(
      httpPort = 19000,
      instanceIdManager = new InstanceIdManager(
        fileHelper = fileHelper,
        startupInfo = startupInfo
      )
    )

    val panel = new QsoEntryPanel(
      qsoStore = qsoStore,
      transport = transport,
      selectedBandModeStore = selectedBandModeManager,
      stationManager = stationManager,
      contestManager = contestManager,
      callsignField = callsignField,
      contestClassField = contestClassField,
      sectionField = sectionField,
      dupPanel = dupPanel,
      nodeIdentityManager = nodeIdentityManager,
      contestConfigMismatchUi = contestConfigMismatchUi
    )

    FxToolkit.setupStage(
      stage =>
        panel.buildUi()
        stage.setScene(
          new javafx.scene.Scene(
            new javafx.scene.layout.StackPane(
              panel.node.delegate
            ),
            900,
            400
          )
        )
        stage.show()
    )

    val robot = new FxRobot
    robot.clickOn(
      "#qsoCallsignField"
    ).write(
      "WA9ZZZ"
    )
    robot.clickOn(
      "#qsoClassField"
    ).write(
      "1H"
    )
    robot.clickOn(
      "#qsoSectionField"
    ).write(
      "IL"
    ).`type`(
      javafx.scene.input.KeyCode.ENTER
    )

    WaitForAsyncUtils.waitForFxEvents()

    val qsoCaptor = ArgumentCaptor.forClass(
      classOf[Qso]
    )
    verify(
      qsoStore
    ).add(
      qsoCaptor.capture()
    )

    val submitted = qsoCaptor.getValue
    assertEquals(
      submitted.callsign.value,
      "WA9ZZZ"
    )
    assertEquals(
      submitted.exchange.fdClass.transmitters,
      1
    )
    assertEquals(
      submitted.exchange.fdClass.classLetter,
      'H'
    )
    assertEquals(
      submitted.exchange.sectionCode,
      "IL"
    )

    val callsignInput = robot.lookup(
      "#qsoCallsignField"
    ).query[javafx.scene.control.TextField]()
    val classInput = robot.lookup(
      "#qsoClassField"
    ).query[javafx.scene.control.TextField]()
    val sectionInput = robot.lookup(
      "#qsoSectionField"
    ).query[javafx.scene.control.TextField]()

    assertEquals(
      callsignInput.getText,
      ""
    )
    assertEquals(
      classInput.getText,
      ""
    )
    assertEquals(
      sectionInput.getText,
      ""
    )
