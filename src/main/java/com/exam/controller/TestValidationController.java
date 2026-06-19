package com.exam.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test-validation")
public class TestValidationController {

    private final JdbcTemplate jdbcTemplate;

    public TestValidationController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> validate() {
        Map<String, Object> results = new HashMap<>();

        // 1. Check allocations exist
        Integer allocCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM allocations", Integer.class);
        results.put("1_AllocationsCount", allocCount);

        // 2. Check batch status
        List<Map<String, Object>> batches = jdbcTemplate.queryForList("SELECT id, status FROM allocation_batches");
        results.put("2_AllocationBatches", batches);

        // 3. Check audit logs
        List<Map<String, Object>> auditLogs = jdbcTemplate.queryForList("SELECT event_type, message FROM audit_logs");
        results.put("3_AuditLogs", auditLogs);

        return results;
    }
}
