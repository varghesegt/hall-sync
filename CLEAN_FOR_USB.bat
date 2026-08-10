@echo off
title HallSync - Clean Project for Pendrive Transfer
color 0E

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║     HallSync - USB PENDRIVE CLEANUP UTILITY               ║
echo  ║     Removing build artifacts and temporary files...       ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.

echo  [1/4] Removing Maven target build directory...
if exist "target" rmdir /s /q "target"
echo      ✓ target/ removed.

echo  [2/4] Removing frontend dist and build cache...
if exist "allocate-artisan\dist" rmdir /s /q "allocate-artisan\dist"
if exist "allocate-artisan\node" rmdir /s /q "allocate-artisan\node"
if exist "allocate-artisan\node_modules" rmdir /s /q "allocate-artisan\node_modules"
echo      ✓ Frontend build cache removed.

echo  [3/4] Removing temporary log and test files...
if exist "logs" rmdir /s /q "logs"
if exist "scratch" rmdir /s /q "scratch"
if exist "CLAIMS" rmdir /s /q "CLAIMS"
if exist "server_console.log" del /q "server_console.log"
if exist "*.xlsx" del /q "*.xlsx"
if exist "*.docx" del /q "*.docx"
if exist "*.doc" del /q "*.doc"
if exist "*.py" del /q "*.py"
echo      ✓ Temp logs and test doc files removed.

echo  [4/4] Ensuring runtime directory structure...
if not exist "uploads" mkdir "uploads"
if not exist "logs" mkdir "logs"
echo      ✓ Directory structure ready.

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║                                                           ║
echo  ║   CLEANUP COMPLETE!                                       ║
echo  ║   Your project folder is now lightweight and optimized    ║
echo  ║   for fast transfer to your Pendrive.                     ║
echo  ║                                                           ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.
pause
