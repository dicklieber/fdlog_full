package fdswarm.logging

enum Locus(
            val value: String
          ):
  case UDP extends Locus("udp")
  case TCP extends Locus("tcp")
  case Qso extends Locus("qso")
  case Search extends Locus("search")

  case Replication extends Locus("Replication")
  case Sync extends Locus("SyncContest")
  case LogEntry extends Locus("LogEntry")
  case Startup extends Locus("Startup")
  case Metrics extends Locus("Metrics")
  case Scoring extends Locus("Scoring")
  case Transport extends Locus("Transport")
  case ClassName extends Locus("ClassName")
