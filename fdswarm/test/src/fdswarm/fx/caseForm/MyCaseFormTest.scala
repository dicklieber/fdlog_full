package fdswarm.fx.caseForm

import fdswarm.JavaFxTestKit
import fdswarm.model.Station
import munit.FunSuite
import scalafx.scene.control.TextField
import ujson.Value
import upickle.default.*
class MyCaseFormTest extends FunSuite:
  override def beforeAll(): Unit =
    JavaFxTestKit.init()

  test("Happy path"):
    val initial = Station.defaultStation
    val myCaseForm: MyCaseForm[Station] = MyCaseForm[Station](initial, newStation =>
      println(newStation)
      val json: Value = writeJs(newStation)
      println(json)
    )

    JavaFxTestKit.runOnFx {

      // Set the field named "head1" (or fall back to the first field)
      val head1Field: Field[String] = myCaseForm.fields.head
      val head1Tf = head1Field.control.asInstanceOf[TextField]
      head1Tf.text.value = "WA9NNN"
      myCaseForm.saveButton.fire()
     }

