@echo off
setlocal EnableDelayedExpansion
title HallSync - Starting System

:: --- PATH RECOVERY (Resolves Docker Desktop not in PATH after fresh installation) ---
if exist "%ProgramFiles%\Docker\Docker\resources\bin\docker.exe" (
    set "PATH=%ProgramFiles%\Docker\Docker\resources\bin;%PATH%"
)
if exist "%LOCALAPPDATA%\Docker\resources\bin\docker.exe" (
    set "PATH=%LOCALAPPDATA%\Docker\resources\bin;%PATH%"
)
if exist "C:\Program Files\Docker\Docker\resources\bin\docker.exe" (
    set "PATH=C:\Program Files\Docker\Docker\resources\bin;%PATH%"
)

:: --- READ PORTS & PROJECT FROM .ENV (Default: 8080 and 8081) ---
set FRONTEND_PORT=8080
set BACKEND_PORT=8081
set PROJECT_NAME=hallsync

if exist .env (
    for /f "usebackq tokens=1,2 delims==" %%a in (.env) do (
        if "%%a"=="PORT_FRONTEND" set FRONTEND_PORT=%%b
        if "%%a"=="PORT_BACKEND" set BACKEND_PORT=%%b
        if "%%a"=="COMPOSE_PROJECT_NAME" set PROJECT_NAME=%%b
    )
)

set APP_URL=http://localhost:%FRONTEND_PORT%
set BACKEND_URL=http://localhost:%BACKEND_PORT%
set HEALTH_URL=%APP_URL%/
set CHROME_PATH="C:\Program Files\Google\Chrome\Application\chrome.exe"
set EDGE_PATH="C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

color 0B
echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║     HallSync - EXAM SEAT ALLOCATION SYSTEM                ║
echo  ║     Starting Production Environment...                    ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.

:: 1. Check if Docker CLI is found
where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo  [ERROR] Docker CLI command was not found in System PATH!
    echo.
    echo  Please ensure Docker Desktop for Windows is installed.
    echo  If recently installed, please restart your computer once.
    echo.
    pause
    exit /b 1
)

:: 2. Check & Wait for Docker Desktop Engine Daemon
echo  [1/5] Verifying Docker Desktop engine...
set /a docker_wait=0
:check_docker_daemon
docker info >nul 2>&1
if %errorlevel% neq 0 (
    set /a docker_wait+=1
    if !docker_wait! equ 1 (
        echo        Docker daemon is initializing... Waiting for engine...
        :: Try starting Docker Desktop if not already running
        tasklist /fi "imagename eq Docker Desktop.exe" 2>nul | findstr /I "Docker Desktop.exe" >nul
        if !errorlevel! neq 0 (
            if exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
                echo        Launching Docker Desktop application...
                start "" "%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
            )
        )
    )
    if !docker_wait! gtr 25 (
        echo.
        echo  [ERROR] Docker Desktop is NOT running or failed to initialize!
        echo.
        echo  Please:
        echo    1. Open Docker Desktop from the Windows Start Menu.
        echo    2. Wait until the whale icon in the bottom-right taskbar says 'Running'.
        echo    3. Then double-click START.bat again.
        echo.
        pause
        exit /b 1
    )
    timeout /t 2 /nobreak >nul
    <nul set /p =.
    goto check_docker_daemon
)
echo      ✓ Docker Desktop engine is active and ready.

:: 3. Detect Docker Compose Engine (Universal V1 / V2 Compatibility)
echo.
echo  [2/5] Detecting Docker Compose engine...
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

:: 4. Check for Offline Image Bundle (for Offline USB Deployments)
echo.
echo  [3/5] Checking container image cache...
set IMAGE_BUNDLE=""
if exist "docker-images\hallsync-images.tar" set IMAGE_BUNDLE="docker-images\hallsync-images.tar"
if exist "hallsync-images.tar" set IMAGE_BUNDLE="hallsync-images.tar"

docker images --format "{{.Repository}}" | findstr /I "hallsync-backend hallsync-app" >nul 2>&1
if %errorlevel% neq 0 (
    if not !IMAGE_BUNDLE!=="" (
        echo        Found offline image bundle: !IMAGE_BUNDLE!
        echo        Loading pre-packaged images (offline mode, no download needed)...
        docker load -i !IMAGE_BUNDLE!
        if !errorlevel! equ 0 (
            echo        ✓ Offline images loaded successfully.
        )
    )
)

:: Check for existing PostgreSQL version mismatch crash
docker ps -a --filter "name=hallsync-db" --format "{{.Status}}" | findstr /I "Exited" >nul 2>&1
if %errorlevel% equ 0 (
    docker logs hallsync-db 2>&1 | findstr /I "incompatible with server" >nul 2>&1
    if !errorlevel! equ 0 (
        echo.
        echo  [WARNING] PostgreSQL version mismatch detected from an older installation!
        echo  Auto-resetting database volume for PostgreSQL 16 clean startup...
        %COMPOSE_CMD% -p %PROJECT_NAME% down -v >nul 2>&1
    )
)

:: 5. Start Container Stack
echo.
echo  [4/5] Starting HallSync container cluster...
echo        (Database, Cache, Object Storage, Backend & Frontend)
echo.

:: Decide whether to run fast-start or initial build
docker images --format "{{.Repository}}" | findstr /I "hallsync-backend" >nul 2>&1
set BACKEND_IMG_EXISTS=%errorlevel%
docker images --format "{{.Repository}}" | findstr /I "hallsync-frontend" >nul 2>&1
set FRONTEND_IMG_EXISTS=%errorlevel%

if %BACKEND_IMG_EXISTS% equ 0 if %FRONTEND_IMG_EXISTS% equ 0 (
    echo        Images already present. Starting containers directly...
    %COMPOSE_CMD% -p %PROJECT_NAME% up -d
) else (
    echo        Images not found locally. Building containers from source...
    %COMPOSE_CMD% -p %PROJECT_NAME% up -d --build
)

if %errorlevel% neq 0 (
    echo.
    echo  [ERROR] Failed to start container stack.
    echo.
    echo  --- System Diagnostics ---
    %COMPOSE_CMD% -p %PROJECT_NAME% ps
    echo.
    echo  --- Backend Logs (Last 25 lines) ---
    docker logs hallsync-backend --tail 25 2>nul
    echo.
    echo  Troubleshooting Tips:
    echo    1. If port %FRONTEND_PORT% or %BACKEND_PORT% is occupied by another app (e.g. Tomcat/Oracle),
    echo       change PORT_FRONTEND or PORT_BACKEND in your .env file.
    echo    2. Run RESET_CONTAINERS.bat to clear corrupted volumes or stale containers.
    echo    3. Re-run START.bat.
    echo.
    pause
    exit /b 1
)

:: 6. Health Polling Loop
echo.
echo  [5/5] Waiting for application services to become ready...
echo        Checking: %APP_URL%

set /a attempt=0
:wait_loop
set /a attempt+=1
if !attempt! gtr 75 (
    echo.
    echo  [TIMEOUT] System took longer than usual to respond.
    echo.
    echo  --- Backend Container Status ---
    docker ps --filter "name=hallsync-backend"
    echo.
    echo  --- Recent Backend Logs ---
    docker logs hallsync-backend --tail 20 2>nul
    echo.
    pause
    exit /b 1
)

:: Check if backend died unexpectedly
docker ps -a --filter "name=hallsync-backend" --format "{{.Status}}" | findstr /I "Exited" >nul 2>&1
if %errorlevel% equ 0 (
    echo.
    echo  [ERROR] Backend container stopped unexpectedly!
    echo.
    echo  --- Backend Error Logs ---
    docker logs hallsync-backend --tail 30 2>nul
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
echo  ║   Portal (Web):  %APP_URL%                    ║
echo  ║   Direct API:    %BACKEND_URL%                    ║
echo  ║                                                           ║
echo  ║   Login Credentials:                                      ║
echo  ║     Username: coe1@krce.ac.in                             ║
echo  ║     Password: skm@8115                                     ║
echo  ║                                                           ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.
echo  Opening the Command Center in your browser...

:: Auto-open in Chrome, Edge, or Default Browser
if exist %CHROME_PATH% (
    start "" %CHROME_PATH% %APP_URL%
) else if exist %EDGE_PATH% (
    start "" %EDGE_PATH% %APP_URL%
) else (
    start "" "%APP_URL%"
)

echo.
echo  System is running smoothly. You can minimize this window.
echo  To shut down safely when finished, double-click STOP.bat.
echo.
pause
