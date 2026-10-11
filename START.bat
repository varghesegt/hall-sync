@echo off
setlocal EnableDelayedExpansion
title HallSync - Starting System

:: --- CONFIGURATION ---
set APP_URL=http://localhost:8080
set BACKEND_URL=http://localhost:8081
set HEALTH_URL=%APP_URL%/
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
echo  [1/4] Verifying Docker Desktop status...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo  [ERROR] Docker Desktop is NOT running!
    echo.
    echo  Please:
    echo    1. Open Docker Desktop from the Start Menu
    echo    2. Wait until the Docker whale icon in taskbar says 'Running'
    echo    3. Run this START.bat again
    echo.
    pause
    exit /b 1
)
echo      ✓ Docker is active.

:: 2. Detect Docker Compose Command (supports modern 'docker compose' and legacy 'docker-compose')
echo.
echo  [2/4] Detecting Docker Compose engine...
set COMPOSE_CMD=docker compose
docker compose version >nul 2>&1
if %errorlevel% neq 0 (
    docker-compose version >nul 2>&1
    if %errorlevel% equ 0 (
        set COMPOSE_CMD=docker-compose
        echo      ✓ Using legacy docker-compose CLI.
    ) else (
        echo  [ERROR] Neither 'docker compose' nor 'docker-compose' command was found.
        echo  Please verify Docker Desktop installation.
        pause
        exit /b 1
    )
) else (
    echo      ✓ Using Docker Compose V2 engine.
)

:: 3. Start Containers
echo.
echo  [3/4] Initializing Database, Cache, Storage and Application...
echo        (This may take a moment while health checks initialize)
echo.

%COMPOSE_CMD% up -d --build
if %errorlevel% neq 0 (
    echo.
    echo  [ERROR] Failed to launch containers.
    echo.
    echo  --- System Diagnostics ---
    %COMPOSE_CMD% ps
    echo.
    echo  --- Application Logs ---
    docker logs hallsync-backend --tail 30
    echo.
    echo  Troubleshooting Tips:
    echo    1. If ports 8080 or 8081 are in use, close any background servers or local Java/Tomcat instances.
    echo    2. Run STOP.bat to clean up existing stopped containers.
    echo    3. Then re-run START.bat.
    echo.
    pause
    exit /b 1
)

:: 4. Smart Wait for Health
echo.
echo  [4/4] Waiting for HallSync to become ready...
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

powershell -command "try { if ((Invoke-WebRequest -Uri '%HEALTH_URL%' -UseBasicParsing -TimeoutSec 2).StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }" >nul 2>&1

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
echo  ║   Portal (Web):  http://localhost:8080                    ║
echo  ║   Direct API:    http://localhost:8081                    ║
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
