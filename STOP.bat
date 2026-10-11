@echo off
setlocal EnableDelayedExpansion
title HallSync - Stopping System
color 0C

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║     HallSync - SHUTTING DOWN SYSTEM                       ║
echo  ║     Saving data and stopping servers...                   ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.

:: Detect Docker Compose Command
set COMPOSE_CMD=docker compose
docker compose version >nul 2>&1
if %errorlevel% neq 0 (
    docker-compose version >nul 2>&1
    if %errorlevel% equ 0 (
        set COMPOSE_CMD=docker-compose
    )
)

:: 1. Stop Containers
echo  [1/2] Stopping all services gracefully...
%COMPOSE_CMD% stop
if %errorlevel% neq 0 (
    echo      [WARNING] Issues stopping some services. Attempting forceful cleanup...
    %COMPOSE_CMD% down
) else (
    echo      ✓ Services stopped safely.
)

:: 2. Cleanup
echo.
echo  [2/2] Cleaning up session temporary files...
if exist "data\parser-tmp" (
    del /q "data\parser-tmp\*" 2>nul
    echo      ✓ Temporary files cleared.
) else (
    echo      ✓ No temporary files to clear.
)

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║                                                           ║
echo  ║   SYSTEM SHUT DOWN SAFELY.                                ║
echo  ║   All your exam data is preserved in the database.        ║
echo  ║                                                           ║
echo  ║   To start again: Double-click START.bat                  ║
echo  ║                                                           ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.
pause
