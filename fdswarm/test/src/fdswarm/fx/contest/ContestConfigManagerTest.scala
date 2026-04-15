package fdswarm.fx.contest

import fdswarm.io.DirectoryProvider
import fdswarm.model.{BandMode, BandModeOperator, Callsign}
import fdswarm.replication.{NodeStatus, StatusMessage}
import fdswarm.store.QsoStore
import fdswarm.util.NodeIdentity
import fdswarm.util.FilenameStamp
import io.circe.Encoder.AsArray.importedAsArrayEncoder
import io.circe.Encoder.AsObject.importedAsObjectEncoder
import io.circe.Encoder.AsRoot.importedAsRootEncoder
import io.circe.generic.auto.deriveEncoder
import jakarta.inject.Provider
import munit.FunSuite
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*

import java.io.File
import java.nio.file.Files
import java.time.Instant

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
      Callsign("")
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

  private def nodeStatusWithConfig(
                                    config: ContestConfig
                                  ): NodeStatus =
    val statusMessage = StatusMessage(
      hashCount = fdswarm.replication.HashCount(),
      hash = Seq.empty,
      bandNodeOperator = BandModeOperator(
        operator = Callsign(
          "W1AW"
        ),
        bandMode = BandMode(
          "20M CW"
        )
      ),
      contestConfig = config
    )
    NodeStatus(
      statusMessage = statusMessage,
      nodeIdentity = NodeIdentity.testNodeIdentity,
      isLocal = false
    )

  test("updateFromNodeStatus sets config when local is NONE and received is non-NONE") {
    val subTempDir = Files.createTempDirectory("update-node-status-none-test")
    val subDirectoryProvider = mock(classOf[DirectoryProvider])
    when(
      subDirectoryProvider.apply()
    ).thenReturn(
      os.Path(
        subTempDir.toAbsolutePath.toString
      )
    )
    val manager = new ContestConfigManager(
      subDirectoryProvider,
      qsoStoreProvider,
      filenameStamp,
      ignoreStatusSec
    )
    val receivedConfig = ContestConfig(
      contestType = ContestType.WFD,
      ourCallsign = Callsign("W1AW"),
      transmitters = 2,
      ourClass = "O",
      ourSection = "CT",
      stamp = Instant.parse("2026-04-01T00:00:00Z")
    )

    manager.updateFromNodeStatus(
      nodeStatusWithConfig(
        receivedConfig
      )
    )

    assertEquals(
      manager.contestConfigProperty.value,
      receivedConfig
    )
    os.remove.all(
      os.Path(
        subTempDir.toAbsolutePath.toString
      )
    )
  }

  test("updateFromNodeStatus replaces local config when received stamp is older") {
    val subTempDir = Files.createTempDirectory("update-node-status-older-test")
    val subDirectoryProvider = mock(classOf[DirectoryProvider])
    when(
      subDirectoryProvider.apply()
    ).thenReturn(
      os.Path(
        subTempDir.toAbsolutePath.toString
      )
    )
    val manager = new ContestConfigManager(
      subDirectoryProvider,
      qsoStoreProvider,
      filenameStamp,
      ignoreStatusSec
    )
    val localConfig = ContestConfig(
      contestType = ContestType.WFD,
      ourCallsign = Callsign("K1ABC"),
      transmitters = 3,
      ourClass = "I",
      ourSection = "IL",
      stamp = Instant.parse("2026-04-02T00:00:00Z")
    )
    val receivedConfig = ContestConfig(
      contestType = ContestType.WFD,
      ourCallsign = Callsign("W1AW"),
      transmitters = 2,
      ourClass = "O",
      ourSection = "CT",
      stamp = Instant.parse("2026-04-01T00:00:00Z")
    )
    manager.setConfig(
      localConfig
    )

    manager.updateFromNodeStatus(
      nodeStatusWithConfig(
        receivedConfig
      )
    )

    assertEquals(
      manager.contestConfigProperty.value,
      receivedConfig
    )
    os.remove.all(
      os.Path(
        subTempDir.toAbsolutePath.toString
      )
    )
  }

  test("updateFromNodeStatus ignores received config when contest type is NONE") {
    val subTempDir = Files.createTempDirectory("update-node-status-ignore-none-test")
    val subDirectoryProvider = mock(classOf[DirectoryProvider])
    when(
      subDirectoryProvider.apply()
    ).thenReturn(
      os.Path(
        subTempDir.toAbsolutePath.toString
      )
    )
    val manager = new ContestConfigManager(
      subDirectoryProvider,
      qsoStoreProvider,
      filenameStamp,
      ignoreStatusSec
    )
    val localConfig = ContestConfig(
      contestType = ContestType.WFD,
      ourCallsign = Callsign("K1ABC"),
      transmitters = 3,
      ourClass = "I",
      ourSection = "IL",
      stamp = Instant.parse("2026-04-02T00:00:00Z")
    )
    manager.setConfig(
      localConfig
    )

    manager.updateFromNodeStatus(
      nodeStatusWithConfig(
        ContestConfig.noContest
      )
    )

    assertEquals(
      manager.contestConfigProperty.value,
      localConfig
    )
    os.remove.all(
      os.Path(
        subTempDir.toAbsolutePath.toString
      )
    )
  }
