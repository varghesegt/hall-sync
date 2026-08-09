package com.exam;

import com.exam.config.tenant.TenantContext;
import com.exam.entity.Allocation;
import com.exam.entity.AllocationBatch;
import com.exam.entity.ExamSession;
import com.exam.entity.Student;
import com.exam.repository.AllocationBatchRepository;
import com.exam.repository.AllocationRepository;
import com.exam.repository.ExamSessionRepository;
import com.exam.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
public class InternalStudentAllocationAnalysisTest {

    @Autowired
    private AllocationBatchRepository batchRepository;

    @Autowired
    private AllocationRepository allocationRepository;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @Transactional
    public void analyzeUnallocatedStudents() {
        // Try master and krce
        String[] tenants = {null, "krce"};
        for (String t : tenants) {
            TenantContext.setCurrentTenant(t);
            System.out.println("==========================================================================");
            System.out.println(" TENANT CONTEXT: " + (t != null ? t : "DEFAULT / PUBLIC"));
            System.out.println("==========================================================================");

            List<AllocationBatch> batches = batchRepository.findAll();
            System.out.println("Total Batches: " + batches.size());

            for (AllocationBatch batch : batches) {
                ExamSession session = batch.getExamSession();
                System.out.println("\n--------------------------------------------------------------------------");
                System.out.println("BATCH ID       : " + batch.getId());
                System.out.println("STATUS         : " + batch.getStatus());
                System.out.println("EXAM SESSION ID: " + (session != null ? session.getId() : "N/A"));
                System.out.println("EXAM DATE      : " + (session != null ? session.getExamDate() : "N/A"));
                System.out.println("EXAM TYPE      : " + (session != null ? session.getExamType() : "N/A"));
                System.out.println("SEASON ID      : " + (session != null ? session.getSeasonId() : "N/A"));
                System.out.println("SESSION NAME   : " + (session != null ? session.getName() : "N/A"));

                if (session == null) continue;

                List<Student> parsedStudents = studentRepository.findByExamSessionId(session.getId());
                System.out.println("TOTAL PARSED STUDENTS IN BATCH: " + parsedStudents.size());

                List<Allocation> allocations = allocationRepository.findByBatchId(batch.getId());
                Set<UUID> allocatedStudentIds = allocations.stream()
                        .map(a -> a.getStudent() != null ? a.getStudent().getId() : null)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                System.out.println("TOTAL ALLOCATED STUDENTS IN BATCH: " + allocatedStudentIds.size());

                List<Student> unallocatedStudents = parsedStudents.stream()
                        .filter(s -> !allocatedStudentIds.contains(s.getId()))
                        .collect(Collectors.toList());

                System.out.println("UNALLOCATED STUDENTS COUNT: " + unallocatedStudents.size());

                if (!unallocatedStudents.isEmpty()) {
                    System.out.println("--------------------------------------------------------------------------------------------------");
                    System.out.println(String.format("%-5s | %-16s | %-25s | %-12s | %-12s | %-25s", "S.No", "Register No", "Student Name", "Department", "Sub Code", "Subject Name"));
                    System.out.println("--------------------------------------------------------------------------------------------------");
                    int idx = 1;
                    for (Student s : unallocatedStudents) {
                        System.out.println(String.format("%-5d | %-16s | %-25s | %-12s | %-12s | %-25s",
                                idx++,
                                s.getRegisterNumber() != null ? s.getRegisterNumber() : "N/A",
                                s.getName() != null ? s.getName() : "N/A",
                                s.getDepartment() != null ? s.getDepartment() : "N/A",
                                s.getSubjectCode() != null ? s.getSubjectCode() : "N/A",
                                s.getSubjectName() != null ? s.getSubjectName() : "N/A"
                        ));
                    }
                    System.out.println("--------------------------------------------------------------------------------------------------");
                }
            }
            
            // Also print all ExamSessions directly
            List<ExamSession> sessions = examSessionRepository.findAll();
            System.out.println("\nAll ExamSessions in DB (" + sessions.size() + "):");
            for (ExamSession s : sessions) {
                List<Student> stList = studentRepository.findByExamSessionId(s.getId());
                System.out.println("Session ID: " + s.getId() + " | Date: " + s.getExamDate() + " | Name: " + s.getName() + " | Type: " + s.getExamType() + " | Students: " + stList.size());
            }
        }
    }
}
