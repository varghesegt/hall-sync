@echo off
setlocal EnableDelayedExpansion
title HallSync - Starting System

:: --- CONFIGURATION ---
set APP_URL=http://localhost:8081
set HEALTH_URL=%APP_URL%/actuator/health
set CHROME_PATH="C:\Program Files\Google\Chrome\Application\chrome.exe"
:: ---------------------

color 0B
echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║     HallSync - EXAM SEAT ALLOCATION SYSTEM                ║
echo  ║     Starting Production Environment...                    ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.

:: 1. Check if Docker is running
echo  [1/3] Verifying Docker Desktop status...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo  [ERROR] Docker Desktop is NOT running!
    echo.
    echo  Please:
    echo    1. Open Docker Desktop from the Start Menu
    echo    2. Wait until the whale icon in taskbar is stable
    echo    3. Run this START.bat again
    echo.
    pause
    exit /b 1
)
echo      ✓ Docker is active.

:: 2. Start Containers
echo.
echo  [2/3] Initializing Database and Application...
echo        (This may take 30-60 seconds on the first run)
echo.

docker compose up -d --build
if %errorlevel% neq 0 (
    echo.
    echo  [ERROR] Failed to launch containers.
    echo  Please ensure no other application is using port 8081.
    echo.
    pause
    exit /b 1
)

:: 3. Smart Wait for Health
echo.
echo  [3/3] Waiting for HallSync to become ready...
echo.

set /a attempt=0
:wait_loop
set /a attempt+=1
if !attempt! gtr 100 (
    echo.
    echo  [TIMEOUT] The system is taking longer than expected to start.
    echo  Please check if your computer is low on memory.
    echo.
    pause
    exit /b 1
)

:: Use PowerShell for silent health check
powershell -command "try { $response = Invoke-WebRequest -Uri '%HEALTH_URL%' -UseBasicParsing -TimeoutSec 2; if ($response.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1

if %errorlevel% neq 0 (
    <nul set /p =.
    timeout /t 2 /nobreak >nul
    goto wait_loop
)

echo.
echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║                                                           ║
echo  ║   HallSync IS ONLINE AND READY!                           ║
echo  ║                                                           ║
echo  ║   Login Credentials:                                      ║
echo  ║     Username: coe1@krce.ac.in                             ║
echo  ║     Password: skm@8115                                     ║
echo  ║                                                           ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.
echo  Opening the Command Center in Chrome...

:: Auto-open in Chrome
if exist %CHROME_PATH% (
    start "" %CHROME_PATH% %APP_URL%
) else (
    start "" "%APP_URL%"
)

echo.
echo  System is running. You can minimize this window.
echo  To shut down safely, use STOP.bat.
echo.
pause
