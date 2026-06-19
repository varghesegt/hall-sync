package com.exam.engine;

import com.exam.engine.model.AllocationViolation;
import com.exam.engine.model.Student;
import com.exam.engine.model.ViolationType;

import java.util.*;

public class HallBalancer {

    public record BalancingResult(
        LinkedHashMap<String, List<Student>> balancedHallAssignments,
        List<AllocationViolation> violations
    ) {}

    public BalancingResult balance(
            LinkedHashMap<String, List<Student>> hallAssignments,
            boolean singleDeptMode,
            Map<String, Set<String>> seasonHistory) {
        
        List<AllocationViolation> violations = new ArrayList<>();
        LinkedHashMap<String, List<Student>> result = new LinkedHashMap<>();
        
        for (Map.Entry<String, List<Student>> entry : hallAssignments.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue())); // Deep copy lists
        }

        if (singleDeptMode || result.size() <= 1) {
            return new BalancingResult(result, violations);
        }

        List<String> orderedHalls = new ArrayList<>(result.keySet());
        String lastHallId = orderedHalls.get(orderedHalls.size() - 1);
        String prevHallId = orderedHalls.get(orderedHalls.size() - 2);

        List<Student> lastHallStudents = result.get(lastHallId);
        List<Student> prevHallStudents = result.get(prevHallId);

        if (lastHallStudents.size() < 10 && !lastHallStudents.isEmpty()) {
            int total = prevHallStudents.size() + lastHallStudents.size();
            int prevTarget = (int) Math.ceil(total / 2.0);
            int lastTarget = (int) Math.floor(total / 2.0);

            int studentsToMove = prevHallStudents.size() - prevTarget;
            
            if (studentsToMove > 0) {
                // Move from the end of prevHall to the start of lastHall
                // Since LAST N students are moved, we pop from the end of prevHall
                List<Student> moving = new ArrayList<>();
                for (int i = 0; i < studentsToMove; i++) {
                    moving.add(prevHallStudents.remove(prevHallStudents.size() - 1));
                }
                
                // Reverse to maintain relative assignment order if they are pushed to the new hall
                Collections.reverse(moving);

                for (Student s : moving) {
                    Set<String> history = seasonHistory.getOrDefault(s.registerNumber(), Collections.emptySet());
                    if (history.contains(lastHallId)) {
                        violations.add(new AllocationViolation(ViolationType.HALL_REUSE_ALLOWED, 
                            "Hall reuse forced during balancing for student " + s.registerNumber() + " in hall " + lastHallId));
                    }
                }

                // Add to last hall (prepending them effectively, or appending?
                // Rule: "shifted last N elements... combine its population". I will append them to the front of last hall to preserve overall sequence, or just add them?
                // The order in the next hall dictates seating. For fully deterministic behavior, let's prepend them so they continue the stream seamlessly, or just addAll at the end? 
                // "combine its population" doesn't strictly order the combination. But adding to the front of the last hall acts like they naturally overflowed.
                List<Student> newLastHall = new ArrayList<>(moving);
                newLastHall.addAll(lastHallStudents);
                result.put(lastHallId, newLastHall);
            }
        }

        return new BalancingResult(result, violations);
    }
}
