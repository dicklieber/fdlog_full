package fdlog

import os.{Path, read, write}
import scalatags.Text.all.*
import upickle.default.*
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}
import java.util.UUID

// ----- Data model -----

final case class NodeId(value: UUID)
final case class QsoId(nodeId: NodeId, seq: Long)

final case class Qso(
  id:            QsoId,
  time:          Instant,
  freqHz:        Long,
  band:          String,   // "40m"
  mode:          String,   // "CW", "SSB", "DIGI"
  myCall:        String,
  theirCall:     String,
  rstSent:       String,
  rstRcvd:       String,
  exchangeSent:  String,   // e.g. "2A IL"
  exchangeRcvd:  String,   // e.g. "3A WI"
  operator:      Option[String],
  notes:         Option[String]
)

final case class NodeMeta(
  nodeId:  NodeId,
  nextSeq: Long
)

final case class LogState(
  meta: NodeMeta,
  qsos: Map[QsoId, Qso]
)

// API payload when adding a QSO
final case class QsoInput(
  freqHz:        Long,
  band:          String,
  mode:          String,
  myCall:        String,
  theirCall:     String,
  rstSent:       String,
  rstRcvd:       String,
  exchangeSent:  String,
  exchangeRcvd:  String,
  operator:      Option[String],
  notes:         Option[String]
)

// For /api/peers
final case class PeersResponse(
  self:  String,
  peers: Seq[String]
)

// For /api/dupes JSON
final case class DupeGroupDto(
  band:      String,
  mode:      String,
  theirCall: String,
  qsoIds:    Seq[QsoId]
)

// ----- uPickle codecs -----

given uuidRW: ReadWriter[UUID] =
  readwriter[String].bimap[UUID](_.toString, UUID.fromString)

given instantRW: ReadWriter[Instant] =
  readwriter[Long].bimap[Instant](_.toEpochMilli, Instant.ofEpochMilli)

given nodeIdRW: ReadWriter[NodeId]       = macroRW
given qsoIdRW: ReadWriter[QsoId]         = macroRW
given qsoRW: ReadWriter[Qso]             = macroRW
given nodeMetaRW: ReadWriter[NodeMeta]   = macroRW
given logStateRW: ReadWriter[LogState]   = macroRW
given qsoInputRW: ReadWriter[QsoInput]   = macroRW
given peersResponseRW: ReadWriter[PeersResponse] = macroRW
given dupeGroupDtoRW: ReadWriter[DupeGroupDto]   = macroRW

// ----- Storage / snapshotting -----

object Storage {
  def loadOrInit(p: Path): LogState = {
    if os.exists(p) then
      val text = os.read(p)
      read[LogState](text)
    else
      LogState(
        meta = NodeMeta(NodeId(UUID.randomUUID()), nextSeq = 0L),
        qsos = Map.empty
      )
  }

  def writeSnapshot(p: Path, state: LogState): Unit = {
    val json = write(state, indent = 2)
    os.write.over(p, json, createFolders = true)
  }

  def startSnapshotter(store: QsoStore, path: Path, periodSeconds: Int): Unit = {
    val t = new Thread(() => {
      while true do
        try
          val s = store.getState()
          writeSnapshot(path, s)
        catch case _: Throwable => ()
        Thread.sleep(periodSeconds.toLong * 1000L)
    })
    t.setDaemon(true)
    t.start()
  }
}

// ----- In-memory store (thread-safe) -----

class QsoStore(initial: LogState) {

  @volatile private var state: LogState = initial
  private val lock = new AnyRef

  def getState(): LogState = state

  def all(): Seq[Qso] =
    state.qsos.values.toSeq.sortBy(_.time.toEpochMilli)

  def createLocalQso(input: QsoInput): Qso = {
    val (id, now) = lock.synchronized {
      val s = state
      val newId = QsoId(s.meta.nodeId, s.meta.nextSeq)
      val updatedMeta = s.meta.copy(nextSeq = s.meta.nextSeq + 1)
      state = s.copy(meta = updatedMeta)
      (newId, Instant.now())
    }

    val q = Qso(
      id             = id,
      time           = now,
      freqHz         = input.freqHz,
      band           = input.band,
      mode           = input.mode,
      myCall         = input.myCall,
      theirCall      = input.theirCall,
      rstSent        = input.rstSent,
      rstRcvd        = input.rstRcvd,
      exchangeSent   = input.exchangeSent,
      exchangeRcvd   = input.exchangeRcvd,
      operator       = input.operator,
      notes          = input.notes
    )

    lock.synchronized {
      val s = state
      state = s.copy(qsos = s.qsos + (q.id -> q))
    }

    q
  }

  // Merge remote QSOs (grow-only set)
  def merge(remote: Seq[Qso]): Unit = lock.synchronized {
    val s = state
    val merged = remote.foldLeft(s.qsos) { (m, q) =>
      if m.contains(q.id) then m else m + (q.id -> q)
    }
    state = s.copy(qsos = merged)
  }
}

// ----- Replicator -----

object Replicator {

  def start(
    store: QsoStore,
    peers: mutable.Set[String],
    selfUrl: String,
    periodSeconds: Int
  ): Unit = {
    val t = new Thread(() => {
      while true do
        try
          val peerList = peers.toList
          for peer <- peerList do
            // 1) pull log
            try
              val resp = requests.get(s"$peer/api/log/full", readTimeout = 10000)
              if resp.statusCode == 200 then
                val qsos = read[Seq[Qso]](resp.text())
                store.merge(qsos)
            catch case _: Throwable => ()

            // 2) peer exchange
            try
              val pResp = requests.get(s"$peer/api/peers", readTimeout = 5000)
              if pResp.statusCode == 200 then
                val parsed = read[PeersResponse](pResp.text())
                peers ++= parsed.peers.filter(url => url != selfUrl && url.nonEmpty)
            catch case _: Throwable => ()
        catch case _: Throwable => ()

        Thread.sleep(periodSeconds.toLong * 1000L)
    })

    t.setDaemon(true)
    t.start()
  }
}

// ----- ADIF helpers -----

object Adif {
  private val dateFmt =
    DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)
  private val timeFmt =
    DateTimeFormatter.ofPattern("HHmmss").withZone(ZoneOffset.UTC)

  private def field(name: String, value: String): String =
    if value == null || value.isEmpty then ""
    else s"<$name:${value.length}>$value"

  def toAdif(qs: Seq[Qso]): String = {
    val header =
      """Generated by FDLog (Scala)
        |<adif_ver:5>3.1.0
        |<programid:5>FDLog
        |<EOH>
        |""".stripMargin

    val body = qs.map { q =>
      val date = dateFmt.format(q.time)
      val time = timeFmt.format(q.time)

      val sb = new StringBuilder
      sb.append(field("CALL", q.theirCall))
      sb.append(field("STATION_CALLSIGN", q.myCall))
      sb.append(field("BAND", q.band))
      sb.append(field("MODE", q.mode))
      sb.append(field("RST_SENT", q.rstSent))
      sb.append(field("RST_RCVD", q.rstRcvd))
      sb.append(field("QSO_DATE", date))
      sb.append(field("TIME_ON", time))
      sb.append(field("APP_FDLOG_EXCH_SENT", q.exchangeSent))
      sb.append(field("APP_FDLOG_EXCH_RCVD", q.exchangeRcvd))
      q.operator.foreach(op => sb.append(field("OPERATOR", op)))
      q.notes.foreach(n => sb.append(field("APP_FDLOG_NOTES", n)))
      sb.append("<EOR>")
      sb.toString
    }.mkString

    header + body
  }
}

// ----- HTTP app (Cask) -----

object FdLogApp extends cask.MainRoutes {

  // Config: port and peers from env
  override def port: Int =
    sys.env.get("FDLOG_PORT").flatMap(_.toIntOption).getOrElse(8080)

  // For building our "self" URL (only used in /api/peers)
  private val hostAddr: String =
    sys.env.getOrElse("FDLOG_HOST",
      java.net.InetAddress.getLocalHost.getHostAddress
    )

  private val selfBaseUrl: String = s"http://$hostAddr:$port"

  // Seed peers via env: FDLOG_PEERS="http://10.0.0.10:8080,http://10.0.0.11:8080"
  private val peers: mutable.Set[String] =
    mutable.Set.from(
      sys.env.get("FDLOG_PEERS")
        .map(_.split(",").map(_.trim).filter(_.nonEmpty))
        .getOrElse(Array.empty[String])
    )

  private val snapshotPath: Path = os.pwd / "fdlog-snapshot.json"
  private val store = new QsoStore(Storage.loadOrInit(snapshotPath))

  // Start background snapshotter and replicator
  Storage.startSnapshotter(store, snapshotPath, periodSeconds = 10)
  Replicator.start(store, peers, selfBaseUrl, periodSeconds = 30)

  // ----- Dupe logic -----
  //
  // Unique contact key: (band, mode, theirCall)
  // First QSO is "valid", later ones dupes.

  private case class DupeKey(
    band:      String,
    mode:      String,
    theirCall: String
  )

  private def computeDupeInfo(qs: Seq[Qso]): (Seq[Qso], Set[QsoId], Seq[DupeGroupDto]) = {
    val grouped = qs.groupBy(q => DupeKey(q.band, q.mode, q.theirCall))

    var dupeIds = Set.empty[QsoId]
    val dupeGroups = mutable.Buffer.empty[DupeGroupDto]
    val uniques = mutable.Buffer.empty[Qso]

    grouped.foreach { case (key, list) =>
      val sorted = list.sortBy(_.time.toEpochMilli)
      sorted.headOption.foreach(uniques += _)
      if sorted.size > 1 then
        val dupes = sorted.drop(1)
        dupeIds ++= dupes.map(_.id)
        dupeGroups += DupeGroupDto(
          band      = key.band,
          mode      = key.mode,
          theirCall = key.theirCall,
          qsoIds    = dupes.map(_.id)
        )
    }

    (uniques.toSeq, dupeIds, dupeGroups.toSeq)
  }

  // ----- Routes -----

  // Simple HTML view of the log + quick-entry UI
  @cask.get("/")
  def index() = {

    val allQsos = store.all()
    val (uniqueQsos, dupeIds, _) = computeDupeInfo(allQsos)

    val rows = allQsos.map { q =>
      val dupeFlag = if dupeIds.contains(q.id) then "YES" else ""
      tr(
        if dupeFlag.nonEmpty then (backgroundColor := "#ffe0e0") else (),
        td(q.time.toString),
        td(q.band),
        td(q.mode),
        td(q.myCall),
        td(q.theirCall),
        td(q.exchangeSent),
        td(q.exchangeRcvd),
        td(dupeFlag)
      )
    }

    val total  = allQsos.size
    val unique = uniqueQsos.size
    val dupes  = total - unique

    cask.Response(
      data = doctype("html")(
        html(
          head(
            meta(charset := "utf-8"),
            tag("title")("Field Day Log")
          ),
          body(
            h1("Field Day Log"),
            p(s"Node: ${store.getState().meta.nodeId.value}"),
            p(s"Total QSOs: $total"),
            p(s"Unique QSOs (for scoring): $unique"),
            p(s"Duplicate QSOs: $dupes"),
            p(
              "ADIF export: ",
              code("/api/export/adif"),
              " (unique only), or ",
              code("/api/export/adif?includeDupes=true"),
              " (all contacts)."
            ),

            // --- Quick entry form + presets ---
            h2("Quick QSO Entry"),
            p(
              "Band/Mode presets: ",
              span(
                button(`type` := "button", onclick := "setBandMode('80m','CW')")("80m CW"),
                " ",
                button(`type` := "button", onclick := "setBandMode('40m','CW')")("40m CW"),
                " ",
                button(`type` := "button", onclick := "setBandMode('40m','SSB')")("40m SSB"),
                " ",
                button(`type` := "button", onclick := "setBandMode('20m','DIGI')")("20m DIGI"),
                " ",
                button(`type` := "button", onclick := "setBandMode('20m','SSB')")("20m SSB")
              ),
              br(),
              small("Keyboard shortcuts (when not in a field): 1=40m CW, 2=20m CW, 3=40m DIGI, 4=20m DIGI, 5=20m SSB")
            ),
            form(
              id := "qsoForm",
              onsubmit := "submitQso(); return false;",
              fieldset(
                legend("New QSO"),
                table(
                  tr(td("Freq (Hz):"), td(input(id:="freqHz", `type`:="number", required := true, value:="14074000"))),
                  tr(td("Band:"),      td(select(id:="band")(option("80m"), option("40m"), option("20m"), option("15m"), option("10m")))),
                  tr(td("Mode:"),      td(select(id:="mode")(option("CW"), option("SSB"), option("DIGI")))),
                  tr(td("My Call:"),   td(input(id:="myCall",    value:="WA9NNN", required := true))),
                  tr(td("Their Call:"),td(input(id:="theirCall", required := true))),
                  tr(td("RST Sent:"),  td(input(id:="rstSent",  value:="59", required := true))),
                  tr(td("RST Rcvd:"),  td(input(id:="rstRcvd",  value:="59", required := true))),
                  tr(td("Exch Sent:"), td(input(id:="exchangeSent",  value:="2A IL", required := true))),
                  tr(td("Exch Rcvd:"), td(input(id:="exchangeRcvd",  required := true))),
                  tr(td("Operator:"),  td(input(id:="operator",   value:="WA9NNN"))),
                  tr(td("Notes:"),     td(input(id:="notes")))
                ),
                p(
                  small("Enter-to-advance flow: Their Call → Exchange Rcvd → Notes → Submit")
                ),
                button(`type` := "submit")("Submit QSO")
              )
            ),

            script(raw("""
              function setBandMode(band, mode) {
                var b = document.getElementById("band");
                var m = document.getElementById("mode");
                if (b) {
                  for (var i = 0; i < b.options.length; i++) {
                    if (b.options[i].value === band) {
                      b.selectedIndex = i;
                      break;
                    }
                  }
                }
                if (m) {
                  for (var j = 0; j < m.options.length; j++) {
                    if (m.options[j].value === mode) {
                      m.selectedIndex = j;
                      break;
                    }
                  }
                }
              }

              function submitQso() {
                const freqVal = document.getElementById("freqHz").value;
                const body = {
                  freqHz:        freqVal ? parseInt(freqVal) : 0,
                  band:          document.getElementById("band").value,
                  mode:          document.getElementById("mode").value,
                  myCall:        document.getElementById("myCall").value,
                  theirCall:     document.getElementById("theirCall").value,
                  rstSent:       document.getElementById("rstSent").value,
                  rstRcvd:       document.getElementById("rstRcvd").value,
                  exchangeSent:  document.getElementById("exchangeSent").value,
                  exchangeRcvd:  document.getElementById("exchangeRcvd").value,
                  operator:      document.getElementById("operator").value,
                  notes:         document.getElementById("notes").value
                };

                fetch("/api/qso", {
                  method: "POST",
                  headers: {"Content-Type": "application/json"},
                  body: JSON.stringify(body)
                }).then(resp => {
                  if (resp.ok) {
                    // Clear key fields; keep My Call / Exch Sent
                    document.getElementById("theirCall").value = "";
                    document.getElementById("exchangeRcvd").value = "";
                    document.getElementById("notes").value = "";
                    // Reload to reflect dupes / counts
                    location.reload();
                  } else {
                    alert("Error submitting QSO");
                  }
                });
              }

              function setupQsoFormShortcuts() {
                // Enter-flow: theirCall -> exchangeRcvd -> notes -> submit
                var order = ["theirCall", "exchangeRcvd", "notes"];
                order.forEach(function(id, idx) {
                  var el = document.getElementById(id);
                  if (!el) return;
                  el.addEventListener("keydown", function(e) {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      if (idx === order.length - 1) {
                        submitQso();
                      } else {
                        var next = document.getElementById(order[idx + 1]);
                        if (next) next.focus();
                      }
                    }
                  });
                });

                // Global keyboard shortcuts for band/mode presets
                document.addEventListener("keydown", function(e) {
                  if (e.altKey || e.ctrlKey || e.metaKey) return;
                  // Ignore when typing in a field
                  var t = e.target;
                  if (t && (t.tagName === "INPUT" || t.tagName === "SELECT" || t.tagName === "TEXTAREA")) return;

                  switch (e.key) {
                    case "1": // 40m CW
                      setBandMode("40m", "CW");
                      break;
                    case "2": // 20m CW
                      setBandMode("20m", "CW");
                      break;
                    case "3": // 40m DIGI
                      setBandMode("40m", "DIGI");
                      break;
                    case "4": // 20m DIGI
                      setBandMode("20m", "DIGI");
                      break;
                    case "5": // 20m SSB
                      setBandMode("20m", "SSB");
                      break;
                  }
                });
              }

              window.addEventListener("load", function() {
                // Focus on Their Call at page load
                var tc = document.getElementById("theirCall");
                if (tc) tc.focus();
                setupQsoFormShortcuts();
              });
            """)),

            h2("QSOs"),
            table(border := 1)(
              thead(
                tr(
                  th("Time"),
                  th("Band"),
                  th("Mode"),
                  th("My Call"),
                  th("Their Call"),
                  th("Sent"),
                  th("Rcvd"),
                  th("Dupe")
                )
              ),
              tbody(rows)
            )
          )
        )
      ).render,
      headers = Seq("Content-Type" -> "text/html; charset=utf-8")
    )
  }

  // Append a QSO (JSON body)
  @cask.post("/api/qso")
  def addQso(request: cask.Request) = {
    try
      val input = read[QsoInput](request.text())
      val qso   = store.createLocalQso(input)
      cask.Response(
        data       = write(qso),
        statusCode = 201,
        headers    = Seq("Content-Type" -> "application/json")
      )
    catch
      case e: upickle.core.Abort =>
        cask.Response(
          data       = s"""{"error":"bad json: ${e.getMessage()}"}""",
          statusCode = 400,
          headers    = Seq("Content-Type" -> "application/json")
        )
  }

  // Full log as JSON (used by replicator)
  @cask.get("/api/log/full")
  def fullLog() =
    cask.Response(
      data    = write(store.all()),
      headers = Seq("Content-Type" -> "application/json")
    )

  // Merge QSOs (used if you want to push logs, not strictly needed)
  @cask.post("/api/log/merge")
  def mergeLog(request: cask.Request) = {
    val remote = read[Seq[Qso]](request.text())
    store.merge(remote)
    cask.Response(
      data    = """{"status":"ok"}""",
      headers = Seq("Content-Type" -> "application/json")
    )
  }

  // Peer list for discovery
  @cask.get("/api/peers")
  def getPeers() = {
    val payload = PeersResponse(self = selfBaseUrl, peers = peers.toSeq)
    cask.Response(
      data    = write(payload),
      headers = Seq("Content-Type" -> "application/json")
    )
  }

  // Add a peer by POSTing plain text or JSON string
  @cask.post("/api/peers/add")
  def addPeer(request: cask.Request) = {
    val body = request.text().trim
    if body.nonEmpty then peers += body
    cask.Response(
      data    = write(PeersResponse(selfBaseUrl, peers.toSeq)),
      headers = Seq("Content-Type" -> "application/json")
    )
  }

  // JSON summary of duplicates
  @cask.get("/api/dupes")
  def dupes() = {
    val allQsos = store.all()
    val (_, _, dupeGroups) = computeDupeInfo(allQsos)
    cask.Response(
      data    = write(dupeGroups),
      headers = Seq("Content-Type" -> "application/json")
    )
  }

  // ADIF export
  @cask.get("/api/export/adif")
  def exportAdif(includeDupes: Boolean = false) = {
    val allQsos = store.all()
    val (uniqueQsos, _, _) = computeDupeInfo(allQsos)
    val qs = if includeDupes then allQsos else uniqueQsos
    val adif = Adif.toAdif(qs)
    cask.Response(
      data    = adif,
      headers = Seq("Content-Type" -> "text/plain; charset=utf-8")
    )
  }

  initialize()
}
