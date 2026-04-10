/*
 * Copyright (c) 2026. Dick Lieber, WA9NNN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package fdswarm.util

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import java.nio.charset.StandardCharsets
import scala.util.{Try, Using}

/**
 * Helper to convert case classes to/from gzipped JSON using Circe.
 */
object CirceGzip:
  def encode[T: Encoder](value: T): Array[Byte] =
    val json = value.asJson.noSpaces
    gzip(json.getBytes(StandardCharsets.UTF_8))

  def decode[T: Decoder](bytes: Array[Byte]): Either[Error, T] =
    val jsonBytes = gunzip(bytes)
    val sJson = new String(jsonBytes, StandardCharsets.UTF_8)
    parse(sJson).flatMap(_.as[T])

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
        val buffer = new Array[Byte](4096)
        var len = gis.read(buffer)
        while (len > -1) {
          bos.write(buffer, 0, len)
          len = gis.read(buffer)
        }
        bos.toByteArray
