package com.exam.engine;

import com.exam.engine.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ColumnEngineTest {

    private AllocationEngine engine;

    @BeforeEach
    void setup() {
        engine = new AllocationEngine();
    }

    private AllocationRequest createRequest(Map<String, Integer> deptCounts, int numHalls) {
        List<Student> students = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : deptCounts.entrySet()) {
            for (int i = 1; i <= entry.getValue(); i++) {
                String reg = entry.getKey() + String.format("%03d", i);
                students.add(new Student(reg, entry.getKey(), "CS101", "UNKNOWN", "UNKNOWN"));
            }
        }
        
        List<Hall> halls = new ArrayList<>();
        for (int i = 1; i <= numHalls; i++) {
            halls.add(new Hall("H" + i, 25));
        }

        return new AllocationRequest(students, halls, Collections.emptyMap(), Collections.emptyMap(), 0, null);
    }

    @Test
    void testColumnWiseFilling() {
        // 25 students of A
        AllocationRequest req = createRequest(Map.of("A", 25), 1);
        AllocationResult res = engine.allocate(req);

        assertEquals(25, res.assignments().size());
        
        // Check first 5 are I
        for (int i = 0; i < 5; i++) {
            assertEquals("I", res.assignments().get(i).col());
            assertEquals(i + 1, res.assignments().get(i).row());
        }
        // Check next 5 are II
        for (int i = 5; i < 10; i++) {
            assertEquals("II", res.assignments().get(i).col());
            assertEquals((i % 5) + 1, res.assignments().get(i).row());
        }
    }

    @Test
    void testAlternationPatternHall1() {
        // 15 of A, 10 of B
        AllocationRequest req = createRequest(Map.of("A", 15, "B", 10), 1);
        AllocationResult res = engine.allocate(req);

        // Expect Hall 1: A B A B A
        // I: A, II: B, III: A, IV: B, V: A
        Map<String, String> colDepts = getColDepts(res, "H1");
        assertEquals("A", colDepts.get("I"));
        assertEquals("B", colDepts.get("II"));
        assertEquals("A", colDepts.get("III"));
        assertEquals("B", colDepts.get("IV"));
        assertEquals("A", colDepts.get("V"));
    }

    @Test
    void testFlipPatternHall2() {
        // 50 students total (2 Halls)
        // Hall 1 (A B A B A) needs 3A, 2B
        // Hall 2 (B A B A B) needs 2A, 3B
        // Total: 5A, 5B = 25A, 25B
        AllocationRequest req = createRequest(Map.of("A", 25, "B", 25), 2);
        AllocationResult res = engine.allocate(req);

        // Hall 1: A B A B A
        // Hall 2: B A B A B
        Map<String, String> hall1Cols = getColDepts(res, "H1");
        assertEquals("A", hall1Cols.get("I"));
        
        Map<String, String> hall2Cols = getColDepts(res, "H2");
        assertEquals("B", hall2Cols.get("I"));
        assertEquals("A", hall2Cols.get("II"));
        assertEquals("B", hall2Cols.get("III"));
        assertEquals("A", hall2Cols.get("IV"));
        assertEquals("B", hall2Cols.get("V"));
    }

    @Test
    void testMultiDeptHandling() {
        // A: 20, B: 20, C: 5, D: 5
        // Hall 1 target: A B A B C (C is secondary)
        AllocationRequest req = createRequest(new LinkedHashMap<>() {{
            put("A", 20);
            put("B", 20);
            put("C", 5);
            put("D", 5);
        }}, 2);
        
        AllocationResult res = engine.allocate(req);
        
        Map<String, String> hall1Cols = getColDepts(res, "H1");
        // Due to strictly deterministic heap polling, Hall 1 gets A B A B A
        assertFalse(hall1Cols.values().contains("C"), "Hall 1 should NOT contain Dept C as it is placed in H2");
        
        Map<String, String> hall2Cols = getColDepts(res, "H2");
        assertTrue(hall2Cols.values().contains("C"), "Hall 2 should contain Dept C");
        assertTrue(hall2Cols.values().contains("D"), "Hall 2 should contain Dept D");
    }

    @Test
    void testContinuousFlow() {
        // A: 10
        AllocationRequest req = createRequest(Map.of("A", 10), 2);
        AllocationResult res = engine.allocate(req);
        
        // H1 C1: A001-A005
        // H1 C2: A006-A010
        List<String> aRegs = res.assignments().stream()
                .filter(a -> a.registerNumber().startsWith("A"))
                .map(SeatAssignment::registerNumber)
                .toList();
        
        assertEquals("A001", aRegs.get(0));
        assertEquals("A010", aRegs.get(9));
    }

    @Test
    void testLowCountMerging() {
        // A: 3, B: 2
        AllocationRequest req = createRequest(Map.of("A", 3, "B", 2), 1);
        AllocationResult res = engine.allocate(req);
        
        // Should be in I
        assertEquals(5, res.assignments().size());
        assertEquals("I", res.assignments().get(0).col());
        assertEquals("I", res.assignments().get(4).col());
        
        assertEquals("A001", res.assignments().get(0).registerNumber());
        assertEquals("B001", res.assignments().get(3).registerNumber());
    }

    private Map<String, String> getColDepts(AllocationResult res, String hallId) {
        Map<String, String> map = new HashMap<>();
        for (SeatAssignment a : res.assignments()) {
            if (a.hallId().equals(hallId)) {
                // Assuming students are in the same dept for the whole column for simplicity of test check
                // In merge case this might pick the last one
                map.put(a.col(), getDeptFromReg(a.registerNumber()));
            }
        }
        return map;
    }
    
    private String getDeptFromReg(String reg) {
        return reg.substring(0, 1);
    }
}
