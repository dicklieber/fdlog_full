package app

import javafx.application.Application
import javafx.stage.Stage as JfxStage
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.layout.VBox
import scalafx.stage.Stage

class HelloApp extends Application:
  override def start(
      primaryStage: JfxStage
    ): Unit =
    val helloButton = new Button("Hello")
    helloButton.id = "helloButton"

    val rootNode = new VBox:
      children = Seq(helloButton)

    val scene = new Scene:
      content = rootNode

    val stage = new Stage(primaryStage)
    stage.scene = scene
    stage.show()
