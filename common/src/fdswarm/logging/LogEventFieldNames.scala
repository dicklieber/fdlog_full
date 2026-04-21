package fdswarm.logging

object LogEventFieldNames:
  val Timestamp = "@timestamp"
  val Level = "level"
  val Locus = "locus"
  val Message = "message"

  def topLevelStringFields(
                            timestamp: String,
                            level: String,
                            locus: String,
                            message: Option[String]
                          ): Seq[(String, String)] =
    Seq(
      Timestamp -> timestamp,
      Level -> level,
      Locus -> locus
    ) ++ message.map(
      Message -> _
    )

  def log4jEventTemplateWithFlattenedMdc: String =
    s"""{
      "$Timestamp": {"$$resolver": "timestamp"},
      "$Level": {"$$resolver": "level", "field": "name"},
      "$Locus": {"$$resolver": "logger", "field": "name"},
      "$Message": {"$$resolver": "message", "stringified": true},
      "mdc": {"$$resolver": "mdc", "flatten": true}
    }"""
