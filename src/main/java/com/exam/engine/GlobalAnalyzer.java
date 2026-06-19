package com.exam.engine;

import com.exam.engine.model.Student;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PHASE 1 — GLOBAL ANALYSIS ENGINE
 * 
 * Performs high-level analysis of the student population to determine
 * department pressure, subject clustering, and overflow risk.
 */
public class GlobalAnalyzer {

    public record GlobalStats(
        Map<String, Long> deptCounts,
        Map<String, Double> deptPressure,
        List<String> dominantDepts,
        int totalStudents
    ) {}

    public GlobalStats analyze(List<Student> students) {
        int total = students.size();
        
        // 1. Calculate Department Distribution
        Map<String, Long> deptCounts = students.stream()
                .collect(Collectors.groupingBy(Student::department, Collectors.counting()));

        // 2. Calculate "Pressure" (Ratio of columns needed globally)
        // Since each hall has 5 columns, total columns = HallCount * 5.
        // Pressure = (DeptCount / TotalStudents) * 5
        Map<String, Double> deptPressure = new HashMap<>();
        deptCounts.forEach((dept, count) -> {
            deptPressure.put(dept, (double) count / total * 5);
        });

        // 3. Identify Dominant Departments (The ones that drive the ABABA pattern)
        List<String> dominantDepts = deptCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        return new GlobalStats(deptCounts, deptPressure, dominantDepts, total);
    }
}
