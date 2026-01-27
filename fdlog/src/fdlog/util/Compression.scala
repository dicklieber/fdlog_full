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

package fdlog.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

object Compression:
  def gzipString(s: String): Array[Byte] =
    val bos = ByteArrayOutputStream()
    val gzip = GZIPOutputStream(bos)
    gzip.write(s.getBytes(StandardCharsets.UTF_8))
    gzip.close() // important: flushes final gzip footer
    bos.toByteArray
//  def gzip(data: Array[Byte]): Array[Byte] =
//    val baos = new ByteArrayOutputStream()
//    val g    = new GZIPOutputStream(baos)
//    g.write(data)
//    g.finish()
//    g.close()
//    baos.toByteArray

  def gunzip(data: Array[Byte]): Array[Byte] =
    val bais = new ByteArrayInputStream(data)
    val g    = new GZIPInputStream(bais)
    val buf  = new Array[Byte](Ids.IdSize * 10000)//todo big enough??
    val baos = new ByteArrayOutputStream()
    var n: Int = g.read(buf)
    while n != -1 do
      baos.write(buf, 0, n)
      n = g.read(buf)
    g.close()
    baos.toByteArray
