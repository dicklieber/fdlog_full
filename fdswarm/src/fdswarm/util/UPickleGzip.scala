package fdswarm.util

import upickle.default._
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.nio.charset.StandardCharsets
import scala.util.{Try, Using}
import cask.Response

/**
 * Helper to convert case classes (or any thing the [[ReadWriter]] trait can handle.
 */
object UPickleGzip:
  def encode[T: ReadWriter](value: T): Array[Byte] =
    val json = write(value)
    gzip(json.getBytes(StandardCharsets.UTF_8))

  def encodeResponse[T: ReadWriter](value: T): Response[Array[Byte]] =
    val gzippedData = encode(value)
    Response(
      data = gzippedData,
      statusCode = 200,
      headers = Seq("Content-Type" -> "application/json")
    )

  def decode[T: ReadWriter](bytes: Array[Byte]): T =
    val jsonBytes = gunzip(bytes)
    read[T](new String(jsonBytes, StandardCharsets.UTF_8))

  def decodeTry[T: ReadWriter](bytes: Array[Byte]): Try[T] =
    Try(decode[T](bytes))

  def gzip(input: Array[Byte]): Array[Byte] =
    Using.resource(new ByteArrayOutputStream()): bos =>
      Using.resource(new GZIPOutputStream(bos)): gos =>
        gos.write(input)
        gos.finish()
      bos.toByteArray

  def gunzip(input: Array[Byte]): Array[Byte] =
    Using.resource(new ByteArrayInputStream(input)): bis =>
      Using.resource(new GZIPInputStream(bis)): gis =>
        val bos = new ByteArrayOutputStream()
        val buffer = new Array[Byte](input.length * 2)
        var len = gis.read(buffer)
        while (len > -1) {
          bos.write(buffer, 0, len)
          len = gis.read(buffer)
        }
        bos.toByteArray