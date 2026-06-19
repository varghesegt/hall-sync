@echo off
title HallSync - Database Restore
color 0D
echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║     HallSync - DATABASE RESTORE UTILITY                   ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.
echo  This will restore the database from a backup file.
echo.

if not exist "backups" (
    echo  [ERROR] No backups folder found!
    echo  Run BACKUP.bat first to create backups.
    echo.
    pause
    exit /b 1
)

echo  Available backup files:
echo  ─────────────────────
dir /b backups\*.sql 2>nul
echo.

if %ERRORLEVEL% NEQ 0 (
    echo  [ERROR] No backup files found in the backups folder!
    echo.
    pause
    exit /b 1
)

echo.
set /p BACKUP_FILE="  Enter the backup filename (e.g., exam_backup_2026-05-03_0930AM.sql): "

if not exist "backups\%BACKUP_FILE%" (
    echo.
    echo  [ERROR] File not found: backups\%BACKUP_FILE%
    echo  Please check the filename and try again.
    echo.
    pause
    exit /b 1
)

echo.
echo  ┌─────────────────────────────────────────────────────────┐
echo  │  WARNING: This will OVERWRITE current database data     │
echo  │  with the data from the backup file!                    │
echo  └─────────────────────────────────────────────────────────┘
echo.
set /p CONFIRM="  Type YES to confirm restore: "

if /I NOT "%CONFIRM%"=="YES" (
    echo.
    echo  Cancelled. No changes made.
    echo.
    pause
    exit /b 0
)

echo.
echo  Restoring database from: %BACKUP_FILE%
echo.

docker exec -i hallsync-db psql -U postgres -d exam_seat_allocation < "backups\%BACKUP_FILE%"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo  ╔═══════════════════════════════════════════════════════════╗
    echo  ║   DATABASE RESTORED SUCCESSFULLY!                         ║
    echo  ║   The system now has data from: %BACKUP_FILE%
    echo  ╚═══════════════════════════════════════════════════════════╝
) else (
    echo.
    echo  [ERROR] Restore failed!
    echo  Make sure the system is running (START.bat) first.
)
echo.
pause
