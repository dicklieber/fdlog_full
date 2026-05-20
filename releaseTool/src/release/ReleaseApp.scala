package release

object ReleaseApp {

  def main(
      args: Array[String]
  ): Unit = {

    args.toList match
      case "fetch-jdks" :: Nil =>
        Jdks.fetchJdks()

      case "check-jdks" :: Nil =>
        Jdks.checkJdks()

      case "prepare-release" :: Nil =>
        prepareRelease()

      case "abort-release" :: Nil =>
        Versioning.abortRelease()

      case "finish-release" :: Nil =>
        Versioning.finishRelease(None)

      case "finish-release" :: next :: Nil =>
        Versioning.finishRelease(Some(next))

      case "build-zips" :: Nil =>
        Packaging.buildZips()

      case "commit-release" :: Nil =>
        Git.commitRelease()

      case "publish-release" :: Nil =>
        Github.publishRelease()

      case "commit-next-development" :: Nil =>
        Git.commitNextDevelopment()

      case _ =>
        usage()
  }

  private def prepareRelease(): Unit = {

    Git.ensureClean()

    val rv =
      Versioning.prepareReleaseVersion()

    Versioning.writePreparedRelease(rv)

    println()
    println("Next commands:")
    println("  ./mill fdswarm.assembly")
    println("  ./mill releaseTool.run build-zips")
    println("  ./mill releaseTool.run commit-release")
    println("  ./mill releaseTool.run publish-release")
    println("  ./mill releaseTool.run finish-release")
    println("  ./mill releaseTool.run commit-next-development")
  }

  private def usage(): Unit = {

    println(
      """
        |Commands:
        |
        |  fetch-jdks
        |  check-jdks
        |  prepare-release
        |  abort-release
        |  finish-release
        |  build-zips
        |  commit-release
        |  publish-release
        |  commit-next-development
        |
        |Normal flow:
        |
        |  ./mill releaseTool.run prepare-release
        |  ./mill fdswarm.assembly
        |  ./mill releaseTool.run build-zips
        |  ./mill releaseTool.run commit-release
        |  ./mill releaseTool.run publish-release
        |  ./mill releaseTool.run finish-release
        |  ./mill releaseTool.run commit-next-development
        |""".stripMargin
    )
  }

}
