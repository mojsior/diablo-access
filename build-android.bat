@echo off
REM ========================================
REQUISITOS:
========================================

REM Este script requer que o Android Studio já tenha sido instalado
REM via: winget install Google.AndroidStudio

echo ========================================
Diablo Access - Build Android APK
=======================================
echo.

REM Verificar se Android Studio está instalado
if not exist "C:\Program Files\Android\Android Studio\bin\studio.exe" (
    if not exist "C:\Program Files (x86)\Android\Android Studio\bin\studio.exe" (
        echo.
        echo [ERRO] Android Studio nao encontrado!
        echo.
        echo Instale primeiro com:
        echo   winget install Google.AndroidStudio
        echo.
        pause
        exit /b 1
    )
    set "STUDIO_PATH=C:\Program Files (x86)\Android\Android Studio"
) else (
    set "STUDIO_PATH=C:\Program Files\Android\Android Studio"
)

echo [OK] Android Studio encontrado em: %STUDIO_PATH%
echo.

REM Definir variáveis de ambiente
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "JAVA_HOME=%STUDIO_PATH%\jre"
echo [INFO] JAVA_HOME: %JAVA_HOME%
echo [INFO] ANDROID_HOME: %ANDROID_HOME%
echo.

REM Entrar no diretório do projeto
cd /d "%~dp0"
cd android-project

echo ========================================
Iniciando build...
=======================================
echo.

REM Compilar usando Gradle
call gradlew.bat assembleDebug

echo.
echo ========================================
Build concluido!
=======================================
echo.
echo APK localizado em:
echo app\build\outputs\apk\debug\app-debug.apk
echo.
echo Para instalar no dispositivo Android:
echo   adb install app\build\outputs\apk\debug\app-debug.apk
echo.
pause
