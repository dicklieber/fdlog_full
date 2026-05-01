package fdswarm.io

import munit.FunSuite

class FileHelperTest extends FunSuite:
  test("loadOrDefault returns default when file is missing"):
    val dir = os.temp.dir(prefix = "file-helper-test", deleteOnExit = true)
    val fileHelper = new FileHelper:
      override val directory: os.Path = dir

    try
      assertEquals(fileHelper.loadOrDefault[String]("missing.json")("default"), "default")
    finally
      if os.exists(dir) then os.remove.all(dir)

  test("loadOrDefault reads existing JSON file"):
    val dir = os.temp.dir(prefix = "file-helper-test", deleteOnExit = true)
    val fileHelper = new FileHelper:
      override val directory: os.Path = dir

    try
      os.write.over(dir / "value.json", "\"saved\"", createFolders = true)

      assertEquals(fileHelper.loadOrDefault[String]("value.json")("default"), "saved")
    finally
      if os.exists(dir) then os.remove.all(dir)
