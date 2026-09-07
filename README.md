# 🍔 VODIL EATS — Enterprise Yetkazib Berish Platformasi

> **1 000 000+ faol foydalanuvchiga mo'ljallangan, to'liq ishlab chiqarishga tayyor (Production-Ready) yetkazib berish ekotizimi.**

Ushbu platforma **Yandex Eats / Uzum Tezkor** darajasidagi to'liq avtomatlashtirilgan yetkazib berish tizimi bo'lib, 3 ta mustaqil Android ilova, biznes egasi uchun Super Admin Web Paneli hamda High-Load Backend arxitekturasidan iborat.

---

## 🏛️ Tizim Arxitekturasi

```
                      +----------------------------------+
                      |     👑 SUPER ADMIN DASHBOARD     |
                      |   (Windows / Web: :3000/admin)   |
                      +-----------------+----------------+
                                        |
                                        v
+------------------+         +--------------------+         +--------------------+
| 📱 MIJOZ ILOVASI | <-----> |   🚀 NESTJS CORE   | <-----> | 🛵 KURYER ILOVASI  |
| (Jetpack Compose)|         |   HIGH-LOAD API    |         | (GPS Service Live) |
+------------------+         +---------+----------+         +--------------------+
                                       |
                                       v
                             +--------------------+
                             | 🍽️ RESTORAN ILOVA  |
                             | (Ovozli Buyurtma)  |
                             +--------------------+
                                       |
                   +-------------------+-------------------+
                   |                   |                   |
                   v                   v                   v
          +-----------------+ +-----------------+ +-----------------+
          |  🐘 PostgreSQL  | | ⚡ Redis Streams| |  🛡️ PgBouncer   |
          |   + PostGIS     | |    + GeoCache   | |  Connection Pool|
          +-----------------+ +-----------------+ +-----------------+
```

---

## 📂 Loyihalar Tuzilmasi

| Papka | Vazifasi | Texnologiyalar |
|---|---|---|
| [`1_MIJOZ_ILOVASI`](./1_MIJOZ_ILOVASI) | Mijoz Android Ilovasi | Jetpack Compose, Room, Retrofit, Maps, Auth (SMS+Parol), O'zbekcha / Русский |
| [`2_KURYER_ILOVASI`](./2_KURYER_ILOVASI) | Kuryer Android Ilovasi | Foreground GPS Service, Google Maps Navigation, 30s taklif taymeri, Uz/Ru |
| [`3_RESTORAN_ILOVASI`](./3_RESTORAN_ILOVASI) | Restoran Android Ilovasi | Ovozli yangi buyurtma signallari, Stop-list boshqaruvi, Holatlar zanjiri, Uz/Ru |
| [`ADMIN_PANEL`](./ADMIN_PANEL) | Biznes Egasi Super Admin Paneli | Glassmorphism UI, OpenStreetMap, Real Boshqaruv, CSV Hisobot, 20 talik Maxfiy Parol |
| [`backend`](./backend) | 1M+ Foydalanuvchi Backend Serveri | NestJS, TypeScript, PostGIS, Redis Streams, PgBouncer, Nginx, Cluster Mode |

---

## 🔐 Super Admin Paneli (Biznes Egasi Uchun)

- **Manzil:** `http://localhost:3000/admin` (yoki `ADMIN_PANEL/OCHISH_ADMIN_PANEL.bat`)
- **20-Xonali O'zgarmas Maxfiy Parol:**
  ```
  VODIL_EATS_2026_UZB!
  ```
- **Asosiy Imkoniyatlar:**
  1. **Jonli Statistika:** Jami tushum, faol buyurtmalar, onlayn kuryerlar, hamkor restoranlar.
  2. **Haqiqiy Restoran Qo'shish:** Nomi, manzili, telefoni, rasmi va ish vaqtlarini to'g'ridan-to'g'ri bazaga kiritish.
  3. **Haqiqiy Kuryerlarni Ro'yxatga Olish:** Kuryer ismi, telefoni, transport turi (motosikl, mashina, velosiped) va davlat raqami.
  4. **Jonli Xarita:** Vodil va Farg'ona xaritasida kuryerlarning jonli harakati.
  5. **CSV / Excel Eksport:** Barcha buyurtmalarni 1 bosishda hisobot qilib yuklab olish.
  6. **Ko'p Tillilik:** O'zbekcha (UZ) va Русский (RU) o'rtasida 1-click bilan almashish.

---

## 📱 Android APK Chiqarish (10 Soniyada)

Har bir ilovadan mustaqil ravishda APK chiqarish mumkin:
1. **Android Studio** dasturida kerakli papkani oching (`1_MIJOZ_ILOVASI`, `2_KURYER_ILOVASI` yoki `3_RESTORAN_ILOVASI`).
2. Yuqori menyudan: **`Build` ➔ `Build Bundle(s) / APK(s)` ➔ `Build APK(s)`** ni bosing.
3. Tayyor bo'lgan APK fayl manzili:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```
4. Uni xohlagan Android telefoningizga o'rnatib ishlataverasiz.

> 💡 Har bir ilovada `gradle.properties` faylida markaziy server manzili (`SERVER_URL`) belgilangan. Hozirda lokal Wi-Fi IP (`http://192.168.1.4:3000/api/v1/`) o'rnatilgan.

---

## ☁️ Bulutli Serverga Joylash (VPS Deployment)

Kelajakda butun vodiy/respublika bo'ylab serverga qo'yish uchun **1-Click Deploy Skripti** tayyorlangan:

```bash
# Ubuntu / Debian VPS serverida:
cd backend
bash deploy_server.sh
```

Ushbu skript:
1. Docker va Docker Composeni avtomatik o'rnatadi.
2. PostgreSQL 15 + PostGIS, Redis 7, PgBouncer va Nginxni ishga tushiradi.
3. Bazani ma'lumotlar bilan to'ldiradi va portlarni ochiq holda sozlaydi.

---

## 🐙 GitHub ga Yuklash Qo'llanmasi

Ushbu kodlar omborini o'z GitHub hisobingizga yuklash uchun quyidagi buyruqlarni bajaring:

```bash
# 1. Repozitoriyni initsializatsiya qilish:
git init

# 2. Fayllarni qo'shish va kommit qilish:
git add .
git commit -m "feat: initial production-ready release of Vodil Eats platform"

# 3. GitHub dagi yangi bo'sh repozitoriyangizga ulash:
git remote add origin https://github.com/SIZNING_USERNAME/vodil-eats.git

# 4. Asosiy tarmoqqa push qilish:
git branch -M main
git push -u origin main
```

---

## 📄 Litsenziya

Mualliflik huquqi © 2026 **VODIL EATS Team**. Barcha huquqlar himoyalangan.
