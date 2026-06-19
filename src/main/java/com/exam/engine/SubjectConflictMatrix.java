package com.exam.engine;

import java.util.*;

/**
 * PHASE 4 — SUBJECT CONFLICT OPTIMIZATION
 * 
 * Manages adjacency risks between different departments and subjects.
 * High scores indicate HIGH RISK (should be avoided).
 */
public class SubjectConflictMatrix {

    private final Map<String, Set<String>> conflictMap = new HashMap<>();

    public SubjectConflictMatrix() {
        // Institutional Knowledge: Define known department overlaps
        // Example: ECE and EEE often share basic electronics or math papers.
        addConflict("ECE", "EEE");
        addConflict("CSE", "IT");
        addConflict("CSE", "AIDS");
        addConflict("MECH", "CIVIL");
        addConflict("AIML", "AIDS");
        addConflict("CSBS", "CSE");
    }

    private void addConflict(String deptA, String deptB) {
        conflictMap.computeIfAbsent(deptA, k -> new HashSet<>()).add(deptB);
        conflictMap.computeIfAbsent(deptB, k -> new HashSet<>()).add(deptA);
    }

    /**
     * Calculates the risk score for placing two departments adjacent to each other.
     * 100 = Same Department (Hard Violation of Column Purity if in same column, 
     * but here we check ADJACENT columns).
     */
    public int getConflictScore(String deptA, String deptB) {
        if (deptA == null || deptB == null) return 0;
        if (deptA.equals(deptB)) return 10; // Same dept in adjacent columns is manageable but not ideal
        
        if (conflictMap.getOrDefault(deptA, Collections.emptySet()).contains(deptB)) {
            return 8; // High conflict (Likely overlapping subjects)
        }
        
        return 0; // Low/No conflict
    }
}
