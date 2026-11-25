#!/usr/bin/env bash
# Load test of the gateway endpoint (autoscaling evidence). Needs `hey` (https://github.com/rakyll/hey) or k6.
#   BASE_URL=http://trustflow.local ./scripts/loadtest.sh [duration] [concurrency]
# Watch in other terminals:  kubectl get hpa -n trustflow -w     and the Grafana security dashboard.
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
DURATION="${1:-120s}"
CONC="${2:-50}"
USER_NAME="${TF_USER:-dev}"
PASS="${TF_PASSWORD:-Passw0rd!}"

TOKEN=$(curl -fsS -X POST "$BASE_URL/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER_NAME\",\"password\":\"$PASS\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
BODY='{"resourceId":1,"action":"READ","simulatedHour":11}'

if command -v hey >/dev/null; then
  hey -z "$DURATION" -c "$CONC" -m POST \
    -H "Authorization: Bearer $TOKEN" -H "X-Device-Id: laptop-managed-01" -H "Content-Type: application/json" \
    -d "$BODY" "$BASE_URL/api/gateway/access"
elif command -v k6 >/dev/null; then
  cat > /tmp/tf-k6.js <<JS
import http from 'k6/http';
export const options = { vus: $CONC, duration: '$DURATION' };
export default function () {
  http.post('$BASE_URL/api/gateway/access', '$BODY', { headers: {
    'Authorization': 'Bearer $TOKEN', 'X-Device-Id': 'laptop-managed-01', 'Content-Type': 'application/json' } });
}
JS
  k6 run /tmp/tf-k6.js
else
  echo "Install 'hey' (go install github.com/rakyll/hey@latest) or k6 first." >&2; exit 1
fi
echo "Note: the token lives 15 minutes; keep the run shorter or log in again."
