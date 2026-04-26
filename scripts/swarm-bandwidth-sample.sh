#!/usr/bin/env bash
set -euo pipefail

# Sample fdswarm UDP broadcast counters from /metrics and estimate rates.
# Defaults target manager-launched ports 8080-8099 on localhost.

HOST="${HOST:-127.0.0.1}"
PORT_START="${PORT_START:-8080}"
PORT_END="${PORT_END:-8099}"
INTERVAL_SEC="${INTERVAL_SEC:-2}"

if [[ $# -gt 0 ]]; then
  PORTS=("$@")
else
  PORTS=()
  for ((p=PORT_START; p<=PORT_END; p++)); do
    PORTS+=("$p")
  done
fi

fetch_metrics() {
  local port="$1"
  curl -fsS "http://${HOST}:${port}/metrics" 2>/dev/null || true
}

metric_value() {
  local body="$1"
  local base="$2"
  # Handle dropwizard-prometheus naming with optional prefixes/suffixes.
  # Example emitted name:
  # fdswarm_replication_BroadcastTransport_udp_broadcast_bytes_sent_total
  awk -v b="$base" '
    $1 ~ ("(^|_)" b "(_total|_count)?($|\\{)") {
      print $2
      exit
    }
  ' <<<"$body"
}

declare -A s1_sent_bytes s1_recv_bytes s1_sent_pkts s1_recv_pkts
sample1_live_endpoints=0
sample1_metric_endpoints=0

gather_sample() {
  local -n out_sent_bytes="$1"
  local -n out_recv_bytes="$2"
  local -n out_sent_pkts="$3"
  local -n out_recv_pkts="$4"

  for port in "${PORTS[@]}"; do
    local body
    body="$(fetch_metrics "$port")"
    [[ -z "$body" ]] && continue
    sample1_live_endpoints=$((sample1_live_endpoints + 1))

    local sent_b recv_b sent_p recv_p
    sent_b="$(metric_value "$body" "udp_broadcast_bytes_sent")"
    recv_b="$(metric_value "$body" "udp_broadcast_bytes_received")"
    sent_p="$(metric_value "$body" "udp_broadcast_packets_sent")"
    recv_p="$(metric_value "$body" "udp_broadcast_packets_received")"

    # Require all 4 metrics to consider this instance healthy for sampling.
    [[ -z "$sent_b" || -z "$recv_b" || -z "$sent_p" || -z "$recv_p" ]] && continue
    sample1_metric_endpoints=$((sample1_metric_endpoints + 1))

    out_sent_bytes["$port"]="$sent_b"
    out_recv_bytes["$port"]="$recv_b"
    out_sent_pkts["$port"]="$sent_p"
    out_recv_pkts["$port"]="$recv_p"
  done
}

gather_sample s1_sent_bytes s1_recv_bytes s1_sent_pkts s1_recv_pkts
sleep "$INTERVAL_SEC"

declare -A s2_sent_bytes s2_recv_bytes s2_sent_pkts s2_recv_pkts
gather_sample s2_sent_bytes s2_recv_bytes s2_sent_pkts s2_recv_pkts

printf "%-6s %12s %12s %12s %12s\n" "PORT" "TX_B/s" "RX_B/s" "TX_pkt/s" "RX_pkt/s"
printf "%-6s %12s %12s %12s %12s\n" "------" "------------" "------------" "------------" "------------"

total_tx_bps=0
total_rx_bps=0
total_tx_pps=0
total_rx_pps=0
rows_printed=0

for port in "${PORTS[@]}"; do
  [[ -z "${s1_sent_bytes[$port]:-}" || -z "${s2_sent_bytes[$port]:-}" ]] && continue

  d_tx_b=$(( s2_sent_bytes[$port] - s1_sent_bytes[$port] ))
  d_rx_b=$(( s2_recv_bytes[$port] - s1_recv_bytes[$port] ))
  d_tx_p=$(( s2_sent_pkts[$port] - s1_sent_pkts[$port] ))
  d_rx_p=$(( s2_recv_pkts[$port] - s1_recv_pkts[$port] ))

  tx_bps=$(awk -v d="$d_tx_b" -v t="$INTERVAL_SEC" 'BEGIN { printf "%.1f", d / t }')
  rx_bps=$(awk -v d="$d_rx_b" -v t="$INTERVAL_SEC" 'BEGIN { printf "%.1f", d / t }')
  tx_pps=$(awk -v d="$d_tx_p" -v t="$INTERVAL_SEC" 'BEGIN { printf "%.2f", d / t }')
  rx_pps=$(awk -v d="$d_rx_p" -v t="$INTERVAL_SEC" 'BEGIN { printf "%.2f", d / t }')

  printf "%-6s %12s %12s %12s %12s\n" "$port" "$tx_bps" "$rx_bps" "$tx_pps" "$rx_pps"
  rows_printed=$((rows_printed + 1))

  total_tx_bps=$(awk -v a="$total_tx_bps" -v b="$tx_bps" 'BEGIN { printf "%.1f", a + b }')
  total_rx_bps=$(awk -v a="$total_rx_bps" -v b="$rx_bps" 'BEGIN { printf "%.1f", a + b }')
  total_tx_pps=$(awk -v a="$total_tx_pps" -v b="$tx_pps" 'BEGIN { printf "%.2f", a + b }')
  total_rx_pps=$(awk -v a="$total_rx_pps" -v b="$rx_pps" 'BEGIN { printf "%.2f", a + b }')
done

printf "\n%-6s %12s %12s %12s %12s\n" "TOTAL" "$total_tx_bps" "$total_rx_bps" "$total_tx_pps" "$total_rx_pps"

if [[ "$sample1_live_endpoints" -eq 0 ]]; then
  echo "NOTE: No /metrics endpoints responded on the requested ports."
elif [[ "$sample1_metric_endpoints" -eq 0 ]]; then
  echo "NOTE: /metrics responded, but udp_broadcast_* metrics were not present."
  echo "      You may be running an older jar that does not include the new counters."
elif [[ "$rows_printed" -eq 0 ]]; then
  echo "NOTE: Counters were found, but no two-point sample rows were produced."
fi
