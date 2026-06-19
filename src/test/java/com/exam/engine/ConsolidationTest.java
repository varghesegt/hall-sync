package com.exam.engine;

import com.exam.engine.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class ConsolidationTest {

    @Test
    void test97StudentsConsolidation() {
        AllocationEngine engine = new AllocationEngine();
        
        List<Student> students = new ArrayList<>();
        // 50 CS students (10 columns)
        for (int i = 1; i <= 50; i++) {
            students.add(new Student("CS" + i, "CS", "CS101", "3", "2022"));
        }
        // 47 ECE students (9 full columns + 2 remainder)
        for (int i = 1; i <= 47; i++) {
            students.add(new Student("EC" + i, "ECE", "EC101", "3", "2022"));
        }
        
        // Provide 10 halls to see if it uses the minimum
        List<Hall> halls = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            halls.add(new Hall("H" + i, 25));
        }

        AllocationRequest request = new AllocationRequest(students, halls, Collections.emptyMap(), Collections.emptyMap(), 0, "97-test");
        AllocationResult result = engine.allocate(request);

        Map<String, Long> hallCounts = result.assignments().stream()
                .collect(Collectors.groupingBy(SeatAssignment::hallId, Collectors.counting()));

        System.out.println("Hall usage: " + hallCounts);
        
        // 97 students should use exactly 4 halls (ceil(97/25) = 4)
        assertEquals(4, hallCounts.size(), "Should use exactly 4 halls for 97 students");
        
        // Total students should still be 97
        assertEquals(97, result.assignments().size());
        
        // Each hall should have at most 25 students
        for (long count : hallCounts.values()) {
            assertTrue(count <= 25, "Hall count exceeds 25: " + count);
        }
    }
}
