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

    /**
     * Uploads a raw byte array to S3.
     */
    public void storeFileBytes(UUID fileId, byte[] bytes, String contentType) {
        String key = "uploads/" + fileId.toString() + ".pdf"; // Hardcoding .pdf since it's the main type for now
        
        try {
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putOb, RequestBody.fromBytes(bytes));
            logger.info("Successfully uploaded file {} to S3 bucket {}", key, bucketName);
        } catch (Exception e) {
            logger.warn("Failed to upload file {} to S3. Attempting local fallback...", key);
            try {
                java.nio.file.Path localDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "hallsync_uploads");
                java.nio.file.Files.createDirectories(localDir);
                java.nio.file.Path localFile = localDir.resolve(fileId.toString() + ".pdf");
                java.nio.file.Files.write(localFile, bytes);
                logger.info("Successfully uploaded file {} to local fallback", localFile);
            } catch (Exception ex) {
                logger.error("Local fallback also failed for storeFileBytes", ex);
                throw new RuntimeException("Cloud storage and local fallback upload failed", ex);
            }
        }
    }

    /**
     * Retrieves a raw byte array from S3. Returns null if not found.
     */
    public byte[] getFileBytes(UUID fileId) {
        String key = "uploads/" + fileId.toString() + ".pdf";

        try {
            GetObjectRequest getOb = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getOb);
            return objectBytes.asByteArray();
        } catch (Exception e) {
            logger.warn("Failed to download file {} from S3. Attempting local fallback...", key);
            try {
                java.nio.file.Path localPath = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "hallsync_uploads", fileId.toString() + ".pdf");
                if (java.nio.file.Files.exists(localPath)) {
                    return java.nio.file.Files.readAllBytes(localPath);
                }
            } catch (Exception ex) {
                logger.error("Local fallback also failed for getFileBytes", ex);
            }
            return null;
        }
    }
}
