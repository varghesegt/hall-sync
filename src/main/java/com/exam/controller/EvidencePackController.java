package com.exam.controller;

import com.exam.service.EvidencePackService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit/evidence")

public class EvidencePackController {

    private final EvidencePackService evidencePackService;

    public EvidencePackController(EvidencePackService evidencePackService) {
        this.evidencePackService = evidencePackService;
    }

    @GetMapping("/naac/{batchId}")
    public ResponseEntity<byte[]> downloadNaacPack(@PathVariable UUID batchId) {
        try {
            byte[] zipData = evidencePackService.generateNaacEvidencePack(batchId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/zip"));
            headers.setContentDispositionFormData("attachment", "NAAC_Evidence_Pack_" + batchId + ".zip");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
