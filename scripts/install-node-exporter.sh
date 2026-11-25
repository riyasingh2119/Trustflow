#!/usr/bin/env bash
# Installs Node Exporter as a systemd service (lab 8). Run as root on every server.
set -euo pipefail
[[ $EUID -eq 0 ]] || { echo "Run as root." >&2; exit 1; }
VERSION="${NODE_EXPORTER_VERSION:-1.8.2}"
ARCH="linux-amd64"
cd /tmp
curl -fsSLO "https://github.com/prometheus/node_exporter/releases/download/v${VERSION}/node_exporter-${VERSION}.${ARCH}.tar.gz"
curl -fsSLO "https://github.com/prometheus/node_exporter/releases/download/v${VERSION}/sha256sums.txt"
grep "node_exporter-${VERSION}.${ARCH}.tar.gz" sha256sums.txt | sha256sum -c -
tar xzf "node_exporter-${VERSION}.${ARCH}.tar.gz"
install -m 755 "node_exporter-${VERSION}.${ARCH}/node_exporter" /usr/local/bin/node_exporter
id node_exporter &>/dev/null || useradd --system --no-create-home --shell /usr/sbin/nologin node_exporter
install -m 644 "$(dirname "$0")/systemd/node_exporter.service" /etc/systemd/system/node_exporter.service
systemctl daemon-reload
systemctl enable --now node_exporter
systemctl --no-pager status node_exporter | head -5
echo "Metrics: curl -s localhost:9100/metrics | head"
