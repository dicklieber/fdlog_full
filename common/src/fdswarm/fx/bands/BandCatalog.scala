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
import com.typesafe.config.Config
import fdswarm.model.BandMode.Band
import fdswarm.model.{Choice, ChoiceItem}
import io.circe.parser.decode
import jakarta.inject.{Inject, Singleton}
/**
 * This is loaded from application.conf, defines all known radio bands.
 */
@Singleton
final class BandCatalog @Inject()(config: Config):
  private val key = "fdswarm.hamBands"

  val hamBands: Seq[HamBand] =
    decode[Seq[HamBand]](config.getValue(key).render(com.typesafe.config.ConfigRenderOptions.concise().setJson(true))).toTry.get




  
