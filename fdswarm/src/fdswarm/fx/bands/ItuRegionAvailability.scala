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

import io.circe.Codec

enum BandClass derives Codec.AsObject:
  case LF, VLF, MF, HF, VHF, UHF, SHF, EHF

enum ItuRegion derives Codec.AsObject:
  case ALL, R1, R2, R3


/**
 * These are loaded from application.conf.
 *
 * @param bandName 30m, 40m, 80m, etc. Shown in UI.
 * @param startFrequencyHz used to map radio frequencies to [[HamBand]]
 * @param endFrequencyHz used to map radio frequencies to [[HamBand]]
 * @param bandClass common user-friendly name for band, e.g. "VHF"
 * @param regions where in the world this band is available.
 */
final case class HamBand(
                          bandName: String,
                          startFrequencyHz: Long,
                          endFrequencyHz: Long,
                          bandClass: BandClass,
                          regions: Set[ItuRegion] = Set(ItuRegion.ALL)
                        ) derives Codec.AsObject
