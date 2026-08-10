package com.exam.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class HomeController {

    /**
     * Forwards root route "/" directly to React Single Page Application (index.html)
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    /**
     * API Status Endpoint
     */
    @GetMapping("/api/v1/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "HallSync Autonomous Examination & Remuneration Engine");
        response.put("status", "UP");
        response.put("version", "1.0-SNAPSHOT");
        return ResponseEntity.ok(response);
    }
}
