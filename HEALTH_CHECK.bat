@echo off
title HallSync - System Health Check
color 0A
echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║     HallSync - SYSTEM HEALTH CHECK                        ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.

:: Check Docker
echo  [1/4] Checking Docker Desktop...
docker info >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo        ✓ Docker Desktop is running
) else (
    echo        ✗ Docker Desktop is NOT running!
    echo          → Open Docker Desktop from Start Menu
    echo.
    pause
    exit /b 1
)

:: Check Database Container
echo.
echo  [2/4] Checking Database Server...
docker ps --filter "name=hallsync-db" --format "{{.Status}}" | findstr /I "Up" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo        ✓ Database server is running
) else (
    echo        ✗ Database server is NOT running!
    echo          → Run START.bat to start the system
    echo.
    pause
    exit /b 1
)

:: Check App Container
echo.
echo  [3/4] Checking Application Server...
docker ps --filter "name=hallsync-app" --format "{{.Status}}" | findstr /I "Up" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo        ✓ Application server is running
) else (
    echo        ✗ Application server is NOT running!
    echo          → Run START.bat to start the system
    echo.
    pause
    exit /b 1
)

:: Check Database Connection
echo.
echo  [4/4] Checking Database Connection...
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

:: Count halls
for /f %%a in ('docker exec hallsync-db psql -U postgres -d exam_seat_allocation -t -c "SELECT COUNT(*) FROM halls;"') do set HALL_COUNT=%%a
echo    Halls configured:     %HALL_COUNT%

:: Count exam sessions  
for /f %%a in ('docker exec hallsync-db psql -U postgres -d exam_seat_allocation -t -c "SELECT COUNT(*) FROM exam_sessions;"') do set SESSION_COUNT=%%a
echo    Exam sessions:        %SESSION_COUNT%

:: Count allocations
for /f %%a in ('docker exec hallsync-db psql -U postgres -d exam_seat_allocation -t -c "SELECT COUNT(*) FROM allocations;"') do set ALLOC_COUNT=%%a
echo    Total allocations:    %ALLOC_COUNT%

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║                                                           ║
echo  ║   ALL SYSTEMS ARE HEALTHY ✓                               ║
echo  ║                                                           ║
echo  ║   Portal: http://localhost:8081                            ║
echo  ║   Login:  coe1@krce.ac.in / skm@8115                      ║
echo  ║                                                           ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.
pause
