package com.exam.engine;

import com.exam.engine.exception.AllocationInvariantException;
import com.exam.engine.model.AllocationViolation;
import com.exam.engine.model.SeatAssignment;
import com.exam.engine.model.Student;

import java.util.List;

public class SeatDistributor {

    private final AllocationConstraintSolver solver = new AllocationConstraintSolver();

    public record DistributorResult(List<SeatAssignment> assignments, List<AllocationViolation> violations) {}

    public DistributorResult distribute(String hallId, List<Student> students, boolean singleDeptMode) {
        int totalCapacity = 25; // 5x5
        if (students.size() > totalCapacity) {
            throw new AllocationInvariantException("Capacity exceeded for hall " + hallId + 
                ". Generator has " + totalCapacity + " positions but given " + students.size() + " students.");
        }

        AllocationConstraintSolver.SolverResult solverResult = solver.allocateHall(hallId, students, null, java.util.Collections.emptyMap(), 0);
        
        return new DistributorResult(solverResult.assignments(), solverResult.violations());
    }
}
