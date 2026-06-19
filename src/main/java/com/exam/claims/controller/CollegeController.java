package com.exam.claims.controller;

import com.exam.claims.entity.CollegeDistance;
import com.exam.claims.service.CollegeDistanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/colleges")
public class CollegeController {

    @Autowired
    private CollegeDistanceService collegeDistanceService;

    @GetMapping
    public ResponseEntity<List<CollegeDistance>> getAllColleges() {
        return ResponseEntity.ok(collegeDistanceService.getAllColleges());
    }

    @GetMapping("/search")
    public ResponseEntity<List<CollegeDistance>> searchColleges(@RequestParam String name) {
        return ResponseEntity.ok(collegeDistanceService.searchByName(name));
    }

    @PostMapping
    public ResponseEntity<CollegeDistance> addCollege(@Valid @RequestBody CollegeDistance college) {
        return ResponseEntity.ok(collegeDistanceService.save(college));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CollegeDistance> updateCollege(@PathVariable Long id, @Valid @RequestBody CollegeDistance college) {
        college.setId(id);
        return ResponseEntity.ok(collegeDistanceService.save(college));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollege(@PathVariable Long id) {
        collegeDistanceService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
