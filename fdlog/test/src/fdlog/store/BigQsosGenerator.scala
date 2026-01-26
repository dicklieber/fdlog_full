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

package fdlog.store

import fdlog.model.{BandMode, Exchange, FdClass, Qso, QsoMetadata}

object BigQsosGenerator:
  def qsos(howMany: Int = 100): Iterator[Qso]=
    val exchange = Exchange(FdClass(1, 'I'), "IL")
    val bandMode = BandMode("20M", "PH")
    callsignIterator(howMany)
      .map(callSign => Qso(callSign, exchange, bandMode, QsoMetadata()))


  def callsignIterator(howMany: Int = 10, prefix: String = "WA9"): Iterator[String] =
    val i: Iterator[String] = for
      a <- Iterator.range('A', 'Z' + 1)
      b <- Iterator.range('A', 'Z' + 1)
      c <- Iterator.range('A', 'Z' + 1)
    yield s"$prefix${a.toChar}${b.toChar}${c.toChar}"
    i.take(howMany)
      
    
