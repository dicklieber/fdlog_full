package release

object Github {

  private val artifactsDir =
    os.pwd / "release" / "artifacts"

  def publishRelease(): Unit = {

    ensureGhInstalled()
    ensureGhAuthenticated()

    val version =
      Versioning.currentVersion()

    if version.endsWith("-SNAPSHOT") then
      sys.error(s"cannot publish snapshot version: $version")

    val tag =
      s"v$version"

    val artifacts =
      os.list(artifactsDir)
        .filter(p => os.isFile(p) && p.last.endsWith(".zip"))
        .sortBy(_.last)

    if artifacts.isEmpty then
      sys.error(s"no zip artifacts found in $artifactsDir")

    println(s"[tag] $tag")

    createReleaseIfMissing(tag)

    artifacts.foreach(uploadArtifact(tag, _))
  }

  private def ensureGhInstalled(): Unit =
    Process.run(Seq("gh", "--version"))

  private def ensureGhAuthenticated(): Unit =
    Process.run(Seq("gh", "auth", "status"))

  private def createReleaseIfMissing(
      tag: String
  ): Unit = {

    val exists =
      try {
        Process.run(
          Seq(
            "gh",
            "release",
            "view",
            tag
          )
        )
        true
      } catch {
        case _: Throwable =>
          false
      }

    if exists then {
      println(s"[exists] github release $tag")
      return
    }

    Process.run(
      Seq(
        "gh",
        "release",
        "create",
        tag,
        "--title",
        tag,
        "--generate-notes"
      )
    )
  }

  private def uploadArtifact(
      tag: String,
      artifact: os.Path
  ): Unit = {

    Process.run(
      Seq(
        "gh",
        "release",
        "upload",
        tag,
        artifact.toString,
        "--clobber"
      )
    )

    println(s"[uploaded] ${artifact.last}")
  }

}
