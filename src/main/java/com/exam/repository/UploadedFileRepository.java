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

    static java.util.Map<UUID, byte[]> FILE_STORAGE = new java.util.concurrent.ConcurrentHashMap<>();
    static java.util.Map<String, ParseResult> CACHE_STORAGE = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Retrieve raw PDF bytes for fallback re-parsing.
     */
    default byte[] getFileBytes(UUID fileId) {
        return FILE_STORAGE.get(fileId);
    }

    /**
     * Store raw PDF bytes for fallback re-parsing.
     */
    default void storeFileBytes(UUID fileId, byte[] bytes) {
        FILE_STORAGE.put(fileId, bytes);
    }

    /**
     * Cache parsed result with config version key.
     */
    default void cacheParseResult(UUID fileId, ParseResult result, String configVersion) {
        CACHE_STORAGE.put(fileId + "_" + configVersion, result);
    }

    /**
     * Retrieve cached parse result.
     */
    default ParseResult getCachedParseResult(UUID fileId, String configVersion) {
        return CACHE_STORAGE.get(fileId + "_" + configVersion);
    }

    /**
     * Fix #2: Evict ALL cache entries for a file (any config version).
     * Must be called when a file is deleted or replaced to prevent stale reads.
     */
    default void evictParseResultCache(UUID fileId) {
        String prefix = fileId.toString() + "_";
        CACHE_STORAGE.keySet().removeIf(key -> key.startsWith(prefix));
    }

    /**
     * Fix #3: Cache TTL Cleanup (Retention Policy).
     * Prevents DB from growing indefinitely across multiple regex/config deployments.
     * Can be wired via @Scheduled cron to clear entries older than TTL (e.g., 7 days).
     *
     * @param retentionDays number of days to keep cached results
     */
    default void cleanupStaleCacheVersions(int retentionDays) {
        // Since we ARE using a ConcurrentHashMap for now instead of a DB table, 
        // and we don't have entry timestamps, we'll just clear the whole cache 
        // to prevent absolute memory leakage over very long durations.
        // In a real DB implementation, this would be a filtered DELETE.
        CACHE_STORAGE.clear();
    }
}
