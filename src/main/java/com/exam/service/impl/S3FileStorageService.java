package com.exam.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
public class S3FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3FileStorageService.class);
    private final S3Client s3Client;

    @Value("${app.aws.s3.bucket-name:hallsync-uploads}")
    private String bucketName;

    public S3FileStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @jakarta.annotation.PostConstruct
    public void initBucket() {
        try {
            s3Client.createBucket(b -> b.bucket(bucketName));
            logger.info("Successfully verified/initialized S3/MinIO bucket '{}'", bucketName);
        } catch (Exception e) {
            logger.info("S3 bucket '{}' readiness notice: {}", bucketName, e.getMessage());
        }
    }

    /**
     * Uploads a raw byte array to local storage and S3.
     */
    public void storeFileBytes(UUID fileId, byte[] bytes, String contentType) {
        String key = "uploads/" + fileId.toString() + ".pdf";
        
        // 1. Always store to local persistent storage FIRST (instant 1ms I/O)
        try {
            java.nio.file.Path localDir = java.nio.file.Paths.get("uploads");
            if (!java.nio.file.Files.exists(localDir)) {
                java.nio.file.Files.createDirectories(localDir);
            }
            java.nio.file.Path localFile = localDir.resolve(fileId.toString() + ".pdf");
            java.nio.file.Files.write(localFile, bytes);
            logger.info("Successfully saved file {} to local storage", localFile);
        } catch (Exception ex) {
            logger.warn("Could not write file to local uploads directory: {}", ex.getMessage());
        }

        // 2. Best-effort upload to S3 / MinIO
        try {
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putOb, RequestBody.fromBytes(bytes));
            logger.info("Successfully uploaded file {} to S3 bucket {}", key, bucketName);
        } catch (Exception e) {
            logger.warn("S3/MinIO upload notice for {}: {} (local copy preserved)", key, e.getMessage());
        }
    }

    /**
     * Retrieves a raw byte array from local storage or S3. Returns null if not found.
     */
    public byte[] getFileBytes(UUID fileId) {
        String key = "uploads/" + fileId.toString() + ".pdf";

        // 1. Check local persistent storage FIRST (1ms response time)
        try {
            java.nio.file.Path localPath = java.nio.file.Paths.get("uploads", fileId.toString() + ".pdf");
            if (java.nio.file.Files.exists(localPath)) {
                return java.nio.file.Files.readAllBytes(localPath);
            }
            // Fallback check temp dir
            java.nio.file.Path tmpPath = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "hallsync_uploads", fileId.toString() + ".pdf");
            if (java.nio.file.Files.exists(tmpPath)) {
                return java.nio.file.Files.readAllBytes(tmpPath);
            }
        } catch (Exception ex) {
            logger.warn("Error reading local storage for file {}: {}", fileId, ex.getMessage());
        }

        // 2. Fallback to S3 / MinIO
        try {
            GetObjectRequest getOb = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getOb);
            return objectBytes.asByteArray();
        } catch (Exception e) {
            logger.warn("File {} not found in S3/MinIO: {}", key, e.getMessage());
            return null;
        }
    }
}
