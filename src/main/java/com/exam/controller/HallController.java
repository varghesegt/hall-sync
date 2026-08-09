package com.exam.controller;

import com.exam.entity.Hall;
import com.exam.repository.HallRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/halls")
public class HallController {

    private final HallRepository hallRepository;

    public HallController(HallRepository hallRepository) {
        this.hallRepository = hallRepository;
    }

    @GetMapping
    @Cacheable(value = "halls", key = "T(com.exam.config.tenant.TenantContext).getCurrentTenant() ?: 'MASTER'")
    public ResponseEntity<List<Hall>> getAllHalls() {
        return ResponseEntity.ok(hallRepository.findAll());
    }

    @PostMapping
    @CacheEvict(value = "halls", allEntries = true)
    public ResponseEntity<Hall> createHall(@RequestBody Hall hall) {
        if (hallRepository.existsById(hall.getId())) {
            return ResponseEntity.badRequest().build();
        }
        // Auto-calculate capacity from grid dimensions if dimensions are set
        if (hall.getSemRows() != null && hall.getSemCols() != null) {
            hall.setCapacity(hall.getSemRows() * hall.getSemCols());
        }
        if (hall.getInternalRows() != null && hall.getInternalCols() != null) {
            hall.setInternalCapacity(hall.getInternalRows() * hall.getInternalCols());
        }
        return ResponseEntity.ok(hallRepository.save(hall));
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = "halls", allEntries = true)
    public ResponseEntity<Void> deleteHall(@PathVariable String id) {
        if (!hallRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        hallRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @CacheEvict(value = "halls", allEntries = true)
    public ResponseEntity<Hall> updateHall(@PathVariable String id, @RequestBody Hall updatedHall) {
        return hallRepository.findById(id)
                .map(hall -> {
                    hall.setName(updatedHall.getName());
                    // Update grid dimensions
                    if (updatedHall.getSemRows() != null) {
                        hall.setSemRows(updatedHall.getSemRows());
                    }
                    if (updatedHall.getSemCols() != null) {
                        hall.setSemCols(updatedHall.getSemCols());
                    }
                    if (updatedHall.getInternalRows() != null) {
                        hall.setInternalRows(updatedHall.getInternalRows());
                    }
                    if (updatedHall.getInternalCols() != null) {
                        hall.setInternalCols(updatedHall.getInternalCols());
                    }
                    // Auto-calculate capacity from grid dimensions
                    hall.setCapacity(hall.getSemRows() * hall.getSemCols());
                    hall.setInternalCapacity(hall.getInternalRows() * hall.getInternalCols());
                    return ResponseEntity.ok(hallRepository.save(hall));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

