package manager

import munit.FunSuite

class FdswarmJarManagerTest extends FunSuite:

  test("jarInfo() is empty when jar does not exist"):
    val tempOut = os.temp.dir()
    val mgr = new FdswarmJarManager(outDir = tempOut)
    assertEquals(mgr.jarInfo(), None)

  test("jarInfo() is defined when jar exists"):
    val tempOut = os.temp.dir()
    val mgr = new FdswarmJarManager(outDir = tempOut)
    os.makeDir.all(tempOut / "fdswarm" / "assembly.dest")
    os.write.over(mgr.jarPath, "not-a-real-jar", createFolders = true)

    val after = mgr.jarInfo()
    assert(after.isDefined, "Jar should exist after creating test file")
