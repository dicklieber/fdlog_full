package manager

import munit.FunSuite

class FdswarmJarManagerTest extends FunSuite:

  test("build() creates jar in temp MILL_OUT"):
    val tempOut = os.temp.dir()

    val mgr = new FdswarmJarManager(
      outDir = tempOut
    )

    assertEquals(mgr.jarInfo(), None)

    mgr.buildFdswarmJar()

//    fdswarm.util.printTreePretty(tempOut)

    val after = mgr.jarInfo()
    assert(after.isDefined, "Jar should exist after build")