# Event Analysis Platform System Architecture and Data Flow

This document shows the current high-level system design for the Event Analysis Platform.

```mermaid
flowchart TB
    Client["Client / Postman / k6 Load Test"] --> API["Spring Boot API Service"]

    API --> Auth["Keycloak / OIDC JWT Validation"]
    API --> RateLimit["Redis Rate Limiter"]
    API --> KafkaProducer["Kafka Producer"]
    API --> ReadAPI["Read/Search Endpoints"]

    KafkaProducer --> Kafka["Kafka Topic: incidents"]

    Kafka --> Consumer["Incident Event Consumer"]
    Consumer --> Postgres["PostgreSQL Source of Truth"]
    Consumer --> Redis["Redis Incident Status Cache"]
    Consumer --> DLT["Kafka DLT: incidents-dlt"]

    Postgres --> Debezium["Debezium CDC Connector"]
    Debezium --> CdcTopic["Kafka CDC Topic"]
    CdcTopic --> SearchIndexer["CDC Search Indexer"]
    SearchIndexer --> Enrichment["Incident Enrichment Service"]
    Enrichment --> SearchDoc["Enriched Search Document"]
    SearchDoc --> Elasticsearch["Elasticsearch Incident Index"]

    ReadAPI --> Postgres
    ReadAPI --> Redis
    ReadAPI --> Elasticsearch

    Elasticsearch --> RankedSearch["Ranked Search"]
    RankedSearch --> RankScore["IncidentRankScore"]

    API --> Actuator["Spring Actuator / Prometheus Endpoint"]
    Actuator --> Prometheus["Prometheus"]
    Prometheus --> Grafana["Grafana Dashboards"]
    Prometheus --> Alertmanager["Alertmanager"]
    Alertmanager --> Slack["Slack Alerts"]

    Kibana["Kibana"] --> Elasticsearch

    Docker["Docker Compose: local infrastructure"]
    Docker --> Kafka
    Docker --> Redis
    Docker --> Postgres
    Docker --> Elasticsearch
    Docker --> Debezium

    K8s["Kubernetes Manifests"] --> ApiDeployment["API Deployment / Service / ConfigMap / Secret"]

    GHActions["GitHub Actions CI"] --> Tests["Maven Tests + JaCoCo"]
    GHActions --> DockerBuild["Docker Image Build"]
    DockerBuild --> GHCR["GitHub Container Registry"]

    subgraph Perf["Performance Experiments"]
        ManualBench["Manual Valhalla Benchmark"]
        JMH["JMH Benchmark"]
        ValhallaJDK["Valhalla EA JDK"]
        ManualBench --> ValhallaJDK
        JMH --> ValhallaJDK
        RankScore --> ManualBench
        RankScore --> JMH
    end
```

## Summary

Requests enter through the Spring Boot API. Authentication and authorization are handled with Keycloak/OIDC JWT validation.

Incident writes are published to Kafka. Kafka consumers persist incidents into PostgreSQL and update Redis with incident processing status.

PostgreSQL changes are captured by Debezium CDC and published back into Kafka. A CDC search indexer consumes those events, enriches the incident data, and writes searchable documents into Elasticsearch.

Read APIs query PostgreSQL, Redis, or Elasticsearch depending on the endpoint. Ranked search uses `IncidentRankScore` to combine operational priority with Elasticsearch relevance.

Prometheus, Grafana, Alertmanager, and Slack provide observability and alerting. Kibana is used to inspect Elasticsearch data.

Docker Compose supports local infrastructure. Kubernetes manifests support API deployment structure. GitHub Actions runs tests, builds the Docker image, and publishes it to GitHub Container Registry.

Project Valhalla and JMH are isolated experiments. They are not in the production request path. They test whether compact ranked-search score objects could benefit from future JVM value-object optimizations.