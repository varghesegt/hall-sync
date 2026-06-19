package com.exam.engine;

import com.exam.engine.model.AllocationViolation;
import com.exam.engine.model.Student;
import com.exam.engine.model.ViolationType;

import java.util.*;
import java.util.stream.Collectors;

public class DeadlockResolver {

    public record InterleavedResult(List<Student> students, List<AllocationViolation> violations) {}

    public InterleavedResult interleave(List<Student> students, String seed) {
        List<AllocationViolation> violations = new ArrayList<>();
        
        // Group by department + subjectCode (LinkedHashMap for strict determinism in map creation, though we sort keys later)
        Map<String, Queue<Student>> grouped = new LinkedHashMap<>();
        for (Student s : students) {
            String subject = s.subjectCode() != null ? s.subjectCode() : "UNKNOWN";
            String groupKey = s.department() + "_" + subject;
            grouped.computeIfAbsent(groupKey, k -> new LinkedList<>()).add(s);
        }

        // Sort each queue by registerNumber ASC
        for (Queue<Student> queue : grouped.values()) {
            List<Student> list = new ArrayList<>(queue);
            list.sort(Comparator.comparing(Student::registerNumber));
            
            // Intra-Department Deterministic Shift (Maintains Sequence)
            if (seed != null && !seed.isEmpty() && !list.isEmpty()) {
                int offset = Math.abs(seed.hashCode()) % list.size();
                java.util.Collections.rotate(list, -offset);
            }
            
            queue.clear();
            queue.addAll(list);
        }

        // PriorityQueue with STRICT deterministic comparator
        // Primary: size DESC, Secondary: key ASC
        PriorityQueue<Map.Entry<String, Queue<Student>>> heap = new PriorityQueue<>(
            Comparator.<Map.Entry<String, Queue<Student>>>comparingInt(e -> e.getValue().size())
                    .reversed()
                    .thenComparing(Map.Entry::getKey)
        );

        for (Map.Entry<String, Queue<Student>> entry : grouped.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                heap.add(entry);
            }
        }

        List<Student> result = new ArrayList<>();
        String lastDept = null;
        String lastSubject = null;

        while (!heap.isEmpty()) {
            Map.Entry<String, Queue<Student>> selected = null;
            List<Map.Entry<String, Queue<Student>>> bypassed = new ArrayList<>();

            // Priority 1: Different Department AND Different Subject
            while (!heap.isEmpty()) {
                Map.Entry<String, Queue<Student>> entry = heap.poll();
                Student peek = entry.getValue().peek();
                if (peek != null) {
                    String peekSubject = peek.subjectCode() != null ? peek.subjectCode() : "UNKNOWN";
                    boolean diffDept = lastDept == null || !peek.department().equals(lastDept);
                    boolean diffSubj = lastSubject == null || !peekSubject.equals(lastSubject);
                    
                    if (diffDept && diffSubj) {
                        selected = entry;
                        break;
                    }
                }
                bypassed.add(entry);
            }

            // Priority 2: Different Department only (Fallback for common subjects like EM-1)
            if (selected == null) {
                heap.addAll(bypassed);
                bypassed.clear();
                
                while (!heap.isEmpty()) {
                    Map.Entry<String, Queue<Student>> entry = heap.poll();
                    Student peek = entry.getValue().peek();
                    if (peek != null) {
                        boolean diffDept = lastDept == null || !peek.department().equals(lastDept);
                        
                        if (diffDept) {
                            selected = entry;
                            break;
                        }
                    }
                    bypassed.add(entry);
                }
            }

            // Priority 3: Last Resort (Only identical departments left)
            if (selected == null) {
                heap.addAll(bypassed);
                bypassed.clear();
                selected = heap.poll();
            }

            // Put bypassed back into heap
            heap.addAll(bypassed);

            // Grouping constraint: fill in blocks of 5 (one column) to satisfy 
            // the "One department per column" requirement.
            for (int i = 0; i < 5 && selected != null && !selected.getValue().isEmpty(); i++) {
                Student s = selected.getValue().poll();

                if (lastDept != null && lastDept.equals(s.department())) {
                    violations.add(new AllocationViolation(ViolationType.CONSECUTIVE_DEPT_ALLOWED, 
                        "Consecutive department allowed for: " + s.department()));
                }
                result.add(s);
                lastDept = s.department();
                lastSubject = s.subjectCode() != null ? s.subjectCode() : "UNKNOWN";
            }

            if (selected != null && !selected.getValue().isEmpty()) {
                heap.add(selected);
            }
        }

        return new InterleavedResult(result, violations);
    }
}
