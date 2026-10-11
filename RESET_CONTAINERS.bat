@echo off
setlocal EnableDelayedExpansion
title HallSync - Reset Container Environment
color 0C

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

set PROJECT_NAME=hallsync
if exist .env (
    for /f "usebackq tokens=1,2 delims==" %%a in (.env) do (
        if "%%a"=="COMPOSE_PROJECT_NAME" set PROJECT_NAME=%%b
    )
)

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║     HallSync - EMERGENCY CONTAINER RESET UTILITY          ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.
echo  This tool will:
echo    1. Stop all stuck or running HallSync containers
echo    2. Remove stale network bindings and conflicting port locks
echo    3. Reset database volume (resolves Postgres version mismatch errors)
echo.
set /p CONFIRM="  Are you sure you want to reset containers and volumes? (YES/NO): "

if /I NOT "%CONFIRM%"=="YES" (
    echo.
    echo  Operation cancelled.
    pause
    exit /b 0
)

echo.
echo  Detecting Docker Compose engine...
set COMPOSE_CMD=docker compose
docker compose version >nul 2>&1
if %errorlevel% neq 0 (
    docker-compose version >nul 2>&1
    if %errorlevel% equ 0 (
        set COMPOSE_CMD=docker-compose
    )
)

echo  Tearing down containers, networks and volumes...
%COMPOSE_CMD% -p %PROJECT_NAME% down -v --remove-orphans

echo  Clearing temporary parser files...
if exist "data\parser-tmp" del /q "data\parser-tmp\*" 2>nul

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║   ✓ RESET COMPLETE!                                       ║
echo  ║   All container state and port locks have been cleared.    ║
echo  ║   You can now run START.bat to start fresh.               ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.
pause
