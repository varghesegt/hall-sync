package com.exam.repository;

import com.exam.entity.AllocationLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface AllocationLockRepository extends JpaRepository<AllocationLock, UUID> {
    
    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value = "INSERT INTO allocation_locks (exam_session_id, locked_by, locked_at) VALUES (:examSessionId, :lockedBy, CURRENT_TIMESTAMP)", nativeQuery = true)
    int attemptLock(@Param("examSessionId") UUID examSessionId, @Param("lockedBy") String lockedBy);
    
    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value = "DELETE FROM allocation_locks WHERE exam_session_id = :examSessionId", nativeQuery = true)
    int releaseLock(@Param("examSessionId") UUID examSessionId);
}
