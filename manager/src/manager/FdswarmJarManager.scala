package manager

import com.typesafe.scalalogging.LazyLogging
import java.time.Instant

/**
 * 
 * @param outDir for unit testing.
 */
class FdswarmJarManager(
                         outDir: os.Path = os.pwd / "out"
                       ) extends LazyLogging:

  def jarPath: os.Path =
    outDir / "fdswarm" / "assembly.dest" / "fdswarm-all.jar"

  def jarInfo(): Option[Instant] =
    if os.exists(jarPath) && os.isFile(jarPath) then
      Some(Instant.ofEpochMilli(os.mtime(jarPath)))
    else
      None

  private def findMillHome(current: os.Path = os.pwd): os.Path =
    if os.exists(current / "build.mill") then
      current
    else if current == os.root then
      sys.error("Could not find mill project root with build.mill")
    else
      findMillHome(current / os.up)

  def buildFdswarmJar(): Unit =
    val millHome = findMillHome()
    logger.info("Killing existing mill processes to avoid conflicts...")
    os.proc("pkill", "-f", "mill")
      .call(cwd = millHome, check = false)
    logger.info("Proceeding with fdswarm assembly build.")
    val result = os.proc("./mill", "--no-daemon", "fdswarm.assembly")
      .call(
        timeout = 120000L,
        cwd = millHome,
        env = Map("MILL_OUTPUT_DIR" -> outDir.toString),
        check = false
      )

    val exitCode = result.exitCode
    if exitCode != 0 then
      val stdoutStr = result.out.text()
      val stderrStr = result.err.text()
      logger.info(
        s"""Mill build exit code $exitCode
           |STDOUT:
           |$stdoutStr
           |STDERR:
           |$stderrStr""".stripMargin
      )
    else
      logger.info(s"Mill fdswarm.assembly succeeded (output dir: $outDir)")