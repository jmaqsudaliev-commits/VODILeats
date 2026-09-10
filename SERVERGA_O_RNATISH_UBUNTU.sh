#!/bin/bash
# ==============================================================================
#  VODIL EATS — UBUNTU VPS SERVERGA O'RNATISH VA ISHGA TUSHIRISH SKRIPTI
#  (Ubuntu 20.04 / 22.04 / 24.04 uchun to'liq avtomatik)
# ==============================================================================

set -e

echo ""
echo "=============================================================================="
echo "🚀 VODIL EATS SERVERINI O'RNATISH BOSHLANDI"
echo "=============================================================================="
echo ""

# 1. Tizim paketlarini yangilash
echo "📦 [1/7] Ubuntu paketlarini yangilash..."
sudo apt update -y && sudo apt upgrade -y
sudo apt install -y curl git ufw nginx build-essential

# 2. Node.js 20 LTS o'rnatish
echo "🟢 [2/7] Node.js 20 LTS o'rnatilmoqda..."
if ! command -v node &> /dev/null; then
    curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
    sudo apt install -y nodejs
fi
echo "Node.js versiyasi: $(node -v)"
echo "NPM versiyasi: $(npm -v)"

# 3. PM2 Process Managerni o'rnatish
echo "⚡ [3/7] PM2 (serverni fonda uzluksiz yurgizuvchi tizim) o'rnatilmoqda..."
sudo npm install -g pm2

# 4. Backend papkasiga o'tib kutubxonalarni o'rnatish va build qilish
echo "🔨 [4/7] Backend bog'liqliklari (dependencies) o'rnatilmoqda va yig'ilmoqda..."
CURRENT_DIR=$(pwd)
if [ -d "$CURRENT_DIR/backend" ]; then
    cd "$CURRENT_DIR/backend"
elif [ -f "$CURRENT_DIR/package.json" ]; then
    cd "$CURRENT_DIR"
fi

npm install --production=false
npm run build

# 5. .env sozlamasi
echo "⚙️ [5/7] .env sozlamalari tekshirilmoqda..."
if [ ! -f .env ]; then
    cat << 'EOF' > .env
PORT=3000
NODE_ENV=production
DATABASE_TYPE=sqlite
SQLITE_DATABASE=vodil_eats.sqlite
JWT_SECRET=super_secret_jwt_key_vodil_eats_2026_production_safe
JWT_REFRESH_SECRET=super_secret_refresh_jwt_key_vodil_eats_2026
JWT_EXPIRATION=86400s
ADMIN_MASTER_KEY=VODIL_EATS_2026_UZB!
EOF
fi

# 6. PM2 orqali backendni ishga tushirish
echo "🚀 [6/7] Server PM2 da ishga tushirilmoqda..."
pm2 delete vodil-eats 2>/dev/null || true
pm2 start dist/main.js --name "vodil-eats"
pm2 save
pm2 startup systemd -u $USER --hp /home/$USER 2>/dev/null || true

# 7. Nginx va Firewall sozlash
echo "🌐 [7/7] Nginx va Xavfsizlik devori (UFW) sozlanmoqda..."
sudo ufw allow 22/tcp 2>/dev/null || true
sudo ufw allow 80/tcp 2>/dev/null || true
sudo ufw allow 443/tcp 2>/dev/null || true
sudo ufw allow 3000/tcp 2>/dev/null || true
sudo ufw --force enable 2>/dev/null || true

# Nginx reverse proxy sozlamasi
cat << 'EOF' | sudo tee /etc/nginx/sites-available/vodil-eats > /dev/null
server {
    listen 80;
    server_name _;

    client_max_body_size 50M;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF

sudo ln -sf /etc/nginx/sites-available/vodil-eats /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default 2>/dev/null || true
sudo nginx -t && sudo systemctl restart nginx

SERVER_IP=$(hostname -I | awk '{print $1}')

echo ""
echo "=============================================================================="
echo "🎉 VODIL EATS SERVERI MUVAFFAQIYATLI O'RNATILDI VA ISHLAMOQDA!"
echo "=============================================================================="
echo "📡 Server Asosiy Manzili: http://$SERVER_IP"
echo "📡 To'g'ridan-to'g'ri API: http://$SERVER_IP/api/v1"
echo "📚 Swagger Hujjatlari:   http://$SERVER_IP/api/docs"
echo "👑 Super Admin Paneli:   http://$SERVER_IP/admin"
echo "🔑 Maxfiy Admin Paroli:  VODIL_EATS_2026_UZB!"
echo "=============================================================================="
echo "💡 Server holatini tekshirish: pm2 status"
echo "💡 Server loglarini ko'rish:    pm2 logs vodil-eats"
echo "=============================================================================="
