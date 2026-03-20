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

  def buildFdswarmJar(): Unit =
    val result = os.proc("./mill", "fdswarm.assembly")
      .call(
        cwd = os.pwd,
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