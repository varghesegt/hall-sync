package com.exam.controller;

import com.exam.entity.Faculty;
import com.exam.service.FacultyService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@RequestMapping("/api/v1/faculty")
public class FacultyController {

    private final FacultyService facultyService;

    public FacultyController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllFaculty() {
        List<Faculty> faculty = facultyService.getAllActiveFaculty();
        List<Map<String, Object>> result = faculty.stream().map(this::toDto).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable UUID id) {
        return facultyService.getById(id)
                .map(f -> ResponseEntity.ok(toDto(f)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Faculty faculty = facultyService.create(
                (String) body.get("name"),
                (String) body.get("employeeId"),
                (String) body.get("department"),
                (String) body.getOrDefault("designation", null),
                (String) body.getOrDefault("phone", null),
                (String) body.getOrDefault("email", null),
                (String) body.getOrDefault("collegeName", null),
                body.containsKey("isInternal") ? (Boolean) body.get("isInternal") : null,
                body.containsKey("isAvailable") ? (Boolean) body.get("isAvailable") : null);
        return ResponseEntity.ok(toDto(faculty));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        Faculty faculty = facultyService.update(id,
                (String) body.get("name"),
                (String) body.get("employeeId"),
                (String) body.get("department"),
                (String) body.getOrDefault("designation", null),
                (String) body.getOrDefault("phone", null),
                (String) body.getOrDefault("email", null),
                (String) body.getOrDefault("collegeName", null),
                body.containsKey("isInternal") ? (Boolean) body.get("isInternal") : null,
                body.containsKey("isAvailable") ? (Boolean) body.get("isAvailable") : null);
        return ResponseEntity.ok(toDto(faculty));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        facultyService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> bulkUpload(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = facultyService.bulkImportFromExcel(file);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=faculty_template.xlsx");
        facultyService.generateTemplate(response.getOutputStream());
    }

    private Map<String, Object> toDto(Faculty f) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", f.getId());
        dto.put("name", f.getName());
        dto.put("employeeId", f.getEmployeeId());
        dto.put("department", f.getDepartment());
        dto.put("designation", f.getDesignation());
        dto.put("phone", f.getPhone());
        dto.put("email", f.getEmail());
        dto.put("isActive", f.getIsActive());
        dto.put("collegeName", f.getCollegeName());
        dto.put("isInternal", f.getIsInternal());
        dto.put("isAvailable", f.getIsAvailable());
        return dto;
    }
}
