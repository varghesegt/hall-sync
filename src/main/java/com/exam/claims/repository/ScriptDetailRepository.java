package com.exam.claims.repository;

import com.exam.claims.entity.ScriptDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScriptDetailRepository extends JpaRepository<ScriptDetail, Long> {

    List<ScriptDetail> findByClaimRecordIdOrderBySessionTypeAscSerialNumberAsc(Long claimRecordId);
}
