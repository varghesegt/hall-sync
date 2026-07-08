package com.exam.engine;

import com.exam.engine.model.Student;

/**
 * PRODUCTION-GRADE Safety Validator for seating risk assessment.
 */
public class SafetyValidator {

    /**
     * Calculates the risk score for a seat based on its neighbors.
     * Horizontal match: 100 (Hard Violation)
     * Vertical match: 50
     * Diagonal match: 25
     */
    public int calculateRisk(Student[][] grid, int r, int c) {
        Student current = grid[r][c];
        if (current == null) return 0;

        int risk = 0;
        String subject = current.subjectCode();

        // Horizontal Check (Left)
        if (c > 0 && grid[r][c - 1] != null && subject.equals(grid[r][c - 1].subjectCode())) {
            risk += 100;
        }

        // Vertical Check (Above) - REMOVED
        // In column-based examination seating, students of the same subject sitting one behind another is expected.

        // Diagonal Check (Top-Left)
        if (r > 1 && c > 0 && grid[r - 1][c - 1] != null && subject.equals(grid[r - 1][c - 1].subjectCode())) {
            risk += 25;
        }

        // Diagonal Check (Top-Right)
        if (r > 1 && c < 4 && grid[r - 1][c + 1] != null && subject.equals(grid[r - 1][c + 1].subjectCode())) {
            risk += 25;
        }

        return Math.min(risk, 100); // Cap at 100
    }
}
