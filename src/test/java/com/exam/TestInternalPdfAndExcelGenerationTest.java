package com.exam;

import com.exam.dto.PdfAllocationView;
import com.exam.entity.AllocationBatch;
import com.exam.entity.BatchStatus;
import com.exam.entity.ExamSession;
import com.exam.service.impl.ExcelServiceImpl;
import com.exam.service.impl.PdfServiceImpl;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestInternalPdfAndExcelGenerationTest {

    @Test
    public void testPdfAndExcelGenerationWithNeonData() throws Exception {
        String jdbcUrl = "jdbc:postgresql://ep-small-bread-b4kpy8u6-pooler.c-6.us-east-2.aws.neon.tech/tenant_krce?sslmode=require";
        String user = "neondb_owner";
        String pass = "npg_SsVa9Wo7mlzw";
        UUID batchId = UUID.fromString("73d7076a-ee7d-4f7f-b867-ce86d444b386");

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {
            // 1. Fetch batch & exam session
            String batchSql = "SELECT b.id, b.status, b.allocation_request_id, b.version, " +
                    "s.id as session_id, s.season_id, s.name as session_name, s.exam_date, s.session, s.exam_type " +
                    "FROM allocation_batches b " +
                    "JOIN exam_sessions s ON b.exam_session_id = s.id " +
                    "WHERE b.id = CAST(? AS uuid)";
            AllocationBatch batch = null;
            ExamSession session = null;

            try (PreparedStatement ps = conn.prepareStatement(batchSql)) {
                ps.setString(1, batchId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        UUID sessId = UUID.fromString(rs.getString("session_id"));
                        Date d = rs.getDate("exam_date");
                        LocalDate examDate = d != null ? d.toLocalDate() : LocalDate.now();
                        session = new ExamSession(
                                sessId,
                                rs.getString("season_id"),
                                rs.getString("session_name"),
                                examDate,
                                rs.getString("session"),
                                rs.getString("exam_type")
                        );
                        batch = new AllocationBatch(
                                UUID.fromString(rs.getString("id")),
                                session,
                                UUID.fromString(rs.getString("allocation_request_id")),
                                BatchStatus.valueOf(rs.getString("status")),
                                rs.getInt("version")
                        );
                    } else {
                        throw new RuntimeException("Batch not found in Neon!");
                    }
                }
            }

            System.out.println("Loaded Batch: " + batch.getId() + ", Session Date: " + session.getExamDate() + ", Session: " + session.getSession());

            // 2. Fetch PdfAllocationViews using the exact query from AllocationRepository
            String allocSql = "SELECT a.hall_id, h.name as hall_name, a.seat_row, a.seat_col, " +
                    "s.register_number, s.name as student_name, s.department, s.subject_code, s.semester, s.regulation, a.risk_score " +
                    "FROM allocations a " +
                    "LEFT JOIN halls h ON a.hall_id = h.id " +
                    "LEFT JOIN students s ON a.student_id = s.id " +
                    "WHERE a.batch_id = CAST(? AS uuid) " +
                    "ORDER BY h.name ASC, a.seat_col ASC, a.seat_row ASC";

            List<PdfAllocationView> allocations = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(allocSql)) {
                ps.setString(1, batchId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        allocations.add(new PdfAllocationView(
                                rs.getString("hall_id"),
                                rs.getString("hall_name"),
                                rs.getInt("seat_row"),
                                rs.getString("seat_col"),
                                rs.getString("register_number"),
                                rs.getString("student_name"),
                                rs.getString("department"),
                                rs.getString("subject_code"),
                                rs.getString("semester"),
                                rs.getString("regulation"),
                                rs.getInt("risk_score")
                        ));
                    }
                }
            }

            System.out.println("Loaded " + allocations.size() + " allocations.");

            // 3. Test PDF generation
            System.out.println("--- Testing PdfServiceImpl.generateHallPdf ---");
            PdfServiceImpl pdfService = new PdfServiceImpl(null, null);
            ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
            try {
                pdfService.generateHallPdf(batch, allocations, pdfOut);
                System.out.println("PDF generation SUCCESS! Size: " + pdfOut.size() + " bytes");
            } catch (Exception e) {
                System.err.println("PDF generation FAILED:");
                e.printStackTrace();
                throw e;
            }

            // 4. Test Excel generation
            System.out.println("--- Testing ExcelServiceImpl.generateBatchExcel ---");
            ExcelServiceImpl excelService = new ExcelServiceImpl();
            ByteArrayOutputStream excelOut = new ByteArrayOutputStream();
            try {
                excelService.generateBatchExcel(batch, allocations, excelOut);
                System.out.println("Excel generation SUCCESS! Size: " + excelOut.size() + " bytes");
            } catch (Exception e) {
                System.err.println("Excel generation FAILED:");
                e.printStackTrace();
                throw e;
            }
        }
    }
}
