package com.exam.repository;

import com.exam.entity.QuestionPaperTracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionPaperTrackerRepository extends JpaRepository<QuestionPaperTracker, UUID> {
    List<QuestionPaperTracker> findByExamSessionId(UUID examSessionId);
    List<QuestionPaperTracker> findByStatus(String status);
    long countByExamSessionId(UUID examSessionId);
    long countByExamSessionIdAndStatus(UUID examSessionId, String status);
}
