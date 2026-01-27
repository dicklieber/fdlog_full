package fdlog.replication

import upickle.default.*

case class NodeState(idsStr: String) derives ReadWriter