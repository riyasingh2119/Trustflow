#!/usr/bin/env bash
# Single-server deployment: pull the image, extract the jar from it and restart the systemd service.
#   sudo ./scripts/deploy.sh riyasingh/trustflow-backend:42
# For Kubernetes use the Jenkins pipeline (kubectl set image) instead.
set -euo pipefail
[[ $EUID -eq 0 ]] || { echo "Run as root (sudo)." >&2; exit 1; }
IMAGE="${1:?usage: deploy.sh <image:tag>}"
APP_DIR=/opt/trustflow
BACKUP="$APP_DIR/app.jar.prev"

echo ">> pulling $IMAGE"
docker pull "$IMAGE"

echo ">> extracting jar"
CID=$(docker create "$IMAGE")
trap 'docker rm -f "$CID" >/dev/null 2>&1 || true' EXIT
docker cp "$CID:/app/app.jar" /tmp/app.jar.new

[[ -f "$APP_DIR/app.jar" ]] && cp -p "$APP_DIR/app.jar" "$BACKUP"
install -m 640 -o trustflow -g trustflow /tmp/app.jar.new "$APP_DIR/app.jar"
rm -f /tmp/app.jar.new

echo ">> restarting service"
systemctl restart trustflow-backend

for i in $(seq 1 30); do
  if curl -fs http://localhost:8080/actuator/health/readiness | grep -q '"UP"'; then
    echo "Healthy after ${i}x2 seconds. Deployed $IMAGE"; exit 0
  fi
  sleep 2
done

echo "Health check failed, rolling back." >&2
if [[ -f "$BACKUP" ]]; then
  install -m 640 -o trustflow -g trustflow "$BACKUP" "$APP_DIR/app.jar"
  systemctl restart trustflow-backend
fi
journalctl -u trustflow-backend -n 30 --no-pager || true
exit 1
