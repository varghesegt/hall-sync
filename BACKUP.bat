@echo off
echo ========================================================
echo        💾 DATABASE BACKUP UTILITY
echo ========================================================
echo.

:: Get current date in YYYY-MM-DD format safely
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set date_stamp=%%c-%%a-%%b)

set BACKUP_FILE=exam_database_backup_%date_stamp%.sql

echo [1/2] Taking a snapshot of the exam seating database...
:: Use the DATABASE container (hallsync-db), not the app container
docker exec -t hallsync-db pg_dump -U postgres -d exam_seat_allocation --clean --if-exists > "%BACKUP_FILE%"

if %errorlevel% neq 0 (
    echo.
    echo ❌ ERROR: Backup failed! Is the system running?
    echo.
    pause
    exit /b
)

echo [2/2] Verifying backup file...
echo ✅ Backup created successfully.

echo.
echo ========================================================
echo 🎉 BACKUP COMPLETE!
echo File: %BACKUP_FILE%
echo.
echo IMPORTANT: Copy this file to a Pendrive or Google Drive.
echo ========================================================
echo.
pause
