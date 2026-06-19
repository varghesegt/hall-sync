@echo off
title HallSync - Stopping System
color 0C

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║     HallSync - SHUTTING DOWN SYSTEM                       ║
echo  ║     Saving data and stopping servers...                   ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.

:: 1. Stop Containers
echo  [1/2] Stopping all services...
docker-compose stop
if %errorlevel% neq 0 (
    echo      [WARNING] Issues stopping some services. Attempting forceful cleanup...
    docker-compose down
) else (
    echo      ✓ Services stopped.
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
