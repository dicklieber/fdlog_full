package fdswarm.logging

object LogFields:
  val callsign   = LogField[String]("callsign")
  val section    = LogField[String]("section")
  val band       = LogField[String]("band")
  val mode       = LogField[String]("mode")
  val bandMode   = LogField[String]("bandMode")
  val nodeId     = LogField[String]("nodeId")
  val traceId    = LogField[String]("traceId")
  val qsoId      = LogField[String]("qsoId")
  val contest    = LogField[String]("contest")
  val port       = LogField[Int]("port")
  val count      = LogField[Int]("count")
  val durationMs = LogField[Long]("durationMs")
  val success    = LogField[Boolean]("success")
