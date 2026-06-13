package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.exception.RateLimitExceededException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimiterService {

    private static final int LIMIT = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(StringRedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    public void checkLimit(String source){
        String key = "rate-limit:source:"+source;
        Long count = redisTemplate.opsForValue().increment(key);

        if(count != null && count == 1){
            redisTemplate.expire(key, WINDOW);
        }

        if (count != null && count > LIMIT){
            throw new RateLimitExceededException(source);
        }
    }
}
