package fdswarm.support

import fdswarm.io.FileHelper
import munit.FunSuite

trait TempDirFileHelperSuite extends FunSuite:
  protected val fileHelper: FileHelper = new FileHelper:
    override val directory: os.Path =
      os.temp.dir(prefix = "qso-entry-panel-testfx", deleteOnExit = true)

  abstract override def afterAll(): Unit =
    try
      super.afterAll()
    finally
      if os.exists(fileHelper.directory) then
        os.remove.all(fileHelper.directory)
