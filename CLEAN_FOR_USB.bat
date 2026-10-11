@echo off
title HallSync - Clean Project for Pendrive Transfer
color 0E

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║     HallSync - USB PENDRIVE CLEANUP UTILITY               ║
echo  ║     Removing build artifacts and temporary files...       ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.

echo  [1/4] Removing Maven target build cache...
if exist "target" rmdir /s /q "target"
echo      ✓ target/ removed.

echo  [2/4] Removing frontend node_modules and dev cache...
if exist "allocate-artisan\node" rmdir /s /q "allocate-artisan\node"
if exist "allocate-artisan\node_modules" rmdir /s /q "allocate-artisan\node_modules"
echo      ✓ Frontend dev cache removed.

echo  [3/4] Removing temporary session log files...
if exist "logs" rmdir /s /q "logs"
if exist "scratch" rmdir /s /q "scratch"
if exist "server_console.log" del /q "server_console.log"
if exist "data\parser-tmp" del /q "data\parser-tmp\*" 2>nul
echo      ✓ Temporary logs and parser caches cleared.

echo  [4/4] Ensuring runtime directory structure...
if not exist "uploads" mkdir "uploads"
if not exist "logs" mkdir "logs"
if not exist "backups" mkdir "backups"
echo      ✓ Directory structure ready.

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║                                                           ║
echo  ║   CLEANUP COMPLETE!                                       ║
echo  ║   Your project folder is now lightweight and optimized    ║
echo  ║   for fast transfer to your Pendrive.                     ║
echo  ║                                                           ║
echo  ║   TIP: If taking to an offline PC, run                    ║
echo  ║   EXPORT_FOR_OFFICE.bat first to bundle Docker images!    ║
echo  ║                                                           ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.
pause
