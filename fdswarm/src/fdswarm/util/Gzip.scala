package fdswarm.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import scala.util.Using

object Gzip:

  def compress(in: Array[Byte]): Array[Byte] =
    val bos = new ByteArrayOutputStream(in.length)

    Using.resource(new GZIPOutputStream(bos)) { gzip =>
      gzip.write(in)
    }

    bos.toByteArray

  def decompress(in: Array[Byte]): Array[Byte] =
    Using.resource(
      new GZIPInputStream(
        new ByteArrayInputStream(in)
      )
    ) { gis =>
      gis.readAllBytes()
    }