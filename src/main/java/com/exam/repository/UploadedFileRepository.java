package com.exam.repository;

import com.exam.entity.UploadedFile;
import com.exam.parser.model.ParseResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {

    boolean existsBySha256Hash(String sha256Hash);

    Optional<UploadedFile> findBySha256Hash(String sha256Hash);



}
