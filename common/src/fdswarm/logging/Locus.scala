package fdswarm.logging

enum Locus(
            val value: String
          ):
  case Replication extends Locus("Replication")
  case Sync extends Locus("SyncContest")
  case LogEntry extends Locus("LogEntry")
  case Startup extends Locus("Startup")
  case Metrics extends Locus("Metrics")
  case Scoring extends Locus("Scoring")
  case Transport extends Locus("Transport")
  case ClassName extends Locus("ClassName")
