package release

object ReleaseApp {

  def main(args: Array[String]): Unit = {

    args.toList match
      case "gh-release" :: Nil =>
        ghRelease()

      case _ =>
        usage()
  }

  private def usage(): Unit = {

    println(
      """
        |usage:
        |
        |  ./mill releaseTool.run gh-release
        |
        |gh-release:
        |  - verifies clean git
        |  - commits release version
        |  - creates git tag
        |  - pushes git + tags
        |  - creates GitHub release
        |  - uploads release artifacts
        |""".stripMargin
    )
  }

  private def ghRelease(): Unit = {

    Git.ensureClean()
    Github.ensureGh()

    val version =
      Versioning.currentVersion()

    if version.endsWith("-SNAPSHOT") then
      sys.error(
        s"version.txt is still snapshot: $version"
      )

    val tag =
      s"v$version"

    Git.commitAll(
      s"Release $version"
    )

    Git.createTag(tag)

    Github.publishRelease()

    println()
    println("[ok] release completed")
  }

}
