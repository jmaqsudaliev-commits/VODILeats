# 🚀 VODIL EATS — To'liq Tizim va APK Qo'llanmasi

Ushbu hujjat **VODIL EATS** platformasining qanday ishlashi, APK fayllarini qanday yig'ish va telefonlarga o'rnatish hamda biznes egasi uchun **Super Admin Paneli**dan foydalanish bo'yicha to'liq qo'llanmadir.

---

## 📱 1. Nega Android Aynan `.apk` Faylni O'qiydi?

**APK** — bu **Android Package Kit** so'zining qisqartmasi. 
Windows tizimi `.exe` dasturlarni, iOS tizimi `.ipa` dasturlarni ishga tushirgani kabi, **Android operatsion tizimi faqat `.apk` fayllarni taniy oladi va o'rnatadi**.

### APK ichida nimalar bor?
1. **`classes.dex` (Dalvik Executable)**: Biz yozgan Kotlin/Java kodlari Dalvik/ART virtual mashinasi tushunadigan mashina baytkodiga aylantirilgan fayl.
2. **`res/` (Resurslar)**: Barcha tugmalar, rasmlar, dizaynlar, ranglar va matnlar to'plami.
3. **`AndroidManifest.xml`**: Ilovaning pasporti — ilovaning nomi, internetga ulanish va GPS lokatsiyadan foydalanish ruxsatnomalari.
4. **`META-INF/` (Raqamli Imzo)**: Ilovaning xavfsizlik sertifikati. Android faqat raqamli imzolangan APK fayllarini telefonga o'rnatishga ruxsat beradi.

---

## 🛠️ 2. Har Bir Ilovadan APK Chiqarish (1-Qadam)

Siz adashib ketmasligingiz uchun barcha 3 ta ilova **alohida, to'liq mustaqil papkalarga** ajratilgan:

| Ilova | Papkasi | Tayyor APK Chiqadigan Manzil |
|---|---|---|
| **1. Mijoz Ilovasi** | `1_MIJOZ_ILOVASI` | `1_MIJOZ_ILOVASI\app\build\outputs\apk\debug\app-debug.apk` |
| **2. Kuryer Ilovasi** | `2_KURYER_ILOVASI` | `2_KURYER_ILOVASI\app\build\outputs\apk\debug\app-debug.apk` |
| **3. Restoran Ilovasi** | `3_RESTORAN_ILOVASI` | `3_RESTORAN_ILOVASI\app\build\outputs\apk\debug\app-debug.apk` |

### Android Studio Orqali 10 Soniyada APK Chiqarish:
1. **Android Studio** dasturini oching.
2. **"Open"** tugmasini bosing va kerakli papkani (masalan, `1_MIJOZ_ILOVASI`) tanlang.
3. Yuqori menyudan: **`Build`** ➔ **`Build Bundle(s) / APK(s)`** ➔ **`Build APK(s)`** ni bosing.
4. 10-20 soniyada o'ng pastki burchakda **"locate"** havolasi chiqadi. Unga bossangiz, tayyor **`app-debug.apk`** fayl turgan papka ochiladi!
5. Ushbu `.apk` faylni Telegram orqali telefoningizga jo'natib, ustiga bossangiz, telefon ilovani o'rnatib beradi.

> 💡 **Qulaylik:** Har bir ilova papkasida `build_apk.bat` skripti ham mavjud.

---

## 👑 3. Ilova Egasi Uchun Super Admin Dashboard (Barchasi Bir Joyda!)

Siz ilova egasisiz. Restoranlar, ularga tushgan buyurtmalar, kuryerlar va mijozlarni bitta markaziy ekranda ko'rish uchun **Super Admin Web Paneli** yaratildi:

- **Joylashuvi:** `ADMIN_PANEL` papkasi
- **Tezkor ochish:** `ADMIN_PANEL\OCHISH_ADMIN_PANEL.bat` fayliga 2 marta bosing (yoki brauzerda `http://localhost:3000/admin` ga kiring).

### Super Admin Panel Imkoniyatlari:
1. **📊 Umumiy Boshqaruv (Dashboard)**:
   - Jami tushum (so'mda)
   - Jami va faol buyurtmalar soni
   - Onlayn kuryerlar va hamkor restoranlar soni
   - Jonli buyurtmalar lentasi
2. **📦 Jonli Zakazlar (Live Orders)**:
   - Restoranlarga tushgan barcha buyurtmalar real vaqtda yangilanadi.
   - Har bir buyurtmaning egasi, telefoni, manzili va buyurtma qilgan taomlari.
   - Statusni bitta tugma bilan o'zgartirish: *[Tasdiqlash]*, *[Tayyorlash]*, *[Kuryerga berish]*, *[Yetkazildi deb belgilash]*, *[Bekor qilish]*.
3. **🍽️ Hamkor Restoranlar**:
   - Vodil va Farg'onadagi barcha oshxonalar, ularning ish vaqti, reytingi.
   - Restoranni 1 ta tugma bilan "Faol" yoki "Yopiq" rejimga o'tkazish (Toggle switch).
4. **🛵 Kuryerlar va Jonli Xarita**:
   - OpenStreetMap interaktiv xaritasi (Vodil & Farg'ona).
   - Kuryerlarning joriy joylashuvi: 🟢 Yashil (Onlayn / Bo'sh), 🟡 Sariq (Buyurtma yetkazyapti), ⚫ Kulrang (Oflayn).
   - Kuryerlar jadvali: ismi, telefoni, transport turi, reytingi, bugungi yetkazishlari.
5. **👥 Ro'yxatdan O'tgan Mijozlar**:
   - Telefon raqamlari, ism-familiyalari, tasdiqlangan holati.
6. **⚡ "Test Buyurtma Yaratish" Tugmasi**:
   - Yuqori o'ng burchakdagi bu tugmani bossangiz, darhol simulyatsiya qilingan yangi buyurtma tushadi va jadvalda paydo bo'ladi!

---

## 🔐 4. Mijoz Ilovasi Kirish va Registratsiya Tizimi

Mijoz ilovasiga telefon raqam orqali xavfsiz autentifikatsiya o'rnatilgan:

1. **Yangi Mijoz Registratsiyasi:**
   - Mijoz telefon raqamini kiritadi (`+998 90 123 45 67`).
   - Telefoniga 6 xonali SMS OTP kod boradi (Tekshirish uchun konsolda ham chiqadi).
   - Mijoz o'z ism-familiyasini yozadi va **doimiy parol** yaratadi (masalan: `MeningParolim123`).
   - Tizim uni ro'yxatdan o'tkazib, darhol ilovaga kiritadi.
2. **Boshqa Telefondan Kirish:**
   - Agar mijoz ertaga boshqa yangi telefondan kirmoqchi bo'lsa, **telefon raqami va parolini** teradi.
   - SMS kod kutmasdan **darhol** akkauntiga kiradi!
   - Agar parolini unutgan bo'lsa, xohlagan paytda yana SMS OTP orqali kirishi mumkin.

---

## ⚡ 5. Backend va Yuqori Yuklama (1,000,000+ Foydalanuvchi)

Backend hozirda kompyuteringizda fon rejimida ishlab turibdi:
- **API manzili:** `http://localhost:3000/api/v1`
- **Swagger hujjatlari:** `http://localhost:3000/api/docs`
- **WebSocket (Jonli ulanish):** `ws://localhost:3000`
- **Arxitektura:** Node.js CPU Multi-Core Clustering, Redis in-memory kesh, PgBouncer ulanishlar puli, Gzip payload siqish, 1000 req/min DDoS himoyasi.
