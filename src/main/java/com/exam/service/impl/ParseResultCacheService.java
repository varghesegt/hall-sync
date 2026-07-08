package com.exam.service.impl;

import com.exam.parser.model.ParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class ParseResultCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ParseResultCacheService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Cache TTL for parsed results to avoid indefinite memory/Redis growth
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    public ParseResultCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildKey(UUID fileId, String configVersion) {
        return "parse_result:" + fileId.toString() + ":" + configVersion;
    }

    public void cacheParseResult(UUID fileId, ParseResult result, String configVersion) {
        String key = buildKey(fileId, configVersion);
        try {
            redisTemplate.opsForValue().set(key, result, CACHE_TTL);
            logger.debug("Cached parse result in Redis for key: {}", key);
        } catch (Exception e) {
            logger.error("Failed to cache parse result in Redis for key: {}", key, e);
        }
    }

    public ParseResult getCachedParseResult(UUID fileId, String configVersion) {
        String key = buildKey(fileId, configVersion);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof ParseResult) {
                return (ParseResult) cached;
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve parse result from Redis for key: {}", key, e);
        }
        return null;
    }

    public void evictParseResultCache(UUID fileId, String configVersion) {
        String key = buildKey(fileId, configVersion);
        try {
            redisTemplate.delete(key);
            logger.debug("Evicted parse result from Redis for key: {}", key);
        } catch (Exception e) {
            logger.error("Failed to evict parse result from Redis for key: {}", key, e);
        }
    }
}
