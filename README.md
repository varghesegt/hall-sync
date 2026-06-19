# 🏛️ HallSync — Office Portal
### Exam Seat Allocation System (v1.1.0)

---

## ⚡ Quick Start (For COE Office Staff)

> **📖 Full instructions: Read [COE_USER_MANUAL.md](COE_USER_MANUAL.md)**

### Prerequisites
- **Docker Desktop** installed → [Download Here](https://www.docker.com/products/docker-desktop/)
- **Google Chrome** → [Download Here](https://www.google.com/chrome/)

### Start the System
1. Open **Docker Desktop** (wait for it to say "running")
2. Double-click **`START.bat`**
3. Chrome opens automatically → Login with `coe1@krce.ac.in` / `skm@8115`

### Stop the System
- Double-click **`STOP.bat`**

---

## 📁 Files in This Folder

| File | Purpose |
|---|---|
| 📗 `START.bat` | Start the system |
| 📕 `STOP.bat` | Stop the system |
| 📘 `BACKUP.bat` | Save a backup of all exam data |
| 📙 `RESTORE_DATABASE.bat` | Restore data from a backup |
| 📒 `HEALTH_CHECK.bat` | Check if everything is working |
| 📓 `CLEAN_DB.bat` | Erase all data (new semester) |
| 📖 `COE_USER_MANUAL.md` | Complete usage guide |

---

## 💎 Features

- **Smart Seat Allocation**: Automatically prevents same-department students from sitting adjacent
- **Hall History Tracking**: Ensures students get different halls across exam days  
- **PDF Hall Slips**: Print-ready seating plans for each hall
- **Excel Reports**: Allocation sheets + Count Opening for QP distribution
- **Multi-User Safe**: 3 staff can use simultaneously without data conflicts

---

## 🏗️ Technical Stack

| Component | Technology |
|---|---|
| Backend | Java 17 + Spring Boot 3.2.5 |
| Frontend | React + Vite + TypeScript |
| Database | PostgreSQL 16 |
| Reports | iTextPDF + Apache POI |
| Deployment | Docker Compose |

---

## 📜 Maintenance

- **Weekly**: Run `BACKUP.bat` and save the backup file to Google Drive
- **End of Semester**: Run `BACKUP.bat` → then `CLEAN_DB.bat` to start fresh
- **Moving Computers**: Copy this entire folder + install Docker Desktop on new PC

---

**HallSync — Exam Seat Allocation System | May 2026**
