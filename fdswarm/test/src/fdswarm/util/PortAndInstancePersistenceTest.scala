package fdswarm.util

import munit.FunSuite
import fdswarm.io.DirectoryProvider

class PortAndInstancePersistenceTest extends FunSuite:

  test("ourInstanceId is persisted and reloaded from DirectoryProvider"):
    val tmpDir = os.temp.dir()
    val provider = new DirectoryProvider:
      def apply(): os.Path = tmpDir

    // First initialization
    PortAndInstance.initOurInstanceId(provider)
    val id1 = PortAndInstance.ourInstanceId
    assert(id1.nonEmpty)
    assert(os.exists(tmpDir / ".instanceId"))
    val savedId = os.read(tmpDir / ".instanceId").trim
    assertEquals(id1, savedId)

    // Second initialization (reloading)
    PortAndInstance.initOurInstanceId(provider)
    val id2 = PortAndInstance.ourInstanceId
    assertEquals(id1, id2)

    // Modification of the file
    os.write.over(tmpDir / ".instanceId", "custom-id")
    PortAndInstance.initOurInstanceId(provider)
    assertEquals(PortAndInstance.ourInstanceId, "custom-id")
