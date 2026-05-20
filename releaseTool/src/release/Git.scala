package release

object Git {

  def ensureClean(): Unit = {

    val result =
      statusPorcelain()

    if result.nonEmpty then
      sys.error(
        s"""
           |git working tree not clean:
           |
           |$result
           |""".stripMargin
      )
  }

  def statusPorcelain(): String =
    os.proc("git", "status", "--porcelain")
      .call()
      .out
      .text()
      .trim()

  def tagExists(
      tag: String
  ): Boolean = {

    try {
      os.proc(
        "git",
        "rev-parse",
        tag
      ).call(
        stdout = os.Pipe,
        stderr = os.Pipe
      )
      true
    } catch {
      case _: Throwable =>
        false
    }
  }

  def commitRelease(): Unit = {

    val version =
      Versioning.currentVersion()

    if version.endsWith("-SNAPSHOT") then
      sys.error(
        s"cannot commit snapshot version: $version"
      )

    val tag =
      s"v$version"

    Process.run(
      Seq(
        "git",
        "add",
        "version.txt",
        "buildnumber.txt"
      )
    )

    val status =
      statusPorcelain()

    if status.nonEmpty then
      Process.run(
        Seq(
          "git",
          "commit",
          "-m",
          s"Release $version"
        )
      )
    else
      println("[skip] nothing to commit")

    if tagExists(tag) then
      println(s"[skip] tag exists: $tag")
    else
      Process.run(
        Seq(
          "git",
          "tag",
          tag
        )
      )

    Process.run(Seq("git", "push"))
    Process.run(Seq("git", "push", "--tags"))

    println()
    println(s"[ok] committed and pushed release $version")
  }

}
