@echo off
title VODIL EATS - 1_MIJOZ_ILOVASI APK Chiqarish
echo ========================================================
echo    VODIL EATS - MIJOZ ILODASIDAN APK CHIQARISH
echo ========================================================
echo.
echo Bu skript mijoz ilovasining tayyor Android o'rnatish (.apk)
echo faylini yig'ib beradi.
echo.
echo Tayyor APK chiqadigan joy:
echo app\build\outputs\apk\debug\app-debug.apk
echo.
echo ========================================================
echo QANDAY QILIB ISHLATILADI:
echo 1. Android Studio dasturini oching.
echo 2. "Open" tugmasini bosib shu "1_MIJOZ_ILOVASI" papkasini oching.
echo 3. Yuqori menyudan: "Build" -> "Build Bundle(s) / APK(s)" -> "Build APK(s)"
echo 4. 10-20 soniyada o'ng pastda "locate" tugmasi chiqadi.
echo    Uni bosganingizda tayyor APK fayl ko'rinadi!
echo.
echo ========================================================
if exist "gradlew.bat" (
    echo Avtomatik APK yig'ish sinab ko'rilmoqda...
    call gradlew.bat assembleDebug
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        echo.
        echo [MUVAFFAQIN!] APK tayyor: app\build\outputs\apk\debug\app-debug.apk
        explorer "app\build\outputs\apk\debug"
    )
)
pause
