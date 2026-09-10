@echo off
title VODIL EATS - Backend Server
echo ========================================================
echo       VODIL EATS - SERVERNI ISHGA TUSHIRISH
echo ========================================================
echo.
cd /d "%~dp0backend"
echo Server ishga tushmoqda...
echo API Manzil: http://localhost:3000
echo Admin Panel: http://localhost:3000/admin
echo.
node dist/main
pause
