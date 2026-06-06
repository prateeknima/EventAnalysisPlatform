package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.record.IncidentStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

@Service
public class RedisService {
    private final RedisTemplate<String, String> redisTemplate;

    public RedisService(
            RedisTemplate<String, String> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String key, String value){
        redisTemplate.opsForValue().set(key, value);
    }

    public String get(String key){
        return redisTemplate.opsForValue().get(key);
    }

    public void saveStatus(String incidentId, IncidentStatus status) {
        save("incident:" + incidentId + ":status", status.name());
    }

    public String getStatus(String incidentId) {
        return get("incident:" + incidentId + ":status");
    }
}
