package com.exam.repository;

import com.exam.entity.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM ExamSession s WHERE s.id = :id")
    Optional<ExamSession> findByIdWithLock(UUID id);

    java.util.List<ExamSession> findByExamDate(java.time.LocalDate examDate);
}
