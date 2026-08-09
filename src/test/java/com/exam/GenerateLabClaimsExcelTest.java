package com.exam;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

public class GenerateLabClaimsExcelTest {

    @Test
    public void create100PlusComprehensiveLabClaimsExcel() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Form Responses 1");

        // Header Styling
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        String[] headers = {
            "Timestamp", "Email Address", "Date of Exam ", "No. of sessions", "Degree ",
            "Subject Type", "Subject Code ", "Subject Name ", "Department of Candidate ", "Semester ",
            "Ext. Examiner type", "No. of Cands. Regd. ", "No. of Cands. Examined ",
            "Title of External Examiner ", "Name of External Examiner ", "Phone No. of External Examiner",
            "Department of External Examiner", "Designation of External Examiner", "College of External Examiner",
            "Bank  Account No. (External Examiner)", "Bank Name  (External Examiner)", "IFSC Code  (External Examiner)", "Bank Branch  (External Examiner)",
            "Title of  Internal   Examiner ", "Name of Internal Examiner ", "Phone No. of Internal Examiner",
            "Department of Internal Examiner", "Designation of Internal Examiner",
            "Bank  Account No. (Internal Examiner)", "Bank Name  (Internal Examiner)", "IFSC Code  (Internal Examiner)", "Bank Branch (Internal Examiner)",
            "Title of Skilled Asst. ", "Name of Skilled Asst.(Initial at End)", "Phone No. of Skilled Asst.",
            "Department of Skilled Asst.", "Designation of Skilled Asst.",
            "Bank  Account No. (Skilled Asst.)", "Bank Name  (Skilled Asst.)", "IFSC Code  (Skilled Asst.)", "Bank Branch (Skilled Asst.)",
            "Title of Lab Technician", "Name of Lab Technician (Initial at End)", "Phone No. of Lab Technician",
            "Department of Lab Technician", "Bank Account No. (Lab Technician)", "Bank Name (Lab Technician)",
            "IFSC Code of  (Lab Technician)", "Bank Branch (Lab Technician)"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Realistic Data Sources
        String[] dates = {"2026-03-30", "05.06.2026", "06.06.2026", "07.06.2026", "08.06.2026", "09.06.2026", "10.06.2026", "12.06.2026", "15.06.2026", "16.06.2026"};
        String[] sessions = {"Both FN and AN", "FN", "AN"};
        String[] degrees = {"UG", "PG"};
        String[] subjectTypes = {"Project / Mini Project / PRBL", "Regular Lab"};

        String[][] subjects = {
            {"UCS1811", "Project Work Phase II", "CSE", "VIII"},
            {"UAM1811", "AI & Deep Learning Laboratory", "CSE(AIML)", "VIII"},
            {"UCB1811", "Business Systems & Analytics Lab", "CSBS", "VIII"},
            {"UAD1811", "Data Science & Machine Learning Lab", "AI&DS", "VIII"},
            {"CS3461", "Operating Systems Laboratory", "CSE", "IV"},
            {"EC3461", "Communication Systems Laboratory", "ECE", "IV"},
            {"EE3461", "Power Electronics & Drives Laboratory", "EEE", "IV"},
            {"ME3461", "CAD / CAM Laboratory", "MECH", "VI"},
            {"CE3461", "Concrete & Highway Engineering Lab", "CIVIL", "IV"},
            {"IT3461", "Web Technology Laboratory", "IT", "IV"},
            {"BM3461", "Biomedical Instrumentation Laboratory", "BME", "IV"},
            {"CS3611", "Compiler Design Laboratory", "CSE", "VI"},
            {"EC3611", "VLSI Design Laboratory", "ECE", "VI"},
            {"EE3611", "Renewable Energy Systems Lab", "EEE", "VI"},
            {"ME3611", "Heat Transfer & Dynamics Lab", "MECH", "VI"},
            {"CS3811", "Cloud Computing Laboratory", "CSE", "VIII"},
            {"MA3251", "Discrete Mathematics Lab", "CSE", "II"},
            {"PH3251", "Engineering Physics Laboratory", "ECE", "II"}
        };

        String[] extColleges = {
            "UNIVERSITY COLLEGE OF ENGINEERING - BIT CAMPUS TRICHY (8100)",
            "SONA COLLEGE OF TECHNOLOGY,SALEM (6178)",
            "SARANATHAN COLLEGE OF ENGINEERING (8138)",
            "KPR INSTITUTE OF ENGINEERING & TECHNOLOGY (7113)",
            "PSNA COLLEGE OF ENGINEERING & TECHNOLOGY,DINDUGAL (9213)",
            "MAM COLLEGE OF ENGINEERING AND TECHNOLOGY (8120)",
            "NATIONAL INSTITUTE OF TECHNOLOGY TRICHY (8001)",
            "COIMBATORE INSTITUTE OF TECHNOLOGY (7100)",
            "THIAGARAJAR COLLEGE OF ENGINEERING,MADURAI (5901)",
            "KUMARAGURU COLLEGE OF TECHNOLOGY,COIMBATORE (7114)",
            "SASTRA DEEMED UNIVERSITY, THANJAVUR (8201)",
            "BISHOP HEBER COLLEGE, TRICHY (8105)"
        };

        String[] titles = {"Mr.", "Mrs.", "Dr.", "Ms."};
        String[] designations = {"Associate Professor", "Assistant Professor", "Professor"};

        String[] extNames = {
            "C Ramasamy Sankar Ram", "VIJAYARAJESWARI R", "Ravimaran S", "B. Nagarajan",
            "S. Pushpalatha", "K. Vimalanathan", "M. Arunkumar", "P. Narayanasamy",
            "A. Ravisankar", "R. Arulraj", "M. Dhanapal", "I. Vijay Arasu",
            "B. Imayavan", "B. Pooja Batt", "D. Vaishnavi", "S. Sivananthan",
            "K. Ramesh Kumar", "N. Senthil Nathan", "P. Murugesan", "G. Balasubramanian",
            "R. Meenakshi", "S. Chitra", "M. Kavitha", "K. Anitha", "T. Vijayalakshmi"
        };

        String[] intNames = {
            "S. Jagadeeswari", "B. Sathiya", "P. Bhavani", "T. John Peter",
            "R. Kasthuri Rengan", "M. Prabakaran", "K. Senthilkumar", "G. Surya",
            "A. Mohamed Aadhil", "S. Karthikeyan", "V. Rajesh Kumar", "N. Dinesh",
            "P. Anand", "M. Subha", "K. Priya", "R. Revathi", "S. Preethi",
            "T. Saravanan", "D. Gokulakrishnan", "K. Venkatesh"
        };

        String[] skilledNames = {
            "G. Surya", "M. Shanmugam", "K. Manikandan", "P. Velmurugan",
            "S. Ganesan", "R. Murugan", "T. Balaji", "V. Kumar",
            "A. Rajasekar", "D. Kannan", "M. Vijay", "K. Saravanan"
        };

        String[] techNames = {
            "R. Ramesh", "S. Suresh", "P. Loganathan", "K. Selvam",
            "M. Durai", "T. Elango", "A. Boopathi", "V. Pandian",
            "C. Chinnasamy", "N. Natarajan", "S. Ramachandran"
        };

        String[] bankNames = {"Canara Bank", "Indian Overseas Bank", "HDFC Bank", "ICICI Bank", "State Bank of India", "Axis Bank", "City Union Bank"};
        String[] ifscCodes = {"CNRB0002841", "IOBA0001542", "HDFC0000240", "ICIC0000128", "SBIN0001234", "UTIB0000412", "CIUB0000185"};
        String[] branches = {"Samayapuram", "Tiruchirappalli Town", "Thillai Nagar", "Kattur", "Trichy Main", "Srirangam", "Lalgudi"};

        Random random = new Random(42);

        // Generate 100 Unique Practical Exam Sessions
        for (int r = 1; r <= 100; r++) {
            Row row = sheet.createRow(r);

            // Group 10 unique subjects per date cleanly
            int dateIdx = (r - 1) / 10;
            int subjIdx = (r - 1) % subjects.length;

            String[] subj = subjects[subjIdx];
            String examDt = dates[dateIdx % dates.length];
            String sess = (r == 1) ? "FN" : sessions[r % sessions.length];
            String deg = (r == 1) ? "PG" : degrees[r % degrees.length];
            String subjType = (r == 1) ? "Regular Lab" : subjectTypes[r % subjectTypes.length];

            int reg = 45;
            int pres = 45;

            String extName = extNames[(r - 1) % extNames.length];
            String intName = intNames[(r - 1) % intNames.length];
            String skilledName = skilledNames[(r - 1) % skilledNames.length];
            String techName = techNames[(r - 1) % techNames.length];

            String extCollege = extColleges[(r - 1) % extColleges.length];
            String titleExt = (r == 1) ? "Mr." : titles[r % titles.length];
            String titleInt = (r == 1) ? "Mrs." : titles[(r + 1) % titles.length];
            String titleSkilled = (r == 1) ? "Ms." : titles[(r + 2) % titles.length];
            String titleTech = "Mr.";

            String desigExt = (r == 1) ? "Associate Professor" : designations[r % designations.length];
            String desigInt = (r == 1) ? "Professor" : designations[(r + 1) % designations.length];

            int bankIdx = (r - 1) % bankNames.length;

            // Col 0: Timestamp
            row.createCell(0).setCellValue("2026-03-30 10:01:" + String.format("%02d", (r % 60)));
            // Col 1: Email Address
            row.createCell(1).setCellValue("examiner" + r + "@gmail.com");
            // Col 2: Date of Exam
            row.createCell(2).setCellValue(examDt);
            // Col 3: No. of sessions
            row.createCell(3).setCellValue(sess);
            // Col 4: Degree
            row.createCell(4).setCellValue(deg);
            // Col 5: Subject Type
            row.createCell(5).setCellValue(subjType);
            // Col 6: Subject Code
            row.createCell(6).setCellValue(subj[0]);
            // Col 7: Subject Name
            row.createCell(7).setCellValue(subj[1]);
            // Col 8: Department of Candidate
            row.createCell(8).setCellValue(subj[2]);
            // Col 9: Semester
            row.createCell(9).setCellValue(subj[3]);
            // Col 10: Ext. Examiner type
            row.createCell(10).setCellValue("External");
            // Col 11: No. of Cands. Regd.
            row.createCell(11).setCellValue(reg);
            // Col 12: No. of Cands. Examined
            row.createCell(12).setCellValue(pres);

            // Col 13: Title of External Examiner
            row.createCell(13).setCellValue(titleExt);
            // Col 14: Name of External Examiner
            row.createCell(14).setCellValue(extName);
            // Col 15: Phone No. of External Examiner
            row.createCell(15).setCellValue((r == 1) ? "9842100123" : "9842" + String.format("%06d", 100000 + r * 123));
            // Col 16: Department of External Examiner
            row.createCell(16).setCellValue(subj[2]);
            // Col 17: Designation of External Examiner
            row.createCell(17).setCellValue(desigExt);
            // Col 18: College of External Examiner
            row.createCell(18).setCellValue((r == 1) ? "UNIVERSITY COLLEGE OF ENGINEERING - BIT CAMPUS TRICHY (8100)" : extCollege);
            // Col 19: Bank Account No. (External Examiner)
            row.createCell(19).setCellValue((r == 1) ? "30948500321" : "30948" + String.format("%06d", 500000 + r * 321));
            // Col 20: Bank Name (External Examiner)
            row.createCell(20).setCellValue((r == 1) ? "Canara Bank" : bankNames[bankIdx]);
            // Col 21: IFSC Code (External Examiner)
            row.createCell(21).setCellValue((r == 1) ? "CNRB0002841" : ifscCodes[bankIdx]);
            // Col 22: Bank Branch (External Examiner)
            row.createCell(22).setCellValue((r == 1) ? "Samayapuram" : branches[bankIdx]);

            // Col 23: Title of Internal Examiner
            row.createCell(23).setCellValue(titleInt);
            // Col 24: Name of Internal Examiner
            row.createCell(24).setCellValue(intName);
            // Col 25: Phone No. of Internal Examiner
            row.createCell(25).setCellValue((r == 1) ? "9443200456" : "9443" + String.format("%06d", 200000 + r * 456));
            // Col 26: Department of Internal Examiner
            row.createCell(26).setCellValue(subj[2]);
            // Col 27: Designation of Internal Examiner
            row.createCell(27).setCellValue(desigInt);
            // Col 28: Bank Account No. (Internal Examiner)
            row.createCell(28).setCellValue((r == 1) ? "20145600654" : "20145" + String.format("%06d", 600000 + r * 654));
            // Col 29: Bank Name (Internal Examiner)
            row.createCell(29).setCellValue((r == 1) ? "Indian Overseas Bank" : bankNames[(bankIdx + 1) % bankNames.length]);
            // Col 30: IFSC Code (Internal Examiner)
            row.createCell(30).setCellValue((r == 1) ? "IOBA0001542" : ifscCodes[(bankIdx + 1) % ifscCodes.length]);
            // Col 31: Bank Branch (Internal Examiner)
            row.createCell(31).setCellValue((r == 1) ? "Tiruchirappalli Town" : branches[(bankIdx + 1) % branches.length]);

            // Col 32: Title of Skilled Asst.
            row.createCell(32).setCellValue(titleSkilled);
            // Col 33: Name of Skilled Asst.(Initial at End)
            row.createCell(33).setCellValue(skilledName);
            // Col 34: Phone No. of Skilled Asst.
            row.createCell(34).setCellValue((r == 1) ? "9789300789" : "9789" + String.format("%06d", 300000 + r * 789));
            // Col 35: Department of Skilled Asst.
            row.createCell(35).setCellValue(subj[2]);
            // Col 36: Designation of Skilled Asst.
            row.createCell(36).setCellValue("Lab Instructor");
            // Col 37: Bank Account No. (Skilled Asst.)
            row.createCell(37).setCellValue((r == 1) ? "10842700987" : "10842" + String.format("%06d", 700000 + r * 987));
            // Col 38: Bank Name (Skilled Asst.)
            row.createCell(38).setCellValue((r == 1) ? "HDFC Bank" : bankNames[(bankIdx + 2) % bankNames.length]);
            // Col 39: IFSC Code (Skilled Asst.)
            row.createCell(39).setCellValue((r == 1) ? "HDFC0000240" : ifscCodes[(bankIdx + 2) % ifscCodes.length]);
            // Col 40: Bank Branch (Skilled Asst.)
            row.createCell(40).setCellValue((r == 1) ? "Thillai Nagar" : branches[(bankIdx + 2) % branches.length]);

            // Col 41: Title of Lab Technician
            row.createCell(41).setCellValue(titleTech);
            // Col 42: Name of Lab Technician (Initial at End)
            row.createCell(42).setCellValue(techName);
            // Col 43: Phone No. of Lab Technician
            row.createCell(43).setCellValue((r == 1) ? "9944400111" : "9944" + String.format("%06d", 400000 + r * 111));
            // Col 44: Department of Lab Technician
            row.createCell(44).setCellValue(subj[2]);
            // Col 45: Bank Account No. (Lab Technician)
            row.createCell(45).setCellValue((r == 1) ? "50148800222" : "50148" + String.format("%06d", 800000 + r * 222));
            // Col 46: Bank Name (Lab Technician)
            row.createCell(46).setCellValue((r == 1) ? "ICICI Bank" : bankNames[(bankIdx + 3) % bankNames.length]);
            // Col 47: IFSC Code of (Lab Technician)
            row.createCell(47).setCellValue((r == 1) ? "ICIC0000128" : ifscCodes[(bankIdx + 3) % ifscCodes.length]);
            // Col 48: Bank Branch (Lab Technician)
            row.createCell(48).setCellValue((r == 1) ? "Kattur" : branches[(bankIdx + 3) % branches.length]);
        }

        // Auto-fit column widths
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 22 * 256);
        }

        // Save to Downloads folder
        File fileDownloads = new File("C:/Users/Asus/Downloads/Comprehensive_Lab_Claims_Form_Responses.xlsx");
        try (FileOutputStream fos = new FileOutputStream(fileDownloads)) {
            workbook.write(fos);
        }

        // Save to target templates resources
        File fileResources = new File("src/main/resources/templates/Comprehensive_Lab_Claims_Form_Responses.xlsx");
        fileResources.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(fileResources)) {
            workbook.write(fos);
        }

        workbook.close();
        System.out.println("Successfully generated 100-row 49-column Lab Claims Excel with exact Row 1 specifications!");
    }
}
