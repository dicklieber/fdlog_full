package fdswarm.fx.caseForm

import fdswarm.JavaFxTestKit
import fdswarm.model.Station
import munit.FunSuite
import scalafx.scene.control.TextField
import scalafx.scene.layout.VBox

class MyCaseFormTest extends FunSuite:
  override def beforeAll(): Unit =
    JavaFxTestKit.init()

  test("Happy path"):
    val initial = Station()
    val myForm = MyCaseForm[Station](initial)

    val pane = myForm.pane.asInstanceOf[VBox]

    JavaFxTestKit.runOnFx {

      // Set the field named "head1" (or fall back to the first field)
      val head1Field = myForm.fields.find(_.name == "head1").getOrElse(myForm.fields.head)
      val head1Tf = head1Field.control.asInstanceOf[TextField]
      head1Tf.text.value = "WA9NNN"
     }

    val result: Station =     myForm.result
    assertEquals( result.bandName,"WA9NNN")
