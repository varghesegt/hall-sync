import openpyxl
import random

wb = openpyxl.Workbook()
ws = wb.active
ws.title = "Faculty"

# Headers
headers = ["Name", "Employee ID", "Department", "Designation", "Phone", "Email"]
ws.append(headers)

# Data with guaranteed unique random employee IDs
rand_suffix = random.randint(1000, 9999)
data = [
    ["Dr. Alice Smith", f"EMP-{rand_suffix}-1", "CSE", "Professor", "9876543210", "alice@example.com"],
    ["Mr. Bob Johnson", f"EMP-{rand_suffix}-2", "CSE", "Assistant Professor", "9876543211", "bob@example.com"],
    ["Dr. Charlie Davis", f"EMP-{rand_suffix}-3", "IT", "Associate Professor", "9876543212", "charlie@example.com"],
    ["Ms. Diana Prince", f"EMP-{rand_suffix}-4", "IT", "Assistant Professor", "9876543213", "diana@example.com"],
    ["Dr. Evan Wright", f"EMP-{rand_suffix}-5", "ECE", "Professor", "9876543214", "evan@example.com"],
    ["Mr. Frank Miller", f"EMP-{rand_suffix}-6", "ECE", "Assistant Professor", "9876543215", "frank@example.com"],
    ["Dr. Grace Lee", f"EMP-{rand_suffix}-7", "MECH", "Professor", "9876543216", "grace@example.com"],
    ["Mr. Henry Ford", f"EMP-{rand_suffix}-8", "MECH", "Associate Professor", "9876543217", "henry@example.com"],
    ["Dr. Ivy Chen", f"EMP-{rand_suffix}-9", "CIVIL", "Professor", "9876543218", "ivy@example.com"],
    ["Mr. Jack Daniels", f"EMP-{rand_suffix}-10", "CIVIL", "Assistant Professor", "9876543219", "jack@example.com"],
]

for row in data:
    ws.append(row)

# Save
wb.save("sample_faculty_v2.xlsx")
print("Saved to sample_faculty_v2.xlsx")
