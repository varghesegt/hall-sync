@echo off
title HallSync - Database Cleanup
color 0E
echo.
echo  ╔═══════════════════════════════════════════════════════════╗
echo  ║     HallSync - DATABASE CLEANUP                           ║
echo  ╚═══════════════════════════════════════════════════════════╝
echo.
echo  ┌─────────────────────────────────────────────────────────┐
echo  │  WARNING: This will DELETE all exam session data!       │
echo  │                                                         │
echo  │  The following will be ERASED:                          │
echo  │    - All exam sessions                                  │
echo  │    - All student uploads                                │
echo  │    - All seat allocations                               │
echo  │    - All audit logs                                     │
echo  │                                                         │
echo  │  Hall/Room data will NOT be affected.                   │
echo  │                                                         │
echo  │  TIP: Run BACKUP.bat first to save a copy!             │
echo  └─────────────────────────────────────────────────────────┘
echo.
echo  Are you sure you want to clean the database?
echo.

set /p CONFIRM="  Type YES to confirm, or press Enter to cancel: "

if /I NOT "%CONFIRM%"=="YES" (
    echo.
    echo  Cancelled. No data was deleted.
    echo.
    pause
    exit /b 0
)

echo.
echo  Cleaning database...

docker exec hallsync-db psql -U postgres -d exam_seat_allocation -c "TRUNCATE TABLE exam_sessions, students, allocation_batches, allocations, audit_log, uploaded_files, allocation_locks CASCADE;"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo  ╔═══════════════════════════════════════════════════════════╗
    echo  ║   DATABASE CLEANED SUCCESSFULLY!                          ║
    echo  ║   Hall/Room data was preserved.                           ║
    echo  ║   You can now start fresh with new exam sessions.         ║
    echo  ╚═══════════════════════════════════════════════════════════╝
) else (
    echo.
    echo  [ERROR] Cleanup failed!
    echo  Make sure the system is running (START.bat) first.
)
echo.
pause
