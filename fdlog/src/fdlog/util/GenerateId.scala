/*
 * Copyright (c) 2025-2026. Dick Lieber, WA9NNN
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

import org.apache.commons.lang3.Conversion

import java.util
import java.util.concurrent.atomic.AtomicInteger
import java.util.{Base64, UUID}

/**
 * An ID based on UUID but in a compact form.
 */
object GenerateId:
  /**
   * Unit tests can set to true to get sequential, predictable Ids.
   */
  private var sequentialIds: Option[AtomicInteger] = None

  def useSeqentialStartingAt(start: Int = 0): Unit =
    sequentialIds = Some(new AtomicInteger(start))

  def revertToRandom(): Unit =
    sequentialIds = None

  /**
   * Create a compact, url-safe, representation of a UUID.
   *
   * @return
   */
  def generate(): String =
    sequentialIds match
      case Some(sequentialIds) =>
        sequentialIds.getAndIncrement().toString
      case None =>
        nextRandom

  private def nextRandom =
    val uuid = UUID.randomUUID
    val bytes = Conversion.uuidToByteArray(uuid, new Array[Byte](16), 0, 16)
    val encoder1 = Base64.getUrlEncoder.withoutPadding()
    encoder1.encodeToString(bytes)

