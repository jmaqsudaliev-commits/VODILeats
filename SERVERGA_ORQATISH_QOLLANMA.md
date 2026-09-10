# Vodil Eats — Serverga O'rnatish va Ishga Tushirish Bo'yicha To'liq Qo'llanma

Ushbu qo'llanma orqali **Vodil Eats** platformasining Backend serveri va Admin panelini istalgan Linux (Ubuntu) VPS serveriga 5 daqiqa ichida o'rnatib, ommaviy foydalanishga topshirishingiz mumkin.

---

## 🖥️ 1. Server Talablari (Qanday server sotib olish kerak?)

- **Operatsion tizim**: Ubuntu 22.04 LTS yoki Ubuntu 24.04 LTS (Tavsiya etiladi)
- **Protsessor (CPU)**: 1 yoki 2 yadro
- **Operativ xotira (RAM)**: Kamida 1 GB yoki 2 GB RAM
- **Xotira (Disk)**: 20 GB SSD
- **Provayderlar**: Hetzner, DigitalOcean, Timeweb Cloud, VDSina, Hostinger yoki AWS.

---

## 📂 2. Loyiha Fayllarini Serverga Yuklash

Serveringizga SSH orqali kiring:
```bash
ssh root@SERVER_IP_MANZILINGIZ
```

Loyiha uchun papka oching va fayllarni yuklang:
```bash
mkdir -p /var/www/vodil-eats
cd /var/www/vodil-eats
```

Fayllarni serverga yuklashning 2 ta oson yo'li bor:
- **Variant A (WinSCP / FileZilla orqali)**: Kompyuteringizdan `backend` va `ADMIN_PANEL` papkalarini hamda `SERVERGA_O_RNATISH_UBUNTU.sh` faylini serverdagi `/var/www/vodil-eats` papkasiga tashlang.
- **Variant B (GitHub orqali - Tavsiya etiladi)**:
  ```bash
  git clone https://github.com/jmaqsudaliev-commits/VODILeats.git .
  ```

---

## ⚡ 3. 1-Bosqichli Tezkor Avtomatik O'rnatish (Eng oson usul)

Loyiha ichidagi tayyor skript barcha kerakli dasturlarni (Node.js 20, PM2, Nginx, UFW firewall) o'zi o'rnatadi, backendni yig'adi va fonda ishga tushiradi:

```bash
cd /var/www/vodil-eats
chmod +x SERVERGA_O_RNATISH_UBUNTU.sh
./SERVERGA_O_RNATISH_UBUNTU.sh
```

> Skript 1-2 daqiqa ichida barcha ishlarni bajaradi va terminalda tayyor havolalarni chiqaradi!

---

## 🛠️ 4. Qo'lda (Manual) O'rnatish Qadamlari (Batafsil tushunish uchun)

Agar avtomatik skriptsiz o'zingiz bosqichma-bosqich o'rnatmoqchi bo'lsangiz:

### 1-qadam: Tizimni yangilash va Node.js 20 o'rnatish
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y curl git nginx build-essential
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
sudo npm install -g pm2
```

### 2-qadam: Backend kutubxonalarini o'rnatish va build qilish
```bash
cd /var/www/vodil-eats/backend
npm install
npm run build
```

### 3-qadam: PM2 orqali ishga tushirish (Server o'chib yonsa ham avtomatik yonadi)
```bash
pm2 start dist/main.js --name "vodil-eats"
pm2 save
pm2 startup
```

### 4-qadam: Nginx reverse proxy sozlash (Port 80 -> 3000)
`/etc/nginx/sites-available/vodil-eats` faylini yarating:
```nginx
server {
    listen 80;
    server_name _; # yoki sizning domeningiz: api.vodileats.uz

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
```
Faollashtirish:
```bash
sudo ln -sf /etc/nginx/sites-available/vodil-eats /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl restart nginx
```

---

## 🔒 5. Domen va Bepul SSL (HTTPS) Sertifikatini Ulash

Agar sizda domen bo'lsa (masalan: `api.vodileats.uz` yoki `vodileats.uz`):

1. Domen boshqaruv panelida (GoDaddy, Namecheap, Reg.ru va h.k.) **A record** ochib, server IP manzilingizga yo'naltiring.
2. Serverda Certbot o'rnating va SSL sertifikatini oling:
   ```bash
   sudo apt install -y certbot python3-certbot-nginx
   sudo certbot --nginx -d vodileats.uz -d api.vodileats.uz
   ```
3. Shundan so'ng serveringiz avtomatik ravishda xavfsiz **`https://`** protokoli orqali ishlaydi!

---

## 📱 6. Mobil Ilovalarni (Mijoz, Kuryer, Restoran) Serverga Ulash

Har uchala ilovaning tayyor APK fayllari `TAYYOR_APK_LAR\` papkasida tayyor:
- `1_VODIL_EATS_MIJOZ.apk`
- `2_VODIL_EATS_KURYER.apk`
- `3_VODIL_EATS_RESTORAN.apk`

Ilovalar serverga 2 xil usulda ulanishi mumkin:

### Usul 1: Ilova ichidan IP/Domenni o'zgartirish (Hech narsa qayta kompilyatsiya qilinmaydi)
Har bir ilovaning kirish oynasi yuqori o'ng burchagida **⚙️ Sozlama** belgisi bor.
1. ⚙️ tugmasini bosing.
2. Server IP yoki domenini kiriting (Masalan: `185.120.45.67:3000` yoki `api.vodileats.uz`).
3. **"Saqlash"** tugmasini bosing. Ilova darhol yangi serveringizga ulanadi!

### Usul 2: Kodda doimiy qilib yozish (Ommaviy Google Play yoki foydalanuvchilarga tarqatishdan oldin)
Agar server IP yoki domeningiz aniq bo'lsa:
- **Mijoz Ilovasi**: `1_MIJOZ_ILOVASI/app/src/main/java/com/vodileats/customer/data/ModelsAndData.kt` faylidagi `DEFAULT_HOST` ga serveringiz IP/domenini yozing.
- **Kuryer Ilovasi**: `2_KURYER_ILOVASI/app/src/main/java/com/vodileats/courier/data/CourierData.kt` faylidagi `DEFAULT_HOST` ga yozing.
- **Restoran Ilovasi**: `3_RESTORAN_ILOVASI/app/src/main/java/com/vodileats/restaurant/data/RestaurantData.kt` faylidagi `DEFAULT_HOST` ga yozing.
- So'ng `build_apk.bat` ni ishga tushirsangiz, yangi server manzili bilan yig'iladi.

---

## 👑 7. Super Admin Paneli va Boshqaruv

Server ishga tushgach:
- **Admin Panel URL**: `http://SIZNING_SERVER_IP/admin` (yoki `https://sizning-domen.uz/admin`)
- **Maxfiy Parol**: `VODIL_EATS_2026_UZB!`

### Admin Paneldan Nimalarni Boshqarasiz:
1. **Restoranlar**:
   - Istalgan ortiqcha yoki test restoranlarni **`[🗑️ O'chirish]`** tugmasi orqali o'chirib tashlashingiz mumkin.
   - Yangi restoran qo'shib, unga **Maxsus Kod** (`VDL-SHOH-101`) berasiz.
   - Har bir restoran kartasidagi **`[📋 Nusxa]`** tugmasi orqali kodni nusxalab, restoran egasiga berasiz.
2. **Kuryerlar**:
   - Yangi ro'yxatdan o'tgan kuryerlar ro'yxati chiqadi (`⏳ Kutilmoqda`).
   - Bitta **`[✅ Tasdiqlash]`** tugmasini bossangiz, kuryer ilovasida faol ish rejimi ochiladi va u buyurtma olish huquqiga ega bo'ladi.
3. **Jonli Buyurtmalar va GPS Xarita**:
   - Buyurtmalar holatini, daromadlarni va kuryerlar harakatini real vaqt rejimida kuzatasiz.

---

## 🩺 8. Foydali Server Buyruqlari (Monitoring)

- Server holatini ko'rish:
  ```bash
  pm2 status
  ```
- Jonli loglarni kuzatish:
  ```bash
  pm2 logs vodil-eats
  ```
- Serverni qayta ishga tushirish (restart):
  ```bash
  pm2 restart vodil-eats
  ```
- Nginx holatini tekshirish:
  ```bash
  sudo systemctl status nginx
  ```
