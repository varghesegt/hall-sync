@echo off
title HallSync - Database Restore Utility
color 0D

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║     HallSync - DATABASE RESTORE UTILITY                   ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.
echo  This utility restores the database from a backup SQL file.
echo.

:: 1. Ensure backups directory exists
if not exist "backups" mkdir "backups"

echo  Available Backup Files in backups\ folder:
echo  ──────────────────────────────────────────
dir /b backups\*.sql 2>nul
echo.

set /p BACKUP_INPUT="  Enter backup filename (e.g., exam_backup_2026-08-10_00-30-00.sql): "

if exist "%BACKUP_INPUT%" (
    set TARGET_FILE=%BACKUP_INPUT%
) else if exist "backups\%BACKUP_INPUT%" (
    set TARGET_FILE=backups\%BACKUP_INPUT%
) else (
    echo.
    echo  ❌ ERROR: File not found!
    echo  Could not locate "%BACKUP_INPUT%" or "backups\%BACKUP_INPUT%".
    echo  Please check the filename and try again.
    echo.
    pause
    exit /b 1
)

echo.
echo  ┌─────────────────────────────────────────────────────────┐
echo  │  WARNING: This will OVERWRITE current database data     │
echo  │  with data from: %TARGET_FILE%
echo  └─────────────────────────────────────────────────────────┘
echo.
set /p CONFIRM="  Type YES to confirm restore: "

if /I NOT "%CONFIRM%"=="YES" (
    echo.
    echo  Restore cancelled. No changes were made.
    echo.
    pause
    exit /b 0
)

echo.
echo  Restoring database from: %TARGET_FILE% ...
echo.

docker exec -i hallsync-db psql -U postgres -d exam_seat_allocation < "%TARGET_FILE%"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo  ╔═══════════════════════════════════════════════════════════╗
    echo  ║   🎉 DATABASE RESTORED SUCCESSFULLY!                      ║
    echo  ║   Restored from file: %TARGET_FILE%
    echo  ╚═══════════════════════════════════════════════════════════╝
) else (
    echo.
    echo  ❌ ERROR: Restore failed!
    echo  Make sure the system is running (START.bat) first.
)
echo.
pause
