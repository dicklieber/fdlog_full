
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

package fdswarm.model
import upickle.default.*

import scala.util.matching.Regex

/**
 * Holding stuff like 1H depends on field day type.
 *
 * @param transmitters how many.
 * @param stationClass
 */
case class FdClass(transmitters: Int = 1, classLetter: Char = 'I') derives ReadWriter:
  override def toString: String = s"$transmitters${classLetter}"

object FdClass:
  def appy(transmitters: Int, stationClass: StationClass): FdClass =
    FdClass(
      transmitters = transmitters,
      classLetter = stationClass.letter)

case class Exchange(fdClass: FdClass = FdClass(), sectionCode: String = "IL") derives ReadWriter:

  /**
   *
   * @return compact form
   */
  override def toString: String = s"""$fdClass $sectionCode"""

//
//object Exchange {
//  val classParser: Regex = """(\d+)([A-Z])""".r
//  private val Parse = """(\d*\p{Upper}) (.*)""".r
//
//  def apply(): Exchange = {
//
//    new Exchange("1O", Sections.defaultCode)
//  }
//
//  def apply(transmitters: Int, category: EntryCategory, section: Section): Exchange = {
//
//    new Exchange(category.buildClass(transmitters), section.code)
//  }
//
//  def apply(in: String): Exchange = {
//
//    in match {
//      case Parse(category, section) ⇒
//        Exchange(category, section)
//      case _ ⇒
//        throw new IllegalArgumentException(s"Can't parse exchange: $in")
//    }
//  }
//
//  def apply(category: String, section: String): Exchange = {
//
//    new Exchange(category.toUpperCase, section.toUpperCase)
//  }
//
//  /**
//   * to make JSON a bit more compact
//   */
//  implicit val exFormat: Format[Exchange] = new Format[Exchange] {
//    override def reads(json: JsValue): JsResult[Exchange] = {
//
//      val ss = json.as[String]
//      try {
//        ss match {
//          case Parse(category, section) ⇒
//            JsSuccess(Exchange(category, section))
//          case _ ⇒
//            JsError(s"Exchange: $ss could not be parsed!")
//        }
//      }
//      catch {
//        case e: IllegalArgumentException ⇒ JsError(e.getMessage)
//      }
//    }
//
//    override def writes(exchange: Exchange): JsValue = {
//
//      JsString(exchange.toString)
//    }
//  }
//}
//
