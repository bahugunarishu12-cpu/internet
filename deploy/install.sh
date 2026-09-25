#!/usr/bin/env bash
# ==============================================================================
# Network & Chrome Activity Monitor - Production Server Installer
#
# Usage:
#   sudo bash deploy/install.sh
# ==============================================================================

set -euo pipefail

# Color formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}===================================================================${NC}"
echo -e "${CYAN}    Network & Chrome Activity Monitor - Production Installer       ${NC}"
echo -e "${BLUE}===================================================================${NC}"

# 1. Verify root privileges
if [ "${EUID:-$(id -u)}" -ne 0 ]; then
    echo -e "${RED}[ERROR] This installer must be run as root.${NC}"
    echo -e "Please re-run with: ${YELLOW}sudo bash $0${NC}"
    exit 1
fi

# 2. Determine project and backend directories dynamically (NO hardcoded paths)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PARENT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

if [ -f "${PARENT_DIR}/backend/main.py" ]; then
    BACKEND_DIR="${PARENT_DIR}/backend"
elif [ -f "${PARENT_DIR}/project/backend/main.py" ]; then
    BACKEND_DIR="${PARENT_DIR}/project/backend"
elif [ -f "${SCRIPT_DIR}/../project/backend/main.py" ]; then
    BACKEND_DIR="${SCRIPT_DIR}/../project/backend"
elif [ -f "/project/backend/main.py" ]; then
    BACKEND_DIR="/project/backend"
else
    echo -e "${RED}[ERROR] Could not locate backend/main.py relative to script location (${SCRIPT_DIR}).${NC}"
    exit 1
fi

echo -e "${GREEN}[INFO] Detected backend directory: ${BACKEND_DIR}${NC}"

# 3. Check and install package prerequisites
echo -e "${CYAN}[1/6] Checking system package prerequisites...${NC}"
if command -v apt-get >/dev/null 2>&1; then
    export DEBIAN_FRONTEND=noninteractive
    apt-get update -qq || true
    apt-get install -y -qq python3 python3-venv python3-pip curl net-tools iproute2 >/dev/null 2>&1 || true
elif command -v dnf >/dev/null 2>&1; then
    dnf install -y -q python3 python3-pip curl net-tools iproute >/dev/null 2>&1 || true
elif command -v yum >/dev/null 2>&1; then
    yum install -y -q python3 python3-pip curl net-tools iproute >/dev/null 2>&1 || true
fi

# 4. Check & configure local MongoDB service
echo -e "${CYAN}[2/6] Configuring MongoDB database service...${NC}"
MONGO_FOUND=false
for svc in mongod mongodb; do
    if systemctl list-unit-files 2>/dev/null | grep -E -q "^${svc}\.service"; then
        echo -e "${GREEN}[INFO] Found local MongoDB service: ${svc}.service${NC}"
        systemctl enable "${svc}.service" >/dev/null 2>&1 || true
        systemctl start "${svc}.service" >/dev/null 2>&1 || true
        MONGO_FOUND=true
        break
    fi
done

if [ "$MONGO_FOUND" = false ]; then
    echo -e "${YELLOW}[NOTICE] No local systemd MongoDB service found.${NC}"
    echo -e "If running MongoDB via Docker or remote host, ensure MONGODB_URL in .env is reachable:"
    echo -e "  Docker example: ${CYAN}docker run -d -p 27017:27017 --name mongo-netmon -v mongo_data:/data/db mongo:latest${NC}"
fi

# 5. Set up Python virtual environment and dependencies
echo -e "${CYAN}[3/6] Setting up isolated Python virtual environment...${NC}"
VENV_DIR="${BACKEND_DIR}/venv"
if [ ! -d "${VENV_DIR}" ]; then
    echo -e "Creating virtual environment at ${VENV_DIR}..."
    python3 -m venv "${VENV_DIR}"
fi

echo -e "Installing and updating Python dependencies..."
"${VENV_DIR}/bin/pip" install --upgrade pip -q
"${VENV_DIR}/bin/pip" install -r "${BACKEND_DIR}/requirements.txt" -q

# 6. Ensure .env production configuration exists
echo -e "${CYAN}[4/6] Verifying environment configuration...${NC}"
ENV_FILE="${BACKEND_DIR}/.env"
if [ ! -f "${ENV_FILE}" ]; then
    if [ -f "${BACKEND_DIR}/.env.example" ]; then
        cp "${BACKEND_DIR}/.env.example" "${ENV_FILE}"
        echo -e "${YELLOW}[INFO] Created .env from .env.example${NC}"
    else
        cat <<EOF > "${ENV_FILE}"
SERVER_HOST=0.0.0.0
SERVER_PORT=8000
MONGODB_URL=mongodb://localhost:27017
DATABASE_NAME=network_monitor_db
SECRET_KEY=$(openssl rand -hex 24 2>/dev/null || echo "prod-secret-key-$(date +%s)")
ACCESS_TOKEN_EXPIRE_MINUTES=1440
DEVICE_SYNC_INTERVAL=10
ROUTER_TYPE=arp_subnet
JIO_ROUTER_IP=192.168.29.1
JIO_ROUTER_USERNAME=admin
JIO_ROUTER_PASSWORD=
SUBNET_CIDR=
EOF
        echo -e "${YELLOW}[INFO] Generated initial .env configuration${NC}"
    fi
fi

# Read configured HOST and PORT from .env
SERVER_HOST=$(grep -E '^SERVER_HOST=' "${ENV_FILE}" | cut -d '=' -f 2- | tr -d ' "' || echo "0.0.0.0")
SERVER_PORT=$(grep -E '^SERVER_PORT=' "${ENV_FILE}" | cut -d '=' -f 2- | tr -d ' "' || echo "8000")
SERVER_HOST=${SERVER_HOST:-0.0.0.0}
SERVER_PORT=${SERVER_PORT:-8000}

# 7. Generate and register systemd unit file
echo -e "${CYAN}[5/6] Installing systemd production service...${NC}"
SERVICE_FILE="/etc/systemd/system/network-monitor.service"

cat <<EOF > "${SERVICE_FILE}"
[Unit]
Description=Network & Chrome Activity Monitor Production Service
Documentation=https://github.com/
After=network.target network-online.target mongod.service mongodb.service
Wants=network-online.target
After=mongod.service mongodb.service

[Service]
Type=simple
User=root
Group=root
WorkingDirectory=${BACKEND_DIR}
EnvironmentFile=-${ENV_FILE}
ExecStart=${VENV_DIR}/bin/uvicorn main:app --host ${SERVER_HOST} --port ${SERVER_PORT} --workers 1 --log-level info
Restart=always
RestartSec=5s
KillMode=process
StandardOutput=journal
StandardError=journal
SyslogIdentifier=network-monitor
LimitNOFILE=65536
TimeoutStopSec=15

[Install]
WantedBy=multi-user.target
EOF

chmod 644 "${SERVICE_FILE}"

echo -e "Reloading systemd daemon and enabling service..."
systemctl daemon-reload
systemctl enable network-monitor.service >/dev/null 2>&1
systemctl restart network-monitor.service

# 8. Verify service health
echo -e "${CYAN}[6/6] Verifying service startup and health endpoint...${NC}"
sleep 2

HEALTH_CHECK_URL="http://127.0.0.1:${SERVER_PORT}/system/health"
RETRIES=10
SUCCESS=false

for i in $(seq 1 $RETRIES); do
    if curl -s -f -m 3 "${HEALTH_CHECK_URL}" >/dev/null 2>&1; then
        SUCCESS=true
        break
    fi
    sleep 1
done

echo ""
echo -e "${BLUE}===================================================================${NC}"
if [ "$SUCCESS" = true ]; then
    echo -e "${GREEN}  ✓ Network & Chrome Activity Monitor successfully installed & running!${NC}"
else
    echo -e "${YELLOW}  ! Service installed and started, but health endpoint is still initializing.${NC}"
fi
echo -e "${BLUE}===================================================================${NC}"

echo -e "Service Status:  ${CYAN}systemctl status network-monitor.service${NC}"
echo -e "Real-time Logs:  ${CYAN}journalctl -u network-monitor.service -f${NC}"
echo -e "Restart Service: ${CYAN}systemctl restart network-monitor.service${NC}"
echo -e "Health Check:    ${CYAN}curl http://localhost:${SERVER_PORT}/system/health${NC}"
echo -e "Swagger Docs:    ${CYAN}http://localhost:${SERVER_PORT}/docs${NC}"
echo ""
echo -e "${YELLOW}Android Tablet/Phone App Connection:${NC}"
HOST_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "YOUR_SERVER_IP")
echo -e "  In the Android app -> Settings tab -> Set Server URL to: ${GREEN}http://${HOST_IP}:${SERVER_PORT}${NC}"
echo -e "${BLUE}===================================================================${NC}"
