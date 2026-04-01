package fdswarm.fx.contest

import fdswarm.io.DirectoryProvider
import fdswarm.model.Callsign
import fdswarm.store.QsoStore
import fdswarm.util.FilenameStamp
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
  val filenameStamp = mock(classOf[FilenameStamp])
  when(filenameStamp.build()).thenReturn("20260331-1526")

  val ignoreStatusSec = 60

  override def afterAll(): Unit =
    os.remove.all(os.Path(tempDir.toAbsolutePath.toString))

  test("hasConfiguration is false when no config exists") {
    val manager = new ContestConfigManager(directoryProvider, qsoStore, filenameStamp, ignoreStatusSec)
    assert(!manager.hasConfiguration.value)
  }

  test("hasConfiguration is true when config exists") {
    val config = ContestConfig(ContestType.WFD, Callsign("W1AW"), 2, "O", "CT")
    val manager = new ContestConfigManager(directoryProvider, qsoStore, filenameStamp, ignoreStatusSec)
    manager.setConfig(config)
    assert(manager.hasConfiguration.value)
  }

  test("hasConfiguration is true when loaded from file") {
    val config = ContestConfig(ContestType.WFD, Callsign("W1AW"), 2, "O", "CT")
    val contestFile = os.Path(tempDir.toAbsolutePath.toString) / "contest.json"
    
    import io.circe.syntax.*
    os.write.over(contestFile, config.asJson.spaces2)

    val manager = new ContestConfigManager(directoryProvider, qsoStore, filenameStamp, ignoreStatusSec)
    assert(manager.hasConfiguration.value)
  }

  test("hasConfiguration is true after handleRestartContest") {
    val config = ContestConfig(ContestType.WFD, Callsign("W1AW"), 2, "O", "CT")
    val subTempDir = Files.createTempDirectory("sub-contest-config-test")
    val subDirectoryProvider = mock(classOf[DirectoryProvider])
    when(subDirectoryProvider.apply()).thenReturn(os.Path(subTempDir.toAbsolutePath.toString))
    
    val manager = new ContestConfigManager(subDirectoryProvider, qsoStore, filenameStamp, ignoreStatusSec)
    assert(!manager.hasConfiguration.value)
    manager.handleRestartContest(config)
    assert(manager.hasConfiguration.value)
    
    os.remove.all(os.Path(subTempDir.toAbsolutePath.toString))
  }
