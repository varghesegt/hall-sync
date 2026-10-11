@echo off
setlocal EnableDelayedExpansion
title HallSync - System Health Check
color 0A

:: --- PATH RECOVERY ---
if exist "%ProgramFiles%\Docker\Docker\resources\bin\docker.exe" (
    set "PATH=%ProgramFiles%\Docker\Docker\resources\bin;%PATH%"
)
if exist "%LOCALAPPDATA%\Docker\resources\bin\docker.exe" (
    set "PATH=%LOCALAPPDATA%\Docker\resources\bin;%PATH%"
)
if exist "C:\Program Files\Docker\Docker\resources\bin\docker.exe" (
    set "PATH=C:\Program Files\Docker\Docker\resources\bin;%PATH%"
)

set FRONTEND_PORT=8080
set BACKEND_PORT=8081
if exist .env (
    for /f "usebackq tokens=1,2 delims==" %%a in (.env) do (
        if "%%a"=="PORT_FRONTEND" set FRONTEND_PORT=%%b
        if "%%a"=="PORT_BACKEND" set BACKEND_PORT=%%b
    )
)

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║     HallSync - SYSTEM HEALTH CHECK                        ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.

:: Check Docker Engine
echo  [1/5] Checking Docker Desktop status...
docker info >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo        ✓ Docker Desktop engine is active
) else (
    echo        ✗ Docker Desktop is NOT running!
    echo          → Open Docker Desktop from the Start Menu
    echo.
    pause
    exit /b 1
)

:: Check Database Container
echo.
echo  [2/5] Checking Database Server (PostgreSQL)...
docker ps --filter "name=hallsync-db" --format "{{.Status}}" | findstr /I "Up" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo        ✓ Database server container is running
) else (
    echo        ✗ Database server is NOT running!
    echo          → Run START.bat to launch the cluster
    echo.
    pause
    exit /b 1
)

:: Check Cache Container
echo.
echo  [3/5] Checking Cache and Storage Services (Redis \u0026 MinIO)...
docker ps --filter "name=hallsync-redis" --format "{{.Status}}" | findstr /I "Up" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo        ✓ Redis cache engine is running
) else (
    echo        ! Redis cache engine is not active
)

docker ps --filter "name=hallsync-minio" --format "{{.Status}}" | findstr /I "Up" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo        ✓ MinIO S3 storage is running
) else (
    echo        ! MinIO S3 storage is not active
)

:: Check App Containers
echo.
echo  [4/5] Checking Application Servers (Backend and Frontend)...
docker ps --filter "name=hallsync-backend" --format "{{.Status}}" | findstr /I "Up" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    docker ps --filter "name=hallsync-app" --format "{{.Status}}" | findstr /I "Up" >nul 2>&1
)
if %ERRORLEVEL% EQU 0 (
    echo        ✓ Backend application engine is running
) else (
    echo        ✗ Backend application server is NOT running!
    echo          → Run START.bat to start the system
    echo.
    pause
    exit /b 1
)

docker ps --filter "name=hallsync-frontend" --format "{{.Status}}" | findstr /I "Up" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo        ✓ Frontend web server is running
) else (
    echo        ! Frontend web server not detected (Backend direct mode active)
)

:: Check Database Connection
echo.
echo  [5/5] Checking Database Connection...
docker exec hallsync-db pg_isready -U postgres >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo        ✓ Database is accepting connections
) else (
    echo        ✗ Database is not accepting connections!
    echo          → Try STOP.bat then START.bat
    echo.
    pause
    exit /b 1
)

:: Show stats
echo.
echo  ─────────────────────────────────────────────────────────
echo  System Statistics:
echo  ─────────────────────────────────────────────────────────

set HALL_COUNT=0
set SESSION_COUNT=0
set ALLOC_COUNT=0

for /f %%a in ('docker exec hallsync-db psql -U postgres -d exam_seat_allocation -t -c "SELECT COUNT(*) FROM halls;" 2^>nul') do set HALL_COUNT=%%a
echo    Halls configured:     %HALL_COUNT%

for /f %%a in ('docker exec hallsync-db psql -U postgres -d exam_seat_allocation -t -c "SELECT COUNT(*) FROM exam_sessions;" 2^>nul') do set SESSION_COUNT=%%a
echo    Exam sessions:        %SESSION_COUNT%

for /f %%a in ('docker exec hallsync-db psql -U postgres -d exam_seat_allocation -t -c "SELECT COUNT(*) FROM allocations;" 2^>nul') do set ALLOC_COUNT=%%a
echo    Total allocations:    %ALLOC_COUNT%

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║                                                           ║
echo  ║   ALL SYSTEMS ARE HEALTHY ✓                               ║
echo  ║                                                           ║
echo  ║   Portal (Web):  http://localhost:%FRONTEND_PORT%                    ║
echo  ║   Direct API:    http://localhost:%BACKEND_PORT%                    ║
echo  ║   Login:         coe1@krce.ac.in / skm@8115               ║
echo  ║                                                           ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.
pause
