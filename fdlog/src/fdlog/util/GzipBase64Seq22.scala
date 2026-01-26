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
import java.util.Base64
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

object GzipBase64Seq22:
  private val MaxIds = 10000
  private val ChunkSize = GenerateId.IdSize

  /** Encode a Seq[String] (each exactly ChunkSize chars) to a Base64-encoded GZIP string */
  def encode(parts: Seq[String]): String =
    require(
      parts.forall(_.length == ChunkSize),
      s"All input strings must be exactly $ChunkSize characters long"
    )

    // 1) Concatenate all pieces
    val concatenated = parts.mkString

    // 2) Gzip the UTF-8 bytes
    val compressed = gzip(concatenated.getBytes(StandardCharsets.UTF_8))

    // 3) Base64-encode
    Base64.getEncoder.encodeToString(compressed)

  /** Decode the Base64-encoded GZIP string back into a Seq[String] of 22-char strings */
  def decode(b64: String): Seq[String] =
    // 1) Base64-decode
    val compressed = Base64.getDecoder.decode(b64)

    // 2) Gunzip to bytes
    val decompressedBytes = gunzip(compressed)

    // 3) Convert back to String
    val concatenated = String(decompressedBytes, StandardCharsets.UTF_8)

    // 4) Split into 22-char chunks
    require(
      concatenated.length % ChunkSize == 0,
      s"Decompressed string length (${concatenated.length}) is not a multiple of $ChunkSize"
    )

    val builder = Seq.newBuilder[String]
    var i = 0
    while i < concatenated.length do
      builder += concatenated.substring(i, i + ChunkSize)
      i += ChunkSize

    builder.result()

  // --- internal helpers ---

  private def gzip(input: Array[Byte]): Array[Byte] =
    val bos  = new ByteArrayOutputStream()
    val gzip = new GZIPOutputStream(bos)
    try
      gzip.write(input)
    finally
      // close to flush all gzip data into bos
      gzip.close()
    bos.toByteArray

  private def gunzip(input: Array[Byte]): Array[Byte] =
    val gis  = new GZIPInputStream(new ByteArrayInputStream(input))
    val bos  = new ByteArrayOutputStream()
    val buf  = new Array[Byte](MaxIds)

    try
      var n = gis.read(buf)
      while n != -1 do
        bos.write(buf, 0, n)
        n = gis.read(buf)
    finally
      gis.close()

    bos.toByteArray