#!/bin/bash
# ==============================================================================
# VODIL EATS — 1-CLICK PRODUCTION SERVER DEPLOYMENT SCRIPT (Ubuntu / Debian VPS)
# ==============================================================================
set -e

echo "🚀 [1/6] Server paketlarini yangilash..."
sudo apt-get update -y && sudo apt-get upgrade -y

echo "🐳 [2/6] Docker va Docker Compose o'rnatilmoqda..."
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker $USER
    rm get-docker.sh
fi

if ! docker compose version &> /dev/null; then
    sudo apt-get install -y docker-compose-plugin
fi

echo "⚙️ [3/6] Konfiguratsiya fayllari (.env) tekshirilmoqda..."
if [ ! -f .env ]; then
    cp .env.example .env 2>/dev/null || cat << 'EOF' > .env
PORT=3000
NODE_ENV=production
DATABASE_TYPE=postgres
DATABASE_HOST=postgres
DATABASE_PORT=5432
DATABASE_USER=vodil_admin
DATABASE_PASSWORD=vodil_secret_2024
DATABASE_NAME=vodil_eats
REDIS_HOST=redis
REDIS_PORT=6379
JWT_SECRET=super_secret_jwt_key_vodil_eats_2026_production
JWT_EXPIRATION=86400s
ADMIN_MASTER_KEY=VODIL_EATS_2026_UZB!
EOF
fi

echo "🧱 [4/6] Docker konteynerlari yig'ilmoqda va ishga tushirilmoqda..."
sudo docker compose -f docker-compose.prod.yml up --build -d

echo "⏳ [5/6] Server bazasi ishga tushishi kutilmoqda (10 soniya)..."
sleep 10

echo "🌱 [6/6] Ma'lumotlar bazasini dastlabki ma'lumotlar bilan to'ldirish..."
sudo docker compose -f docker-compose.prod.yml exec -T backend npm run seed || true

SERVER_IP=$(hostname -I | awk '{print $1}')
echo ""
echo "=============================================================================="
echo "🎉 VODIL EATS SERVERI MUVAFFAQIYATLI ISHGA TUSHIRILDI!"
echo "=============================================================================="
echo "📡 Asosiy API:         http://$SERVER_IP:3000/api/v1"
echo "📚 Swagger Docs:       http://$SERVER_IP:3000/api/docs"
echo "👑 Super Admin Paneli: http://$SERVER_IP:3000/admin"
echo "🔑 Maxfiy Parol:       VODIL_EATS_2026_UZB!"
echo "=============================================================================="
