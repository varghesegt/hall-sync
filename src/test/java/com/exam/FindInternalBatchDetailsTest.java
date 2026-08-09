package com.exam;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.*;
import java.util.*;

@SpringBootTest
public class FindInternalBatchDetailsTest {

    @Test
    public void findUnallocatedStudentsAcrossDatabases() {
        String[] dbNames = {"exam_seat_allocation", "tenant_krce"};
        String urlPrefix = "jdbc:postgresql://localhost:5432/";
        String user = "postgres";
        String pass = "jittu007";

        for (String db : dbNames) {
            String jdbcUrl = urlPrefix + db;
            System.out.println("==========================================================================");
            System.out.println(" INSPECTING POSTGRESQL DATABASE: " + db);
            System.out.println("==========================================================================");

            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {
                // 1. Check all tables
                List<String> tables = new ArrayList<>();
                DatabaseMetaData md = conn.getMetaData();
                try (ResultSet rs = md.getTables(null, null, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        tables.add(rs.getString("TABLE_NAME"));
                    }
                }
                System.out.println("Tables in " + db + ": " + tables);

                // 2. Query allocation_batches / exam_sessions / students / allocations
                if (tables.contains("allocation_batches") && tables.contains("exam_sessions")) {
                    String queryBatches = "SELECT b.id as batch_id, b.status, s.id as session_id, s.exam_date, s.name as session_name, s.exam_type " +
                            "FROM allocation_batches b " +
                            "JOIN exam_sessions s ON b.exam_session_id = s.id";
                    try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(queryBatches)) {
                        while (rs.next()) {
                            System.out.println("\n--- BATCH FOUND ---");
                            System.out.println("Batch ID    : " + rs.getString("batch_id"));
                            System.out.println("Status      : " + rs.getString("status"));
                            System.out.println("Session ID  : " + rs.getString("session_id"));
                            System.out.println("Exam Date   : " + rs.getDate("exam_date"));
                            System.out.println("Exam Name   : " + rs.getString("session_name"));
                            System.out.println("Exam Type   : " + rs.getString("exam_type"));

                            String batchIdStr = rs.getString("batch_id");
                            String sessionIdStr = rs.getString("session_id");

                            // Query students for this session
                            List<Map<String, String>> studentList = new ArrayList<>();
                            String stQuery = "SELECT id, register_number, name, department, subject_code, subject_name FROM students WHERE exam_session_id = CAST(? AS uuid)";
                            try (PreparedStatement ps = conn.prepareStatement(stQuery)) {
                                ps.setString(1, sessionIdStr);
                                try (ResultSet srs = ps.executeQuery()) {
                                    while (srs.next()) {
                                        Map<String, String> m = new HashMap<>();
                                        m.put("id", srs.getString("id"));
                                        m.put("reg", srs.getString("register_number"));
                                        m.put("name", srs.getString("name"));
                                        m.put("dept", srs.getString("department"));
                                        m.put("subCode", srs.getString("subject_code"));
                                        m.put("subName", srs.getString("subject_name"));
                                        studentList.add(m);
                                    }
                                }
                            }
                            System.out.println("Total Parsed Students: " + studentList.size());

                            // Query allocated student IDs
                            Set<String> allocatedStudentIds = new HashSet<>();
                            String allocQuery = "SELECT student_id FROM allocations WHERE batch_id = CAST(? AS uuid)";
                            try (PreparedStatement ps = conn.prepareStatement(allocQuery)) {
                                ps.setString(1, batchIdStr);
                                try (ResultSet ars = ps.executeQuery()) {
                                    while (ars.next()) {
                                        allocatedStudentIds.add(ars.getString("student_id"));
                                    }
                                }
                            }
                            System.out.println("Total Allocated Students: " + allocatedStudentIds.size());

                            // Find unallocated students
                            List<Map<String, String>> unallocated = new ArrayList<>();
                            for (Map<String, String> st : studentList) {
                                if (!allocatedStudentIds.contains(st.get("id"))) {
                                    unallocated.add(st);
                                }
                            }

                            System.out.println("\nUNALLOCATED STUDENTS COUNT: " + unallocated.size());
                            if (!unallocated.isEmpty()) {
                                System.out.println("--------------------------------------------------------------------------------------------------");
                                System.out.println(String.format("%-5s | %-18s | %-25s | %-12s | %-12s | %-25s", "S.No", "Register No", "Student Name", "Department", "Sub Code", "Subject Name"));
                                System.out.println("--------------------------------------------------------------------------------------------------");
                                int idx = 1;
                                for (Map<String, String> u : unallocated) {
                                    System.out.println(String.format("%-5d | %-18s | %-25s | %-12s | %-12s | %-25s",
                                            idx++,
                                            u.get("reg") != null ? u.get("reg") : "N/A",
                                            u.get("name") != null ? u.get("name") : "N/A",
                                            u.get("dept") != null ? u.get("dept") : "N/A",
                                            u.get("subCode") != null ? u.get("subCode") : "N/A",
                                            u.get("subName") != null ? u.get("subName") : "N/A"
                                    ));
                                }
                                System.out.println("--------------------------------------------------------------------------------------------------");
                            }
                        }
                    }
                }

                if (tables.contains("students")) {
                    String cntQuery = "SELECT count(*) FROM students";
                    try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(cntQuery)) {
                        if (rs.next()) {
                            System.out.println("Direct Students Count in " + db + ": " + rs.getInt(1));
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("Could not query DB " + db + ": " + e.getMessage());
            }
        }
    }
}
