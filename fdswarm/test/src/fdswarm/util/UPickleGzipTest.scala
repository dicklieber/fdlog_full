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

import munit.FunSuite
import upickle.default.*
import java.nio.charset.StandardCharsets
import scala.util.Success
import scala.util.Failure

case class TestData(name: String, age: Int) derives ReadWriter

class UPickleGzipTest extends FunSuite:

  test("gzip and gunzip are complementary"):
    val original = "Hello, World! This is a test string to be compressed.".getBytes(StandardCharsets.UTF_8)
    val compressed = UPickleGzip.gzip(original)
    val decompressed = UPickleGzip.gunzip(compressed)
    
    assert(compressed.length > 0)
    assertEquals(new String(decompressed, StandardCharsets.UTF_8), "Hello, World! This is a test string to be compressed.")

  test("gzip and gunzip handle empty byte array"):
    val original = Array.empty[Byte]
    val compressed = UPickleGzip.gzip(original)
    val decompressed = UPickleGzip.gunzip(compressed)
    
    assertEquals(decompressed.length, 0)

  test("encode and decode are complementary"):
    val original = TestData("Junie", 2)
    val encoded = UPickleGzip.encode(original)
    val decoded = UPickleGzip.decode[TestData](encoded)
    
    assertEquals(decoded, original)

  test("decodeTry handles valid data"):
    val original = TestData("Junie", 2)
    val encoded = UPickleGzip.encode(original)
    val result = UPickleGzip.decodeTry[TestData](encoded)
    
    assertEquals(result, Success(original))

  test("decodeTry handles invalid data"):
    val invalidBytes = "Not gzipped data".getBytes(StandardCharsets.UTF_8)
    val result = UPickleGzip.decodeTry[TestData](invalidBytes)
    
    assert(result.isFailure)

  test("gunzip handles larger data (expanding buffer)"):
    // UPickleGzip.gunzip uses a buffer size of input.length * 2
    // Let's create a string that is highly compressible but expands to more than 2x its compressed size
    val longString = "A" * 10000
    val original = longString.getBytes(StandardCharsets.UTF_8)
    val compressed = UPickleGzip.gzip(original)
    val decompressed = UPickleGzip.gunzip(compressed)
    
    assertEquals(new String(decompressed, StandardCharsets.UTF_8), longString)
    assert(compressed.length < original.length / 10) // Should be highly compressed
    assert(original.length > compressed.length * 2) // Expanded size should be > 2 * compressed size
