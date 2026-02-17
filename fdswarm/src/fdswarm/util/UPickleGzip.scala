package fdswarm.util

import upickle.default._
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.nio.charset.StandardCharsets
import scala.util.Try

object UPickleGzip:
  def encode[T: ReadWriter](value: T): Array[Byte] =
    val json = write(value)
    gzip(json.getBytes(StandardCharsets.UTF_8))

  def decode[T: ReadWriter](bytes: Array[Byte]): T =
    val jsonBytes = gunzip(bytes)
    read[T](new String(jsonBytes, StandardCharsets.UTF_8))

  def decodeTry[T: ReadWriter](bytes: Array[Byte]): Try[T] =
    Try(decode[T](bytes))

  def gzip(input: Array[Byte]): Array[Byte] =
    val bos = new ByteArrayOutputStream()
    val gos = new GZIPOutputStream(bos)
    gos.write(input)
    gos.close()
    bos.toByteArray

  def gunzip(input: Array[Byte]): Array[Byte] =
    val bis = new ByteArrayInputStream(input)
    val gis = new GZIPInputStream(bis)
    val bos = new ByteArrayOutputStream()

    val buffer = new Array[Byte](input.length * 2)
    var len = gis.read(buffer)
    while (len > -1) {
      bos.write(buffer, 0, len)
      len = gis.read(buffer)
    }

    gis.close()
    bos.toByteArray