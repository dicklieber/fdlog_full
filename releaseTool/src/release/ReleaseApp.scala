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

      case _ =>
        usage()
  }

  private def prepareRelease(): Unit = {

    Git.ensureClean()

    val rv =
      Versioning.prepareReleaseVersion()

    Versioning.writePreparedRelease(rv)
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
        |""".stripMargin
    )
  }

}
