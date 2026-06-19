# 🏛️ HallSync — Exam Seat Allocation System
# USER MANUAL (For Office Staff)
---

## 📋 Table of Contents

1. [What This System Does](#what-this-system-does)
2. [One-Time Setup (First Time Only)](#one-time-setup)
3. [Daily Usage — How to Run Exams](#daily-usage)
4. [The Buttons on Your Desktop](#the-buttons)
5. [Step-by-Step: Running a Complete Exam](#step-by-step)
6. [Troubleshooting](#troubleshooting)
7. [Backup & Safety](#backup-safety)
8. [Important Notes](#important-notes)

---

## 1. What This System Does

This software automatically assigns exam seats to students, ensuring:
- ✅ No two students from the same department sit next to each other
- ✅ Students don't get the same hall repeatedly across exam days
- ✅ Each hall has exactly 25 seats (5 rows × 5 columns)
- ✅ **Visual Override:** Manually move students or edit register numbers after allocation
- ✅ **Integrity Audit:** Mathematical proof of zero subject-adjacency violations for auditors
- ✅ Generates ready-to-print **Hall Slips**, **Account Opening Sheets**, and **Security Certificates**

You upload a student list → select halls → click allocate → download the PDFs.

---

## 2. One-Time Setup (First Time Only)

### What the COE Office Computer Needs:

| Requirement | How to Install |
|---|---|
| **Windows 10 or 11** | Already installed on office PCs |
| **Docker Desktop** | Download from: https://www.docker.com/products/docker-desktop/ |
| **Google Chrome** | Download from: https://www.google.com/chrome/ |

### Installing Docker Desktop:

1. Go to https://www.docker.com/products/docker-desktop/
2. Click **"Download for Windows"**
3. Run the installer → click **Next** on every screen
4. **Restart the computer** when asked
5. After restart, Docker Desktop will open — wait until it says **"Docker Desktop is running"**
6. You only need to do this ONCE

> ⚠️ **IMPORTANT:** Docker Desktop must be running whenever you use this system.
> Set it to "Start when Windows starts" in Docker Desktop Settings → General.

### First Run:

1. Copy the entire `EXAM-SEAT-FINAL` folder to `C:\HallSync\` on the office computer
2. Double-click **START.bat**
3. Wait 1-2 minutes (first time takes longer as it downloads components)
4. Chrome will open automatically to the login page

---

## 3. Daily Usage — How to Run Exams

### Morning Routine (Every Exam Day):

```
Step 1: Turn on the computer
Step 2: Wait for Docker Desktop icon (whale) to appear in taskbar
Step 3: Double-click START.bat
Step 4: Chrome opens → Login → Upload → Allocate → Print
Step 5: At end of day, double-click STOP.bat
```

### Login Credentials:

| Field | Value |
|---|---|
| **Username** | `coe1@krce.ac.in` |
| **Password** | `skm@8115` |

---

## 4. The Buttons on Your Desktop

You have 6 batch files (.bat). Here's what each does:

| Button (File) | When to Use | Safe? |
|---|---|---|
| 📗 **START.bat** | Start the system every morning | ✅ Always safe |
| 📕 **STOP.bat** | Stop the system at end of day | ✅ Always safe |
| 📘 **BACKUP.bat** | Save a copy of all data before cleanup | ✅ Always safe |
| 📒 **HEALTH_CHECK.bat** | Check if everything is working | ✅ Always safe |
| 📙 **RESTORE_DATABASE.bat** | Recover data from a backup file | ⚠️ Overwrites current data |
| 📓 **CLEAN_DB.bat** | Erase all exam data (start fresh) | ❌ DESTRUCTIVE — asks for confirmation |

### Rule of Thumb:
- **Every morning:** START.bat
- **Every evening:** STOP.bat  
- **Every week:** BACKUP.bat
- **End of semester:** BACKUP.bat → then CLEAN_DB.bat

---

## 5. Step-by-Step: Running a Complete Exam

### Step 1: Prepare the Student List (Excel)

Create an Excel file (.xlsx or .xls) with student data. The system is smart — it can read many formats. But the cleanest format is:

| Register Number | Department |
|---|---|
| 22CSE001 | CSE |
| 22ECE002 | ECE |
| 22MECH003 | MECH |
| 22EEE004 | EEE |

> **TIP:** You can use the same format that the COE already uses. The system's smart parser
> can extract register numbers and departments from most Excel formats.

### Step 2: Open the Portal

1. Double-click **START.bat** (or if already running, open Chrome and go to `http://localhost:8081`)
2. Login with `coe1@krce.ac.in` / `skm@8115`

### Step 3: Create Exam Session

1. On the dashboard, click **"New Session"**
2. Enter:
   - **Exam Name** (e.g., "End Semester - April 2026")
   - **Exam Date** (select from calendar)
   - **Session** (FN = Forenoon / AN = Afternoon)
3. Click **Create**

### Step 4: Upload Student List

1. Click on the exam session you just created
2. Click **"Upload"** button
3. Select your Excel file
4. The system will parse and show you the student count
5. Verify the count matches your records

### Step 5: Select Halls

1. After upload, you'll see the **Room Selector**
2. Select the halls you want to use for this exam:
   - LH (Lecture Halls): 201-220, 301-321, 401-420, 501-515
   - SH (Seminar Halls): 301-303, 501-503
   - CONF (Conference Halls): 1-3
3. Select enough halls for your student count (25 students per hall)

> **Formula:** Number of halls needed = Total students ÷ 25 (round up)
> Example: 150 students → you need at least 6 halls

### Step 6: Allocate Seats

1. Click **"Allocate"** button
2. Wait 5-10 seconds
3. The system will show: "Allocation Complete — X students assigned to Y halls"

### Step 7: Download & Print

You get three downloads:

| Download | What It Is | Who Gets It |
|---|---|---|
| **📄 Hall Slip (PDF)** | Seating arrangement per hall | Print & give to hall invigilators |
| **📊 Allocation Sheet (Excel)** | Full student-to-seat mapping | Office record |
| **📊 Count Opening (Excel)** | Summary of students per hall per department | For QP distribution |
| **🛡️ Integrity Certificate (PDF)** | Proof of 0% subject adjacency | For NAAC/NBA auditors |

1. Click **"Download PDF"** → Print all pages
2. Click **"Download Excel"** → Save to office folder
3. Click **"Download Count Opening"** → Save for QP counting
4. Click **"Download Integrity Certificate"** → Keep for inspections

### Step 8: Visual Adjustments (Optional)

If you need to move a student or fix a typo:
1. Scroll down to the **"Allocation Preview"** panel
2. Click the **"Edit"** button (top right of the panel)
3. **To Move/Swap:** Drag any student seat and drop it onto another seat. They will swap instantly
4. **To Edit:** Double-click any register number → type the new value → press **Enter**
5. Click **"Done Editing"** to save. All PDFs and Excels will update automatically

### Step 9: Verify Integrity

1. Look for the **"Integrity Verified"** shield on the allocation card
2. Click **"Download Integrity Certificate (PDF)"**
3. This document proves to auditors that the seating is mathematically secure

### Step 10: Done!

Distribute the printed hall slips to the invigilators. 🎉

---

## 6. Troubleshooting

### Problem: "START.bat says Docker is not running"

**Solution:**
1. Look for the whale icon 🐳 in the taskbar (bottom-right)
2. If not there, open **Docker Desktop** from Start Menu
3. Wait 1-2 minutes until it says "running"
4. Run START.bat again

### Problem: "Chrome shows blank page or error"

**Solution:**
1. Wait 30 more seconds — the system might still be booting
2. Press **Ctrl + Shift + R** in Chrome (hard refresh)
3. If still not working, run STOP.bat, then START.bat again

### Problem: "Login is not working"

**Solution:**
- Username is: `coe1@krce.ac.in` (with @ symbol)
- Password is: `skm@8115`
- Make sure CAPS LOCK is off

### Problem: "Allocation shows an error"

**Solution:**
1. Check: did you upload students first?
2. Check: did you select enough halls? (need 1 hall per 25 students)
3. If still failing, wait 60 seconds and try again (cooldown period)

### Problem: "System is very slow"

**Solution:**
1. Run HEALTH_CHECK.bat to verify everything is running
2. Close other heavy programs
3. If problem persists: STOP.bat → restart computer → START.bat

### Problem: "I accidentally deleted data"

**Solution:**
1. If you ran BACKUP.bat recently, run RESTORE_DATABASE.bat
2. Select the backup file to restore from
3. The data will be recovered

### Problem: "Need to move to a new computer"

**Solution:**
1. Run BACKUP.bat on old computer
2. Copy entire `EXAM-SEAT-FINAL` folder + `backups` folder to new computer
3. Install Docker Desktop on new computer
4. Run START.bat on new computer
5. Run RESTORE_DATABASE.bat with the backup file

---

## 7. Backup & Safety

### Weekly Backup Routine:

1. Double-click **BACKUP.bat**
2. A file like `exam_backup_2026-05-03_0930AM.sql` will be created in the `backups` folder
3. **Copy this file to Google Drive or a pendrive**

### Before Semester Cleanup:

1. Run **BACKUP.bat** (save the backup!)
2. Then run **CLEAN_DB.bat**
3. Type `YES` when asked
4. All exam data is erased — halls are preserved
5. You're ready for the new semester

### What Gets Preserved During Cleanup:
- ✅ All 85 hall/room configurations (permanent)
- ❌ All exam sessions (deleted)
- ❌ All student data (deleted)
- ❌ All allocations (deleted)

---

## 8. Important Notes

### Security:
- Only people with the login credentials can access the system
- The system only works on `localhost` (the same computer) — it's not accessible from the internet
- If you need to access from another computer on the same network, contact the developer

### Multi-Computer Access:
- Currently set up for **single computer use**
- If 3 staff members need to use it simultaneously, all PCs must connect to the same database
- Contact the developer for multi-computer setup

### System Requirements:
- **Minimum:** 4 GB RAM, Windows 10
- **Recommended:** 8 GB RAM, Windows 10/11
- **Storage:** ~2 GB for Docker + application

### Support Contact:
- **Developer:** [Your Name]
- **Phone:** [Your Phone Number]
- **Email:** [Your Email]

---

*Last Updated: May 2026*
*Version: 1.1.0-ELITE*
