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
      releaseArtifacts(version)

    if artifacts.isEmpty then
      sys.error(
        s"no zip artifacts found for version $version in $artifactsDir"
      )

    println(s"[tag] $tag")

    createReleaseIfMissing(tag)

    artifacts.foreach(uploadArtifact(tag, _))

    println()
    println(s"[ok] github release published: $tag")
  }

  private def releaseArtifacts(
      version: String
  ): Seq[os.Path] = {

    if !os.exists(artifactsDir) then
      Seq.empty
    else
      os.list(artifactsDir)
        .filter(p =>
          os.isFile(p) &&
            p.last.startsWith(s"fdswarm-$version-") &&
            p.last.endsWith(".zip")
        )
        .toSeq
        .sortBy(_.last)
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

    println(s"[created] github release $tag")
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
