package fdswarm.logging

import munit.FunSuite

class LazyStructuredLoggingTest extends FunSuite:

  test("normalizeClassName removes $ suffix"):
    assertEquals(LazyStructuredLogging.normalizeClassName("com.foo.Bar$"), "com.foo.Bar")

  test("normalizeClassName removes fdswrn. prefix"):
    assertEquals(LazyStructuredLogging.normalizeClassName("fdswrn.com.foo.Bar"), "com.foo.Bar")

  test("normalizeClassName removes both fdswrn. prefix and $ suffix"):
    assertEquals(LazyStructuredLogging.normalizeClassName("fdswrn.com.foo.Bar$"), "com.foo.Bar")

  test("selectableLoggerNames includes normalized names"):
    LazyStructuredLogging.normalizeClassName("fdswrn.test.Logger")
    assert(LazyStructuredLogging.selectableLoggerNames.contains("test.Logger"))
