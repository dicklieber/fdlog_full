package fdswarm

import fdswarm.logging.LogEventFieldNames
import munit.FunSuite

class LogEventFieldNamesTest extends FunSuite:

  test("log4j event template includes message resolver"):
    val template =
      LogEventFieldNames.log4jEventTemplateWithFlattenedMdc

    assert(
      template.contains(
        "\"message\": {\"$resolver\": \"message\", \"stringified\": true}"
      )
    )
