@echo off
setlocal enabledelayedexpansion
title VODIL EATS - Barcha APK larni chiqarish

echo ================================================================
echo       VODIL EATS - BARCHA ILOVALARNING APK LARINI YIG'ISH
echo ================================================================
echo.

set "JAVA_HOME=C:\Users\Rayimjonov\.jdks\jbr-21.0.11"
set "ANDROID_HOME=C:\Users\Rayimjonov\AppData\Local\Android\Sdk"
set "PATH=%JAVA_HOME%\bin;%PATH%"

set "GRADLE_BAT=C:\Users\Rayimjonov\.gradle\wrapper\dists\gradle-8.7-bin\bhs2wmbdwecv87pi65oeuq5iu\gradle-8.7\bin\gradle.bat"
set "OUTPUT_DIR=%~dp0TAYYOR_APK_LAR"

if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

echo [1/3] MIJOZ ILOVASI APK yig'ilmoqda...
call "%GRADLE_BAT%" -p "%~dp01_MIJOZ_ILOVASI" assembleDebug
if exist "%~dp01_MIJOZ_ILOVASI\app\build\outputs\apk\debug\app-debug.apk" (
    copy /y "%~dp01_MIJOZ_ILOVASI\app\build\outputs\apk\debug\app-debug.apk" "%OUTPUT_DIR%\1_VODIL_EATS_MIJOZ.apk" >nul
    echo [MUVAFFAQ!] Mijoz ilovasi APK tayyor: TAYYOR_APK_LAR\1_VODIL_EATS_MIJOZ.apk
) else (
    echo [OGOHLANTIRISH] Mijoz ilovasi APK chiqishi kutilmoqda.
)

echo.
echo [2/3] KURYER ILOVASI APK yig'ilmoqda...
call "%GRADLE_BAT%" -p "%~dp02_KURYER_ILOVASI" assembleDebug
if exist "%~dp02_KURYER_ILOVASI\app\build\outputs\apk\debug\app-debug.apk" (
    copy /y "%~dp02_KURYER_ILOVASI\app\build\outputs\apk\debug\app-debug.apk" "%OUTPUT_DIR%\2_VODIL_EATS_KURYER.apk" >nul
    echo [MUVAFFAQ!] Kuryer ilovasi APK tayyor: TAYYOR_APK_LAR\2_VODIL_EATS_KURYER.apk
) else (
    echo [OGOHLANTIRISH] Kuryer ilovasi APK chiqishi kutilmoqda.
)

echo.
echo [3/3] RESTORAN ILOVASI APK yig'ilmoqda...
call "%GRADLE_BAT%" -p "%~dp03_RESTORAN_ILOVASI" assembleDebug
if exist "%~dp03_RESTORAN_ILOVASI\app\build\outputs\apk\debug\app-debug.apk" (
    copy /y "%~dp03_RESTORAN_ILOVASI\app\build\outputs\apk\debug\app-debug.apk" "%OUTPUT_DIR%\3_VODIL_EATS_RESTORAN.apk" >nul
    echo [MUVAFFAQ!] Restoran ilovasi APK tayyor: TAYYOR_APK_LAR\3_VODIL_EATS_RESTORAN.apk
) else (
    echo [OGOHLANTIRISH] Restoran ilovasi APK chiqishi kutilmoqda.
)

echo.
echo ================================================================
echo Barcha APK lar "%OUTPUT_DIR%" papkasiga joylandi!
echo ================================================================
explorer "%OUTPUT_DIR%"
pause
