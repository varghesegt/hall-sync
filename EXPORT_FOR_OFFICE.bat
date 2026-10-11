@echo off
setlocal EnableDelayedExpansion
title HallSync - Export Images for Offline / Pendrive Use
color 0B

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

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║     HallSync - EXPORT IMAGES FOR PENDRIVE / COE OFFICE    ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.
echo  This utility packages all 5 Docker containers into a single
echo  offline bundle so the COE Office computer can run HallSync
echo  without needing to download or build anything from internet!
echo.

:: 1. Verify Docker Engine
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo  [ERROR] Docker Desktop is not running. Please start Docker first.
    pause
    exit /b 1
)

:: 2. Ensure destination folder
if not exist "docker-images" mkdir "docker-images"

:: 3. Detect Compose CLI
set COMPOSE_CMD=docker compose
docker compose version >nul 2>&1
if %errorlevel% neq 0 (
    docker-compose version >nul 2>&1
    if %errorlevel% equ 0 set COMPOSE_CMD=docker-compose
)

:: 4. Build fresh images
echo  [1/2] Building fresh production application images...
%COMPOSE_CMD% -p hallsync build
if %errorlevel% neq 0 (
    echo  [ERROR] Image build failed. Please inspect build logs above.
    pause
    exit /b 1
)

:: 5. Export to tarball
set TAR_FILE=docker-images\hallsync-images.tar
echo.
echo  [2/2] Exporting all images to %TAR_FILE%...
echo        (This may take 1-2 minutes, please wait...)

docker save -o "%TAR_FILE%" hallsync-backend:latest hallsync-frontend:latest postgres:16-alpine redis:7.2-alpine cgr.dev/chainguard/minio:latest

if %errorlevel% equ 0 (
    echo.
    echo  ╔═══════════════════════════════════════════════════════════╗
    echo  ║   ✓ EXPORT COMPLETED SUCCESSFULLY!                        ║
    echo  ║                                                           ║
    echo  ║   Bundle created at: %TAR_FILE%
    echo  ║                                                           ║
    echo  ║   Instructions:                                           ║
    echo  ║   1. Copy this entire project folder to your Pendrive     ║
    echo  ║   2. On the COE Office PC, simply run START.bat           ║
    echo  ║   3. START.bat will detect and load these images instantly ║
    echo  ║      with ZERO internet required!                         ║
    echo  ╚═══════════════════════════════════════════════════════════╝
) else (
    echo.
    echo  [ERROR] Failed to export Docker images.
)

echo.
pause
