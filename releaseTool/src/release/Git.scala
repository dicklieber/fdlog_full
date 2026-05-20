package release

object Git {

  def ensureClean(): Unit = {

    val result =
      os.proc("git", "status", "--porcelain")
        .call()
        .out
        .text()
        .trim()

    if result.nonEmpty then
      sys.error(
        s"""
           |git working tree not clean:
           |
           |$result
           |""".stripMargin
      )
  }

}
