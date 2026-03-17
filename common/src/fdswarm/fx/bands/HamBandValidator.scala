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

package fdswarm.fx.bands

object HamBandValidator:

  final case class Issue(kind: String, message: String)

  def validate(bands: Seq[HamBand]): Seq[Issue] =
    val issues = Vector.newBuilder[Issue]

    // ---- duplicates by name ----
    val dupes =
      bands.groupBy(_.bandName.trim.toLowerCase).collect { case (k, xs) if xs.size > 1 => (k, xs.size) }
    dupes.toSeq.sortBy(_._1).foreach { case (name, n) =>
      issues += Issue("DuplicateName", s"Band name '$name' appears $n times.")
    }

    // ---- range sanity ----
    bands.foreach { b =>
      if b.startFrequencyHz <= 0 || b.endFrequencyHz <= 0 then
        issues += Issue("InvalidRange", s"${b.bandName}: frequencies must be > 0 (got ${b.startFrequencyHz}..${b.endFrequencyHz}).")
      if b.startFrequencyHz >= b.endFrequencyHz then
        issues += Issue("InvalidRange", s"${b.bandName}: startFrequencyHz must be < endFrequencyHz (got ${b.startFrequencyHz}..${b.endFrequencyHz}).")
    }

    // ---- overlaps (within same bandClass) ----
    val byClass = bands.groupBy(_.bandClass)
    byClass.foreach { case (cls, xs) =>
      val sorted = xs.sortBy(_.startFrequencyHz)
      sorted.sliding(2).foreach {
        case Seq(a, b) =>
          // Overlap if b starts before a ends
          if b.startFrequencyHz < a.endFrequencyHz then
            issues += Issue(
              "Overlap",
              s"$cls overlap: '${a.bandName}' (${a.startFrequencyHz}-${a.endFrequencyHz}) overlaps '${b.bandName}' (${b.startFrequencyHz}-${b.endFrequencyHz})."
            )
        case _ => ()
      }
    }

    issues.result()