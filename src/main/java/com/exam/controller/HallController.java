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
                    hall.setCapacity(updatedHall.getCapacity());
                    if (updatedHall.getInternalCapacity() != null) {
                        hall.setInternalCapacity(updatedHall.getInternalCapacity());
                    }
                    return ResponseEntity.ok(hallRepository.save(hall));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

