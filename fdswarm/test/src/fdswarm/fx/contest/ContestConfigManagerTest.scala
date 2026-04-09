package fdswarm.fx.contest

import fdswarm.io.DirectoryProvider
import fdswarm.model.Callsign
import fdswarm.store.QsoStore
import fdswarm.util.FilenameStamp
import jakarta.inject.Provider
import munit.FunSuite
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*

import java.io.File
import java.nio.file.Files

class ContestConfigManagerTest extends FunSuite:

  val tempDir = Files.createTempDirectory("contest-config-test")
  val directoryProvider = mock(classOf[DirectoryProvider])
  when(directoryProvider.apply()).thenReturn(os.Path(tempDir.toAbsolutePath.toString))

  val qsoStore = mock(classOf[QsoStore])
  val qsoStoreProvider = new Provider[QsoStore] {
    override def get(): QsoStore = qsoStore
  }
  val filenameStamp = mock(classOf[FilenameStamp])
  when(filenameStamp.build()).thenReturn("20260331-1526")

  val ignoreStatusSec = 60

  override def afterAll(): Unit =
    os.remove.all(os.Path(tempDir.toAbsolutePath.toString))

  test("hasConfiguration is false when no config exists") {
    val manager = new ContestConfigManager(directoryProvider, qsoStoreProvider, filenameStamp, ignoreStatusSec)
    assert(!manager.hasConfiguration.value)
  }

  test("hasConfiguration is true when config exists") {
    val config = ContestConfig(ContestType.WFD, Callsign("W1AW"), 2, "O", "CT")
    val manager = new ContestConfigManager(directoryProvider, qsoStoreProvider, filenameStamp, ignoreStatusSec)
    manager.setConfig(config)
    assert(manager.hasConfiguration.value)
  }

  test("hasConfiguration is true when loaded from file") {
    val config = ContestConfig(ContestType.WFD, Callsign("W1AW"), 2, "O", "CT")
    val contestFile = os.Path(tempDir.toAbsolutePath.toString) / "contest.json"
    
    import io.circe.syntax.*
    os.write.over(contestFile, config.asJson.spaces2)

    val manager = new ContestConfigManager(directoryProvider, qsoStoreProvider, filenameStamp, ignoreStatusSec)
    assert(manager.hasConfiguration.value)
  }

  test("hasConfiguration is true after handleRestartContest") {
    val config = ContestConfig(ContestType.WFD, Callsign("W1AW"), 2, "O", "CT")
    val subTempDir = Files.createTempDirectory("sub-contest-config-test")
    val subDirectoryProvider = mock(classOf[DirectoryProvider])
    when(subDirectoryProvider.apply()).thenReturn(os.Path(subTempDir.toAbsolutePath.toString))
    
    val manager = new ContestConfigManager(subDirectoryProvider, qsoStoreProvider, filenameStamp, ignoreStatusSec)
    assert(!manager.hasConfiguration.value)
    manager.handleRestartContest(config)
    assert(manager.hasConfiguration.value)
    
    os.remove.all(os.Path(subTempDir.toAbsolutePath.toString))
  }

  test("returns default values when not initialized") {
    val subTempDir = Files.createTempDirectory("empty-contest-config-test")
    val subDirectoryProvider = mock(classOf[DirectoryProvider])
    when(subDirectoryProvider.apply()).thenReturn(os.Path(subTempDir.toAbsolutePath.toString))

    val manager = new ContestConfigManager(subDirectoryProvider, qsoStoreProvider, filenameStamp, ignoreStatusSec)
    assert(!manager.hasConfiguration.value)
    assertEquals(
      manager.contestType,
      ContestType.NONE
    )
    assertEquals(
      manager.ourCallsign,
      Callsign("N0CALL")
    )
    assertEquals(
      manager.transmitters,
      0
    )
    assertEquals(
      manager.ourClass,
      "-"
    )
    assertEquals(
      manager.ourSection,
      "-"
    )
    assertEquals(
      manager.contestConfigOption,
      Some(ContestConfig.noContest)
    )
    
    os.remove.all(os.Path(subTempDir.toAbsolutePath.toString))
  }

  test("contestConfigProperty returns noContest when not initialized") {
    val subTempDir = Files.createTempDirectory("throws-contest-config-test")
    val subDirectoryProvider = mock(classOf[DirectoryProvider])
    when(subDirectoryProvider.apply()).thenReturn(os.Path(subTempDir.toAbsolutePath.toString))

    val manager = new ContestConfigManager(subDirectoryProvider, qsoStoreProvider, filenameStamp, ignoreStatusSec)
    assertEquals(
      manager.contestConfigProperty.value,
      ContestConfig.noContest
    )
    
    os.remove.all(os.Path(subTempDir.toAbsolutePath.toString))
  }

  test("contestConfigOption returns Some when initialized") {
    val config = ContestConfig(ContestType.WFD, Callsign("W1AW"), 2, "O", "CT")
    val manager = new ContestConfigManager(directoryProvider, qsoStoreProvider, filenameStamp, ignoreStatusSec)
    manager.setConfig(config)
    assertEquals(manager.contestConfigOption, Some(config))
  }

  test("clearContestConfig removes contest.json file") {
    val config = ContestConfig(
      ContestType.WFD,
      Callsign("W1AW"),
      2,
      "O",
      "CT"
    )
    val subTempDir = Files.createTempDirectory("clear-contest-config-test")
    val subDirectoryProvider = mock(classOf[DirectoryProvider])
    when(
      subDirectoryProvider.apply()
    ).thenReturn(
      os.Path(
        subTempDir.toAbsolutePath.toString
      )
    )
    val contestFile = os.Path(
      subTempDir.toAbsolutePath.toString
    ) / "contest.json"

    val manager = new ContestConfigManager(
      subDirectoryProvider,
      qsoStoreProvider,
      filenameStamp,
      ignoreStatusSec
    )
    manager.setConfig(
      config
    )
    assert(
      os.exists(
        contestFile
      )
    )

    manager.clearContestConfig()

    assert(
      !os.exists(
        contestFile
      )
    )
    assertEquals(
      manager.contestConfigProperty.value,
      ContestConfig.noContest
    )
    assert(
      !manager.hasConfiguration.value
    )

    os.remove.all(
      os.Path(
        subTempDir.toAbsolutePath.toString
      )
    )
  }
