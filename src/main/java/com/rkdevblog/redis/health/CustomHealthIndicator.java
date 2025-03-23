package com.rkdevblog.redis.health;

import com.rkdevblog.redis.repository.CacheRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CustomHealthIndicator implements HealthIndicator {

    private final CacheRepository cacheRepository;

    @Autowired
    public CustomHealthIndicator(CacheRepository cacheRepository) {
        this.cacheRepository = cacheRepository;
    }

    @Override
    public Health health() {
        try {
            // Perform a simple check using cacheRepository.
            // For example, if your CacheRepository has a ping method, you can call it.
            // Otherwise, you may perform a simple get/put or similar check.
            String ping = cacheRepository.ping(); // implement ping() in your repository

            if ("PONG".equalsIgnoreCase(ping)) {
                return Health.up().withDetail("Cache", "Available").build();
            } else {
                return Health.down().withDetail("Cache", "Unexpected response: " + ping).build();
            }
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}