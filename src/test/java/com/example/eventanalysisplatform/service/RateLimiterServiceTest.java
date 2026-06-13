package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class RateLimiterServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp(){
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        rateLimiterService = new RateLimiterService(redisTemplate);
    }

    @Test
    void checkLimitAllowsRequestWithinLimit(){
        when(valueOperations.increment("rate-limit:source:payment"))
                .thenReturn(5L);

        rateLimiterService.checkLimit("payment");

        verify(valueOperations).increment("rate-limit:source:payment");
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void checkLimitSetsTtlForFirstRequest(){
        when(valueOperations.increment("rate-limit:source:payment"))
                .thenReturn(1L);

        rateLimiterService.checkLimit("payment");

        verify(redisTemplate).expire(
                "rate-limit:source:payment",
                Duration.ofMinutes(1)
        );
    }

    @Test
    void checkLimitThrowsWhenLimitExceeded() {
        when(valueOperations.increment("rate-limit:source:payment"))
                .thenReturn(11L);

        assertThatThrownBy(() -> rateLimiterService.checkLimit("payment"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("Rate limit exceeded for source: payment");
    }
}
