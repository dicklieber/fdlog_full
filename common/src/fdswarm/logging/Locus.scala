package fdswarm.logging

enum Locus(
            val value: String
          ):
  case Replication extends Locus("Replication")
  case LogEntry extends Locus("LogEntry")
  case Startup extends Locus("Startup")
  case Metrics extends Locus("Metrics")
  case ClassName extends Locus("ClassName")
