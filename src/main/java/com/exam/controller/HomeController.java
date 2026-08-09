package com.exam.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "HallSync Autonomous Examination & Remuneration API Engine");
        response.put("status", "UP");
        response.put("version", "1.0-SNAPSHOT");
        response.put("uiUrl", "http://localhost:8080/");
        response.put("message", "Please access the HallSync User Interface at http://localhost:8080/");
        return ResponseEntity.ok(response);
    }
}
