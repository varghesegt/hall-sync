package com.exam.service;

import com.exam.entity.Faculty;
import com.exam.repository.master.TenantRepository;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class AppointmentOrderService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentOrderService.class);

    private final TenantRepository tenantRepository;

    public AppointmentOrderService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public byte[] generateAppointmentOrderWord(Faculty faculty, String role, String season, LocalDate date) throws IOException {
        XWPFDocument document = new XWPFDocument();

        // Add Header Table (Logo + College Name)
        addHeader(document);

        // Date
        XWPFParagraph datePara = document.createParagraph();
        datePara.setAlignment(ParagraphAlignment.RIGHT);
        datePara.setSpacingBefore(400);
        XWPFRun dateRun = datePara.createRun();
        LocalDate displayDate = date != null ? date : LocalDate.now();
        dateRun.setText("Date: " + displayDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        dateRun.setFontFamily("Times New Roman");
        dateRun.setFontSize(12);

        // To Section
        XWPFParagraph toPara = document.createParagraph();
        toPara.setSpacingBefore(200);
        XWPFRun toRun = toPara.createRun();
        toRun.setText("To,");
        toRun.setBold(true);
        toRun.setFontFamily("Times New Roman");
        toRun.setFontSize(12);

        XWPFParagraph addressPara = document.createParagraph();
        addressPara.setSpacingBefore(0);
        addressPara.setSpacingAfter(0);
        XWPFRun addressRun = addressPara.createRun();
        addressRun.setText(faculty.getName() + " (" + faculty.getEmployeeId() + ")");
        addressRun.setFontFamily("Times New Roman");
        addressRun.setFontSize(12);
        addressRun.addBreak();
        addressRun.setText((faculty.getDesignation() != null ? faculty.getDesignation() : "Staff") + ", Dept of " + (faculty.getDepartment() != null ? faculty.getDepartment() : "Unknown"));
        addressRun.addBreak();
        addressRun.setText(faculty.getCollegeName() != null ? faculty.getCollegeName() : "Institution");

        // Subject
        XWPFParagraph subPara = document.createParagraph();
        subPara.setSpacingBefore(400);
        XWPFRun subRun = subPara.createRun();
        String displaySeason = season != null && !season.isBlank() ? season : "End Semester Examinations";
        subRun.setText("Sub: Appointment as " + role + " for " + displaySeason + " - Reg.");
        subRun.setBold(true);
        subRun.setFontFamily("Times New Roman");
        subRun.setFontSize(12);

        // Salutation
        XWPFParagraph salPara = document.createParagraph();
        salPara.setSpacingBefore(400);
        XWPFRun salRun = salPara.createRun();
        salRun.setText("Sir/Madam,");
        salRun.setFontFamily("Times New Roman");
        salRun.setFontSize(12);

        // Body
        XWPFParagraph bodyPara = document.createParagraph();
        bodyPara.setSpacingBefore(200);
        bodyPara.setAlignment(ParagraphAlignment.BOTH);
        XWPFRun bodyRun = bodyPara.createRun();
        bodyRun.setText("By direction, I am pleased to appoint you as the ");
        
        XWPFRun roleRun = bodyPara.createRun();
        roleRun.setText(role);
        roleRun.setBold(true);
        
        XWPFRun bodyRun2 = bodyPara.createRun();
        bodyRun2.setText(" for the upcoming " + displaySeason + " at our college. ");
        bodyRun2.setText("Your expertise and professionalism have led to this appointment. You are requested to follow the standard operating procedures and guidelines prescribed by the examination board to ensure the smooth conduct of the examinations.");
        bodyRun.setFontFamily("Times New Roman");
        bodyRun.setFontSize(12);
        roleRun.setFontFamily("Times New Roman");
        roleRun.setFontSize(12);
        bodyRun2.setFontFamily("Times New Roman");
        bodyRun2.setFontSize(12);

        XWPFParagraph para2 = document.createParagraph();
        para2.setSpacingBefore(200);
        XWPFRun run2 = para2.createRun();
        run2.setText("You are requested to report to the Chief Superintendent / Exam Cell on the scheduled dates of your duty. Any changes or inability to attend should be communicated to the Exam Cell well in advance.");
        run2.setFontFamily("Times New Roman");
        run2.setFontSize(12);

        // Closing
        XWPFParagraph closePara = document.createParagraph();
        closePara.setSpacingBefore(800);
        closePara.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun closeRun = closePara.createRun();
        closeRun.setText("Yours faithfully,");
        closeRun.setFontFamily("Times New Roman");
        closeRun.setFontSize(12);
        closeRun.addBreak();
        closeRun.addBreak();
        closeRun.addBreak();
        closeRun.setText("Principal / Chief Superintendent");
        closeRun.setBold(true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.write(baos);
        document.close();
        return baos.toByteArray();
    }

    private void addHeader(XWPFDocument document) {
        XWPFTable titleTable = document.createTable(1, 2);
        titleTable.setWidth("100%");
        titleTable.removeBorders();

        XWPFTableCell logoCell = titleTable.getRow(0).getCell(0);
        XWPFParagraph logoPara = logoCell.getParagraphs().get(0);
        logoPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun logoRun = logoPara.createRun();

        String collegeName = "HallSync Institution";

        try {
            byte[] logoBytes = null;
            int pictureType = Document.PICTURE_TYPE_JPEG;
            String fileName = "logo.jpg";
            
            String tenantId = com.exam.config.tenant.TenantContext.getCurrentTenant();
            if (tenantId != null) {
                com.exam.config.tenant.TenantContext.clear();
                try {
                    java.util.Optional<com.exam.entity.master.Tenant> tenantOpt = tenantRepository.findByTenantId(tenantId);
                    if (tenantOpt.isPresent()) {
                        collegeName = tenantOpt.get().getCollegeName();
                        if (tenantOpt.get().getLogoBase64() != null && !tenantOpt.get().getLogoBase64().isEmpty()) {
                            String base64 = tenantOpt.get().getLogoBase64();
                            String[] parts = base64.split(",");
                            String imageString = parts.length > 1 ? parts[1] : parts[0];
                            logoBytes = java.util.Base64.getDecoder().decode(imageString);
                            
                            if (logoBytes != null && logoBytes.length > 8 && logoBytes[0] == (byte) 137 && logoBytes[1] == (byte) 80) {
                                pictureType = Document.PICTURE_TYPE_PNG;
                                fileName = "logo.png";
                            }
                        }
                    }
                } finally {
                    com.exam.config.tenant.TenantContext.setCurrentTenant(tenantId);
                }
            }

            if (logoBytes != null) {
                try {
                    java.io.ByteArrayInputStream imageBis = new java.io.ByteArrayInputStream(logoBytes);
                    java.awt.image.BufferedImage bimg = javax.imageio.ImageIO.read(imageBis);
                    if (bimg != null) {
                        double scale = Math.min(70.0 / bimg.getWidth(), 70.0 / bimg.getHeight());
                        int widthEMU = org.apache.poi.util.Units.toEMU(bimg.getWidth() * scale);
                        int heightEMU = org.apache.poi.util.Units.toEMU(bimg.getHeight() * scale);
                        
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        javax.imageio.ImageIO.write(bimg, "png", baos);
                        byte[] cleanPngBytes = baos.toByteArray();
                        
                        logoRun.addPicture(new java.io.ByteArrayInputStream(cleanPngBytes), Document.PICTURE_TYPE_PNG, "logo.png", widthEMU, heightEMU);
                    }
                } catch (Exception e) {
                    log.warn("Could not scale logo", e);
                }
            } else {
                try (var is = getClass().getResourceAsStream("/krce_logo.jpg")) {
                    if (is != null) {
                        logoRun.addPicture(is, Document.PICTURE_TYPE_JPEG, "krce_logo.jpg",
                            org.apache.poi.util.Units.toEMU(70), org.apache.poi.util.Units.toEMU(70));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not insert logo into Word", e);
        }

        XWPFTableCell textCell = titleTable.getRow(0).getCell(1);
        XWPFParagraph textPara = textCell.getParagraphs().get(0);
        textPara.setAlignment(ParagraphAlignment.CENTER);
        
        XWPFRun collegeRun = textPara.createRun();
        collegeRun.setText(collegeName);
        collegeRun.setFontSize(22);
        collegeRun.setBold(true);
        collegeRun.setColor("14326E");
        collegeRun.setFontFamily("Times New Roman");
        collegeRun.addBreak();

        XWPFRun titleRun = textPara.createRun();
        titleRun.setText("APPOINTMENT ORDER");
        titleRun.setFontSize(16);
        titleRun.setBold(true);
        titleRun.setColor("555555");
        titleRun.setFontFamily("Times New Roman");

        // Set columns width
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth tblW = titleTable.getCTTbl().addNewTblPr().addNewTblW();
        tblW.setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA);
        tblW.setW(java.math.BigInteger.valueOf(10000));
        
        titleTable.getRow(0).getCell(0).getCTTc().addNewTcPr().addNewTcW().setW(java.math.BigInteger.valueOf(2000));
        titleTable.getRow(0).getCell(1).getCTTc().addNewTcPr().addNewTcW().setW(java.math.BigInteger.valueOf(8000));

        // Add a line below header
        XWPFParagraph line = document.createParagraph();
        line.setBorderBottom(Borders.SINGLE);
        line.setSpacingAfter(200);
    }
}
