# Service Level Objectives

## Purpose

This document defines the initial Service Level Objectives (SLOs) for the Event Analysis Platform.

SLOs describe what "reliable enough" means for the service. They give engineering, product, and operations teams a shared way to evaluate whether the system is healthy and whether reliability work should take priority over feature work.

These SLOs are initial local-development targets. They will be refined after ramp testing, stress testing, soak testing, Kubernetes deployment, and production-like monitoring.

## Service Overview

The Event Analysis Platform accepts incident events, processes them asynchronously, stores durable state in PostgreSQL, caches status in Redis, and indexes searchable incident data into Elasticsearch through CDC.

High-level flow:

Client
↓
Spring Boot API
↓
Kafka
↓
Incident Consumer
↓
PostgreSQL
↓
Debezium CDC
↓
Elasticsearch

Supporting systems:

- Redis for incident status, caching, and rate limiting
- Keycloak for authentication and authorization
- Prometheus and Grafana for metrics and dashboards
- Kibana for Elasticsearch inspection

## User Journeys

The initial SLOs focus on three important journeys.

### Incident Creation

A client submits a new incident.

Endpoint: POST /incidents

Expected behavior: 202 Accepted

This endpoint is asynchronous. A successful response means the incident was accepted for processing, not necessarily fully persisted and indexed.

### Incident Search

A client searches incidents through the read model.

Endpoint: GET /incidents/search?q={query}

Expected behavior: 200 OK

Search reads from Elasticsearch.

### Incident Processing

An accepted incident should eventually be processed by the consumer, persisted to PostgreSQL, and indexed into Elasticsearch through CDC.

This journey is asynchronous and should be measured separately from API request latency.

## Initial SLO Targets

### API Availability

Target: 99.9% successful requests over 30 days

Successful request definition:

- 2xx responses for valid requests
- 4xx responses caused by client behavior are not counted as service failures

Examples of client-caused 4xx:

- 400 Bad Request for validation errors
- 401 Unauthorized for missing or invalid tokens
- 403 Forbidden for missing roles
- 404 Not Found for missing incident IDs
- 429 Too Many Requests when rate limiting is working as designed

Service failures include:

- 5xx responses
- unexpected dependency failures surfaced to clients
- timeouts caused by the service

### Incident Creation Latency

Target: 95% of POST /incidents requests complete in less than 500ms

Measured by: p95 HTTP request latency

Reason:

The API is asynchronous and should return quickly after accepting and publishing the event.

### Incident Search Latency

Target: 95% of GET /incidents/search requests complete in less than 400ms

Measured by: p95 HTTP request latency

Reason:

Search is user-facing and should remain responsive under normal read traffic.

### Incident Processing Freshness

Target: 95% of accepted incidents are processed and searchable within 5 seconds

Measured from:

POST /incidents accepted time
↓
incident available in Elasticsearch

Reason:

The system uses Kafka and CDC, so some delay is expected. The goal is not immediate consistency, but bounded eventual consistency.

### Kafka Consumer Lag

Target: Kafka consumer lag should remain below 100 messages for the incident-group consumer during normal traffic

Reason:

Kafka lag indicates whether the consumer is keeping up with produced incident events.

Sustained lag means the API can accept incidents faster than the backend can process them.

### Error Rate

Target: Service-generated 5xx error rate below 1% over rolling 30 minutes

Reason:

A low error rate indicates that the service and its dependencies are operating normally.

## Error Budget

For a 99.9% monthly availability SLO, the monthly error budget is approximately:

0.1% downtime
≈ 43 minutes per 30-day month

The error budget represents the amount of unreliability the service can tolerate while still meeting the SLO.

## Error Budget Policy

If the service is within budget:

- Continue normal feature delivery
- Continue reliability improvements opportunistically
- Review dashboards during planned releases

If the service burns more than 50% of the monthly error budget:

- Review recent incidents and deployments
- Prioritize fixes for top reliability risks
- Increase monitoring of high-risk components

If the service burns more than 100% of the monthly error budget:

- Freeze risky feature work
- Prioritize reliability fixes
- Review alerting and runbooks
- Conduct root cause analysis
- Define prevention actions before resuming normal delivery

## Monitoring Signals

The following signals should be tracked in Prometheus and Grafana.

### API Signals

- Request rate
- p50, p95, and p99 latency
- Error rate
- Response status distribution
- Rate-limited request count

### JVM Signals

- Heap memory usage
- Non-heap memory usage
- GC pause duration
- Thread count
- CPU usage

### Kafka Signals

- Consumer lag
- Messages produced
- Messages consumed
- Consumer error count
- Dead-letter topic count

### PostgreSQL Signals

- Transaction rate
- Active connections
- Slow queries
- Lock waits
- Disk usage

### Redis Signals

- Memory usage
- Command rate
- Key count
- Evictions
- Connection count

### Elasticsearch Signals

- JVM heap usage
- Indexed document count
- Search latency
- Indexing latency
- Cluster health

## Alerting Strategy

Initial alert candidates:

- High API 5xx error rate
- High POST /incidents p95 latency
- High GET /incidents/search p95 latency
- Kafka consumer lag above threshold
- Redis unavailable
- PostgreSQL unavailable
- Elasticsearch unavailable
- JVM heap usage above safe threshold
- Frequent or long GC pauses

Alerts should be actionable. Each alert should point to a runbook explaining how to diagnose and mitigate the issue.

## Known Limitations

These SLOs are initial targets and are not yet based on production traffic.

Current limitations:

- Tests were run in a local Docker Compose environment
- No Kubernetes resource limits have been applied yet
- No multi-instance API deployment has been tested
- Kafka producer and consumer currently run in the same Spring Boot application
- Long-duration soak testing has not been performed
- Failure-mode and chaos testing have not yet been performed
- Incident freshness is not yet measured automatically

## Next Steps

- Add Prometheus alert rules for the initial SLO signals
- Add Grafana panels for SLO visibility
- Add ramping load tests
- Add stress tests to identify failure points
- Add soak tests for memory and latency stability
- Add runbooks for common alerts
- Add automated measurement for incident processing freshness
- Revisit SLO targets after production-like Kubernetes testing