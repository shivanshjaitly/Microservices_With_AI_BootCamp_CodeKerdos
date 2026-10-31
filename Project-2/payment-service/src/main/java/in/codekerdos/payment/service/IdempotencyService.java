package in.codekerdos.payment.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed idempotency guard for Kafka consumers. Key pattern
 * "processed:<eventId>" per docs/CONVENTIONS.md — collisions across services
 * are avoided by each service using its own Redis logical database (see
 * spring.data.redis.database in application.yml — payment-service uses
 * database 2), not by namespacing the key itself, so the key stays literally
 * "processed:<eventId>".
 */
@Service
public class IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Returns true the FIRST time this eventId is seen; false on any repeat delivery. */
    public boolean markProcessedIfNew(String eventId) {
        Boolean firstTime = redisTemplate.opsForValue()
                .setIfAbsent("processed:" + eventId, "1", TTL);
        return Boolean.TRUE.equals(firstTime);
    }
}
