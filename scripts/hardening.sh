#!/usr/bin/env bash
# Baseline hardening for the TrustFlow servers (Ubuntu Server 22.04). Run as root:
#   sudo ADMIN_IP=203.0.113.7 ./scripts/hardening.sh
# Idempotent. Prints a before and after snapshot for the report.
set -euo pipefail

[[ $EUID -eq 0 ]] || { echo "Run as root (sudo)." >&2; exit 1; }
ADMIN_IP="${ADMIN_IP:-}"
DEPLOY_USER="${DEPLOY_USER:-deploy}"
SERVICE_USER="${SERVICE_USER:-trustflow}"
export DEBIAN_FRONTEND=noninteractive

snapshot() {
  echo "----- $1 -----"
  echo "[sshd]";   sshd -T 2>/dev/null | grep -E '^(permitrootlogin|passwordauthentication|maxauthtries|x11forwarding) ' || true
  echo "[ufw]";    ufw status verbose 2>/dev/null | head -15 || true
  echo "[listen]"; ss -tlnp | awk 'NR==1 || /LISTEN/' | head -20
  echo "[updates]"; systemctl is-enabled unattended-upgrades 2>/dev/null || echo "unattended-upgrades: not enabled"
  echo "[fail2ban]"; systemctl is-active fail2ban 2>/dev/null || echo "inactive"
}

snapshot "BEFORE"

echo ">> packages"
apt-get update -y
apt-get install -y ufw fail2ban unattended-upgrades logrotate curl

echo ">> users"
id "$DEPLOY_USER" &>/dev/null || adduser --disabled-password --gecos "" "$DEPLOY_USER"
usermod -aG sudo "$DEPLOY_USER"
if [[ -f /root/.ssh/authorized_keys || -f /home/ubuntu/.ssh/authorized_keys ]] && [[ ! -s /home/$DEPLOY_USER/.ssh/authorized_keys ]]; then
  install -d -m 700 -o "$DEPLOY_USER" -g "$DEPLOY_USER" "/home/$DEPLOY_USER/.ssh"
  cat /home/ubuntu/.ssh/authorized_keys 2>/dev/null > "/home/$DEPLOY_USER/.ssh/authorized_keys" || true
  chown "$DEPLOY_USER:$DEPLOY_USER" "/home/$DEPLOY_USER/.ssh/authorized_keys"; chmod 600 "/home/$DEPLOY_USER/.ssh/authorized_keys"
fi
id "$SERVICE_USER" &>/dev/null || useradd --system --home /opt/trustflow --shell /usr/sbin/nologin "$SERVICE_USER"

echo ">> ssh (key-only, no root login)"
if [[ ! -s /home/$DEPLOY_USER/.ssh/authorized_keys && ! -s /home/ubuntu/.ssh/authorized_keys ]]; then
  echo "WARNING: no authorized SSH key found for $DEPLOY_USER or ubuntu; skipping SSH lock-down to avoid locking you out." >&2
else
  cat > /etc/ssh/sshd_config.d/99-trustflow.conf <<'CONF'
PermitRootLogin no
PasswordAuthentication no
KbdInteractiveAuthentication no
PubkeyAuthentication yes
MaxAuthTries 3
X11Forwarding no
ClientAliveInterval 300
ClientAliveCountMax 2
CONF
  sshd -t && systemctl reload ssh
fi

echo ">> firewall (default deny incoming)"
ufw default deny incoming
ufw default allow outgoing
if [[ -n "$ADMIN_IP" ]]; then
  ufw allow from "$ADMIN_IP" to any port 22 proto tcp comment 'ssh admin'
else
  echo "WARNING: ADMIN_IP not set, allowing SSH from anywhere. Set ADMIN_IP to restrict it." >&2
  ufw allow 22/tcp comment 'ssh'
fi
ufw allow 80/tcp comment 'http'
ufw allow 443/tcp comment 'https'
# Internal ports only from the VPC range
for p in 9090 3000 9100 5432; do ufw allow from 10.0.0.0/16 to any port "$p" proto tcp comment "internal $p"; done
ufw --force enable

echo ">> fail2ban"
cat > /etc/fail2ban/jail.d/trustflow.local <<'CONF'
[sshd]
enabled  = true
maxretry = 4
findtime = 10m
bantime  = 1h
CONF
systemctl enable --now fail2ban
systemctl restart fail2ban

echo ">> automatic security updates"
cat > /etc/apt/apt.conf.d/20auto-upgrades <<'CONF'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
CONF
systemctl enable --now unattended-upgrades

echo ">> kernel network hardening"
cat > /etc/sysctl.d/99-trustflow.conf <<'CONF'
net.ipv4.conf.all.rp_filter = 1
net.ipv4.conf.all.accept_redirects = 0
net.ipv4.conf.all.send_redirects = 0
net.ipv4.tcp_syncookies = 1
net.ipv4.icmp_echo_ignore_broadcasts = 1
CONF
sysctl --system >/dev/null

echo ">> directories and permissions"
install -d -m 750 -o "$SERVICE_USER" -g "$SERVICE_USER" /opt/trustflow /var/log/trustflow
install -d -m 750 -o root -g "$SERVICE_USER" /etc/trustflow
[[ -f /etc/trustflow/trustflow.env ]] && chmod 600 /etc/trustflow/trustflow.env && chown "$SERVICE_USER":"$SERVICE_USER" /etc/trustflow/trustflow.env || true

echo ">> logrotate"
install -m 644 "$(dirname "$0")/etc/logrotate-trustflow" /etc/logrotate.d/trustflow

snapshot "AFTER"
echo "Hardening complete."
