package release

case class ReleaseVersion(
    snapshotVersion: String,
    baseVersion: String,
    buildNumber: Int,
    releaseVersion: String,
    tagName: String
)

object Versioning {

  private val versionFile =
    os.pwd / "version.txt"

  private val buildNumberFile =
    os.pwd / "buildnumber.txt"

  def currentVersion(): String =
    os.read(versionFile).trim

  def currentBuildNumber(): Int = {

    if !os.exists(buildNumberFile) then {
      os.write.over(buildNumberFile, "0\n")
      0
    } else
      os.read(buildNumberFile).trim.toInt
  }

  def prepareReleaseVersion(): ReleaseVersion = {

    val snapshot =
      currentVersion()

    if !snapshot.endsWith("-SNAPSHOT") then
      sys.error(s"version.txt must end with -SNAPSHOT, found: $snapshot")

    val base =
      snapshot.stripSuffix("-SNAPSHOT")

    val nextBuild =
      currentBuildNumber() + 1

    val release =
      s"$base-$nextBuild"

    ReleaseVersion(
      snapshot,
      base,
      nextBuild,
      release,
      s"v$release"
    )
  }

  def writePreparedRelease(
      rv: ReleaseVersion
  ): Unit = {

    os.write.over(
      versionFile,
      s"${rv.releaseVersion}\n"
    )

    os.write.over(
      buildNumberFile,
      s"${rv.buildNumber}\n"
    )

    println(s"[version] ${rv.snapshotVersion} -> ${rv.releaseVersion}")
    println(s"[buildnumber] ${rv.buildNumber}")
    println(s"[tag] ${rv.tagName}")
  }

  def abortRelease(): Unit = {

    val version =
      currentVersion()

    if version.endsWith("-SNAPSHOT") then {
      println(s"[ok] already snapshot: $version")
      return
    }

    val snapshot =
      version.replaceFirst("-[0-9]+$", "-SNAPSHOT")

    os.write.over(
      versionFile,
      s"$snapshot\n"
    )

    println(s"[version] $version -> $snapshot")
  }

  def finishRelease(
      nextSnapshot: Option[String]
  ): Unit = {

    val version =
      currentVersion()

    val next =
      nextSnapshot.getOrElse(
        "0.0.1-SNAPSHOT"
      )

    os.write.over(
      versionFile,
      s"$next\n"
    )

    println(s"[version] $version -> $next")
  }

}
