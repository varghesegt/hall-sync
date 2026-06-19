package com.exam.engine;

import java.util.*;

/**
 * PHASE 5 — COLUMN RISK SCORING
 * 
 * Mathematically evaluates a proposed column arrangement for a hall.
 */
public class HallScoringSystem {

    private final SubjectConflictMatrix conflictMatrix = new SubjectConflictMatrix();

    public record LayoutScore(String[] pattern, int score) {}

    /**
     * Scores a 5-column pattern. 
     * LOWER SCORE is BETTER (Minimum Risk).
     */
    public int calculateRisk(String[] pattern) {
        int risk = 0;

        for (int i = 0; i < pattern.length - 1; i++) {
            String current = pattern[i];
            String next = pattern[i + 1];

            // 1. Conflict Matrix Score (Adjacent Departments)
            risk += conflictMatrix.getConflictScore(current, next) * 5;

            // 2. Repeated Department Penalty (e.g., A A B C D)
            // Note: Horizontal safety is already handled, but grouping is preferred.
            if (current.equals(next)) {
                risk += 4; 
            }
        }

        // 3. Symmetry Check (Institutional Preference for ABABA or ABCAB)
        if (pattern[0].equals(pattern[2]) && pattern[2].equals(pattern[4])) {
            risk -= 10; // Bonus for standard alternating layout
        }

        return risk;
    }

    public String[] findBestPattern(List<String> availableDepts) {
        // Simple permutation search for small dept counts, or just heuristic
        // For now, we prioritize the ABABA or ABCAB heuristics
        if (availableDepts.size() >= 2) {
            String a = availableDepts.get(0);
            String b = availableDepts.get(1);
            if (availableDepts.size() >= 3) {
                String c = availableDepts.get(2);
                return new String[]{a, b, c, a, b};
            }
            return new String[]{a, b, a, b, a};
        }
        return new String[]{availableDepts.get(0), availableDepts.get(0), availableDepts.get(0), availableDepts.get(0), availableDepts.get(0)};
    }
}
