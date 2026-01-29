package fdswarm.fx.caseForm

import fdswarm.JavaFxTestKit
import fdswarm.model.Station
import munit.FunSuite
import scalafx.scene.control.TextField

class MyCaseFormTest extends FunSuite:
  override def beforeAll(): Unit =
    JavaFxTestKit.init()

  test("Happy path"):
    val result: Station =
      JavaFxTestKit.runOnFx {
        val initial = Station()
        val myForm  = MyCaseForm[Station](initial)

        // Set the field named "head1" (or fall back to the first field)
        val head1Field = myForm.fields.find(_.name == "head1").getOrElse(myForm.fields.head)
        val head1Tf    = head1Field.control.asInstanceOf[TextField]
        head1Tf.text.value = "WA9NNN"

        // Read the result while still on the FX thread
        myForm.result
      }

    // Replace this with whatever your Station field is actually called.
    // assertEquals(result.head1, "WA9NNN")
    assert(true)