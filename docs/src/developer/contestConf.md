# Contest Configuration
All nodes need to be configured with the following parameters:

```aiignore
/**
 * @param transmitters number of transmitters
 * @param ourCallsign  our callsign
 * @param ourClass     our class
 * @param ourSection   our section
 * @param contestType WFD or ARRL
 */

case class ContestConfig(
    contestType: ContestType,
    ourCallsign: Callsign,
    transmitters: Int = 1,
    ourClass: String,
    ourSection: String)
 
```

The ContestConfig is used primarily in the Qso entry panel to indicate what classes are allowed. (They are different for WFD and ARRL).
and in the exchange display:
```
We are 1H IL
```
