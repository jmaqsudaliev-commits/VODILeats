@echo off
title VODIL EATS - Super Admin Paneli
echo ========================================================
echo    VODIL EATS - BIZNES EGASI BOSHQARUV PANELINI OCHISH
echo ========================================================
echo.
echo Brauzerda Super Admin paneli ochilmoqda: http://localhost:3000/admin
echo.
start http://localhost:3000/admin
timeout /t 2 >nul
exit
