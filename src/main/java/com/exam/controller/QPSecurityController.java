package com.exam.controller;

import com.exam.entity.ExamSession;
import com.exam.entity.QuestionPaperTracker;
import com.exam.repository.QuestionPaperTrackerRepository;
import com.exam.repository.ExamSessionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/security/qp")

@Transactional
public class QPSecurityController {

    private final QuestionPaperTrackerRepository qpRepository;
    private final ExamSessionRepository sessionRepository;

    public QPSecurityController(QuestionPaperTrackerRepository qpRepository, ExamSessionRepository sessionRepository) {
        this.qpRepository = qpRepository;
        this.sessionRepository = sessionRepository;
    }

    @GetMapping("/session/{sessionId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<QuestionPaperTracker>> getTrackersForSession(@PathVariable UUID sessionId) {
        List<QuestionPaperTracker> trackers = qpRepository.findByExamSessionId(sessionId);
        trackers.forEach(t -> org.hibernate.Hibernate.initialize(t.getExamSession()));
        return ResponseEntity.ok(trackers);
    }

    @PostMapping("/receive")
    public ResponseEntity<QuestionPaperTracker> receiveQP(@RequestBody QuestionPaperTracker tracker) {
        tracker.setStatus("RECEIVED");
        ExamSession session = sessionRepository.findById(tracker.getExamSession().getId()).orElseThrow();
        tracker.setExamSession(session);
        return ResponseEntity.ok(qpRepository.save(tracker));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<QuestionPaperTracker> updateStatus(@PathVariable UUID id, @RequestParam String status, @RequestParam(required = false) String remarks) {
        QuestionPaperTracker tracker = qpRepository.findById(id).orElseThrow();
        tracker.setStatus(status);
        if ("DISTRIBUTED".equals(status) && tracker.getDistributedTime() == null) {
            tracker.setDistributedTime(LocalDateTime.now());
        } else if ("RETURNED".equals(status) && tracker.getReturnedTime() == null) {
            tracker.setReturnedTime(LocalDateTime.now());
        }
        if (remarks != null) {
            tracker.setRemarks(remarks);
        }
        ExamSession session = sessionRepository.findById(tracker.getExamSession().getId()).orElseThrow();
        tracker.setExamSession(session);
        return ResponseEntity.ok(qpRepository.save(tracker));
    }
}
