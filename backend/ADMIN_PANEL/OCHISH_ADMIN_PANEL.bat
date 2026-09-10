@echo off
title VODIL EATS - Super Admin Paneli
echo ========================================================
echo    VODIL EATS - BIZNES EGASI BOSHQARUV PANELINI OCHISH
echo ========================================================
echo.
REM Server ishlab turganini tekshirish yoki ishga tushirish
curl -s http://localhost:3000 >nul 2>&1
if %errorlevel% neq 0 (
    echo Server ishga tushirilmoqda...
    start /min cmd /c "cd /d "%~dp0..\backend" && node dist/main"
    timeout /t 3 >nul
)

echo Brauzerda Super Admin paneli ochilmoqda: http://localhost:3000/admin
echo.
start http://localhost:3000/admin
timeout /t 2 >nul
exit
