import openpyxl

wb = openpyxl.Workbook()
ws = wb.active
ws.title = "Faculty"

# Headers
headers = ["Name", "Employee ID", "Department", "Designation", "Phone", "Email"]
ws.append(headers)

# Data
data = [
    ["Dr. Alice Smith", "EMP101", "CSE", "Professor", "9876543210", "alice@example.com"],
    ["Mr. Bob Johnson", "EMP102", "CSE", "Assistant Professor", "9876543211", "bob@example.com"],
    ["Dr. Charlie Davis", "EMP103", "IT", "Associate Professor", "9876543212", "charlie@example.com"],
    ["Ms. Diana Prince", "EMP104", "IT", "Assistant Professor", "9876543213", "diana@example.com"],
    ["Dr. Evan Wright", "EMP105", "ECE", "Professor", "9876543214", "evan@example.com"],
    ["Mr. Frank Miller", "EMP106", "ECE", "Assistant Professor", "9876543215", "frank@example.com"],
    ["Dr. Grace Lee", "EMP107", "MECH", "Professor", "9876543216", "grace@example.com"],
    ["Mr. Henry Ford", "EMP108", "MECH", "Associate Professor", "9876543217", "henry@example.com"],
    ["Dr. Ivy Chen", "EMP109", "CIVIL", "Professor", "9876543218", "ivy@example.com"],
    ["Mr. Jack Daniels", "EMP110", "CIVIL", "Assistant Professor", "9876543219", "jack@example.com"],
]

for row in data:
    ws.append(row)

# Save
wb.save("sample_faculty.xlsx")
print("Saved to sample_faculty.xlsx")
