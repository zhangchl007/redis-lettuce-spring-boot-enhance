package com.rkdevblog.redis.repository;

import com.rkdevblog.redis.exception.OTPServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link CacheRepository}
 */
@Repository
public class OTPCacheRepository implements CacheRepository {

    private final long ttl;
    private final StringRedisTemplate redisTemplate;
    private final ValueOperations<String, String> valueOps;

    public OTPCacheRepository(StringRedisTemplate redisTemplate,
                              @Value("${spring.redis.timeToLive}") long ttl) {
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
        this.ttl = ttl;
    }

    @Override
    public void put(String key, Integer value) {
        try {
            valueOps.set(key, String.valueOf(value));
            redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            throw new OTPServiceException("Error while saving to cache ", e);
        }
    }

    @Override
    public Optional<String> get(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            if (Boolean.TRUE.equals(exists)) {
                return Optional.ofNullable(valueOps.get(key));
            }
            return Optional.empty();
        } catch (RuntimeException e) {
            throw new OTPServiceException("Error while retrieving from the cache ", e);
        }
    }

    @Override
    public void remove(String key) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException e) {
            throw new OTPServiceException("Error while removing from the cache ", e);
        }
    }

    /**
     * Sonar issue (always true/false condition) deep dive:
     *
     * Previous code:
     *   var factory = redisTemplate.getConnectionFactory();
     *   if (factory == null) { ... }
     *   var connection = factory.getConnection();
     *   if (connection != null) return connection.ping();
     *   throw ...
     *
     * In normal Spring Data Redis usage redisTemplate is constructed with a non-null
     * connection factory. getConnectionFactory() will not return null after bean
     * initialization. Also LettuceConnectionFactory#getConnection() never returns null.
     * Sonar flags these null checks as always false/true (dead code) -> code smell.
     *
     * Resolution:
     * - Remove redundant null checks.
     * - Use the template callback API which handles resource acquisition/release.
     * - Wrap any RuntimeException in OTPServiceException.
     */
    @Override
    public String ping() {
        try {
            // Explicit cast to RedisCallback<String> removes compile-time ambiguity.
            String result = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<String>)
                    connection -> connection.ping());

            if (result == null) {
                throw new OTPServiceException("Received null ping response from Redis");
            }
            return result;
        } catch (RuntimeException e) {
            throw new OTPServiceException("Error while pinging the cache ", e);
        }
    }
}
