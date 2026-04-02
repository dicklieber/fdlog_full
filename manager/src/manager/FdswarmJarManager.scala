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
