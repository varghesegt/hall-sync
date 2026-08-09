@echo off
title HallSync - Database Backup Utility
color 0A

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║     HallSync - DATABASE BACKUP UTILITY                    ║
echo  ║     Creating a complete snapshot of all exam data...       ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.

:: 1. Ensure backups directory exists
if not exist "backups" mkdir "backups"

:: 2. Generate robust, locale-independent timestamp
for /f %%a in ('powershell -command "Get-Date -Format 'yyyy-MM-dd_HH-mm-ss'"') do set STAMP=%%a
set BACKUP_FILE=backups\exam_backup_%STAMP%.sql

echo  [1/2] Creating database snapshot...
docker exec -t hallsync-db pg_dump -U postgres -d exam_seat_allocation --clean --if-exists > "%BACKUP_FILE%"

if %errorlevel% neq 0 (
    echo.
    echo  ❌ ERROR: Backup failed!
    echo  Please ensure the system is running (START.bat) before taking a backup.
    echo.
    pause
    exit /b 1
)

echo        ✓ Snapshot saved to: %BACKUP_FILE%

echo.
echo  [2/2] Verifying backup integrity...
if exist "%BACKUP_FILE%" (
    echo        ✓ Backup verified successfully.
) else (
    echo        ❌ ERROR: Backup file could not be verified.
    pause
    exit /b 1
)

echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║                                                           ║
echo  ║   🎉 BACKUP COMPLETED SUCCESSFULLY!                       ║
echo  ║                                                           ║
echo  ║   Saved to: %BACKUP_FILE%
echo  ║   TIP: Copy this SQL file to a Pendrive or Google Drive.  ║
echo  ║                                                           ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.
pause
