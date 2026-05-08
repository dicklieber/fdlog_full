package fdswarm.logging

object LogEventFieldNames:
  val Timestamp = "@timestamp"
  val Level = "level"
  val Logger = "logger"
  val Message = "message"

  def topLevelStringFields(timestamp: String, level: String, logger: String, message: Option[String]): Seq[(String, String)] =
    Seq(
      Timestamp -> timestamp,
      Level -> level,
      Logger -> logger
    ) ++ message.map(
      Message -> _
    )

  def log4jEventTemplateWithFlattenedMdc: String =
    s"""{
      "$Timestamp": {"$$resolver": "timestamp"},
      "$Level": {"$$resolver": "level", "field": "name"},
      "$Logger": {"$$resolver": "logger", "field": "name"},
      "$Message": {"$$resolver": "message", "stringified": true},
      "mdc": {"$$resolver": "mdc", "flatten": true}
    }"""
