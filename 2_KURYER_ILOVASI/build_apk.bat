@echo off
set "JAVA_HOME=C:\Users\Rayimjonov\.jdks\jbr-21.0.11"
set "ANDROID_HOME=C:\Users\Rayimjonov\AppData\Local\Android\Sdk"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "GRADLE_BAT=C:\Users\Rayimjonov\.gradle\wrapper\dists\gradle-8.7-bin\bhs2wmbdwecv87pi65oeuq5iu\gradle-8.7\bin\gradle.bat"

cd /d "%~dp0"
echo Kuryer ilovasi APK yig'ilmoqda...
call "%GRADLE_BAT%" assembleDebug
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo [MUVAFFAQIYAT!] Kuryer APK tayyor: app\build\outputs\apk\debug\app-debug.apk
)
