package monitor

import fdswarm.DirectoryProvider
import fdswarm.util.NodeIdentity

class NodeStoreTest extends munit.FunSuite:
  test("last index offset is persisted and restored by instanceId"):
    val dir = os.temp.dir(prefix = "node-store-test-")
    val directoryProvider: DirectoryProvider = () => dir
    val nodeIdentity = NodeIdentity(
      hostIp = "127.0.0.1",
      port = 8080,
      hostName = "alpha",
      instanceId = "alpha-id"
    )
    val sameInstanceNewAddress = nodeIdentity.copy(
      hostIp = "127.0.0.2",
      port = 8081,
      hostName = "alpha-renamed"
    )

    val firstStore = NodeStore(directoryProvider)
    firstStore.statusReceived(nodeIdentity)
    firstStore.updateNodeData(nodeIdentity, IndexOperation(itemCount = 3, offset = 42))

    val restartedStore = NodeStore(directoryProvider)
    restartedStore.statusReceived(sameInstanceNewAddress)

    assertEquals(restartedStore.nodes.head.lastIndexOffset.value, 42L)
