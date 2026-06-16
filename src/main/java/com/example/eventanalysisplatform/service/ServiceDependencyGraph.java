package com.example.eventanalysisplatform.service;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Service
public class ServiceDependencyGraph {

    private final Map<String, List<String>> dependencies = Map.ofEntries(
            Map.entry("payment", List.of("fraud", "checkout", "notification")),
            Map.entry("fraud", List.of("risk-engine", "notification")),
            Map.entry("checkout", List.of("inventory", "shipping", "notification")),
            Map.entry("notification", List.of("email", "sms")),
            Map.entry("inventory", List.of("warehouse")),
            Map.entry("shipping", List.of("carrier")),
            Map.entry("risk-engine", List.of()),
            Map.entry("email", List.of()),
            Map.entry("sms", List.of()),
            Map.entry("warehouse", List.of()),
            Map.entry("carrier", List.of())
    );

    public List<String> findAffectedServices(String source) {
        if (source == null || source.isBlank()) {
            return List.of();
        }

        String normalizedSource = source.toLowerCase();

        if (!dependencies.containsKey(normalizedSource)) {
            return List.of(normalizedSource);
        }

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        List<String> affectedServices = new ArrayList<>();

        queue.add(normalizedSource);
        visited.add(normalizedSource);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            affectedServices.add(current);

            for (String dependency : dependencies.getOrDefault(current, List.of())) {
                if (visited.add(dependency)) {
                    queue.add(dependency);
                }
            }
        }

        return affectedServices;
    }
}