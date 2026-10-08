package com.exam.service.impl;

import com.exam.parser.model.ParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ParseResultCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ParseResultCacheService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, ParseResult> localCache = new ConcurrentHashMap<>();
    
    // Cache TTL for parsed results to avoid indefinite memory/Redis growth
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    public ParseResultCacheService(@Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate != null) {
            logger.info("ParseResultCacheService initialized with Redis caching.");
        } else {
            logger.info("ParseResultCacheService initialized with in-memory caching.");
        }
    }

    private String buildKey(UUID fileId, String configVersion) {
        return "parse_result:" + fileId.toString() + ":" + configVersion;
    }

    public void cacheParseResult(UUID fileId, ParseResult result, String configVersion) {
        String key = buildKey(fileId, configVersion);
        localCache.put(key, result);
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, result, CACHE_TTL);
                logger.debug("Cached parse result in Redis for key: {}", key);
            } catch (Exception e) {
                logger.debug("Redis caching skipped, using local cache: {}", e.getMessage());
            }
        }
    }

    public ParseResult getCachedParseResult(UUID fileId, String configVersion) {
        String key = buildKey(fileId, configVersion);
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(key);
                if (cached instanceof ParseResult) {
                    return (ParseResult) cached;
                }
            } catch (Exception e) {
                logger.debug("Redis get skipped, using local cache: {}", e.getMessage());
            }
        }
        return localCache.get(key);
    }

    public void evictParseResultCache(UUID fileId, String configVersion) {
        String key = buildKey(fileId, configVersion);
        localCache.remove(key);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
                logger.debug("Evicted parse result from Redis for key: {}", key);
            } catch (Exception e) {
                logger.debug("Redis evict skipped: {}", e.getMessage());
            }
        }
    }
}
