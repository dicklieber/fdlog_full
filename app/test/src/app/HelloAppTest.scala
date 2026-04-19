package app

import javafx.scene.Node
import munit.AfterEach
import munit.BeforeEach
import munit.FunSuite
import org.testfx.api.FxRobot
import org.testfx.api.FxToolkit

class HelloAppTest extends FunSuite:
  override def beforeEach(
      context: BeforeEach
    ): Unit =
    FxToolkit.registerPrimaryStage()
    FxToolkit.setupApplication(() => new HelloApp)

  override def afterEach(
      context: AfterEach
    ): Unit =
    FxToolkit.cleanupStages()

  test("hello button exists and can be clicked"):
    val robot = new FxRobot
    val node = robot.lookup("#helloButton").query[Node]()
    assert(node != null)
    robot.clickOn("#helloButton")
