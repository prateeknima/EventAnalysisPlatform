# Load Test Results

## Baseline Environment

- Date: 2026-06-15
- Runtime: Local Mac development environment
- Application: Spring Boot API running on `localhost:8080`
- Dependencies: Docker Compose
    - PostgreSQL
    - Redis
    - Kafka
    - Debezium / Kafka Connect
    - Elasticsearch
    - Keycloak
    - Prometheus
    - Grafana
    - Kibana
- Tool: k6
- Test type: Local baseline load test

## Scope

These tests validate the baseline behavior of the Event Analysis Platform under low, steady local load.

The tests cover:

- Authenticated incident creation
- Redis-backed rate limiting
- Elasticsearch-backed incident search
- Basic latency and error-rate thresholds

These tests do not represent production capacity. They provide an initial baseline for future ramp, stress, soak, and chaos testing.

## Tests Executed

### Write Path Test

Script: `load-tests/incidents-write.js`

Command: `ACCESS_TOKEN='...' k6 run load-tests/incidents-write.js`

Purpose:

Validate that authenticated clients can submit incidents successfully under steady concurrent traffic.

Path tested: `POST /incidents`

Expected behavior: `202 Accepted`

Result:

- Virtual users: 5
- Duration: 30 seconds
- Total requests: 150
- Failure rate: 0%
- p95 latency: 33.55ms
- Threshold: p95 latency < 500ms
- Outcome: PASS

Interpretation:

The API accepted low steady write traffic successfully. At this load level, the write path stayed well below the initial latency target.

### Ramping Write Path Test

Script: `load-tests/incidents-ramp.js`

Command: `ACCESS_TOKEN='...' k6 run load-tests/incidents-ramp.js`

Purpose:

Validate that authenticated incident creation remains stable while traffic gradually increases.

Path tested: `POST /incidents`

Expected behavior: `202 Accepted`

Result:

- Maximum virtual users: 50
- Duration: 5 minutes
- Total requests: 5288
- Failure rate: 0%
- Check success rate: 100%
- p95 latency: 17.24ms
- Max latency: 126.79ms
- Threshold: p95 latency < 750ms
- Outcome: PASS

Interpretation:

The service handled a local ramp from 5 to 50 virtual users without failed requests. API latency remained well below the ramp-test threshold. Earlier failures were caused by Keycloak access token expiry during the test, so the local token lifespan was increased for load testing.

### Soak Write Path Test

Script: `load-tests/incidents-soak.js`

Command: `ACCESS_TOKEN='...' k6 run load-tests/incidents-soak.js`

Purpose:

Validate that authenticated incident creation remains stable during sustained local traffic.

Path tested: `POST /incidents`

Expected behavior: `202 Accepted`

Result:

- Virtual users: 10
- Duration: 10 minutes
- Total requests: 5890
- Failure rate: 0%
- Check success rate: 100%
- p95 latency: 25.92ms
- Max latency: 104.82ms
- Threshold: p95 latency < 750ms
- Outcome: PASS

Interpretation:

The service handled sustained local write traffic for 10 minutes without failed requests. API latency remained stable and well below the soak-test threshold.

### Rate Limit Test

Script: `load-tests/incidents-rate-limit.js`

Command: `ACCESS_TOKEN='...' k6 run load-tests/incidents-rate-limit.js`

Purpose:

Validate that Redis-backed rate limiting protects the API from repeated traffic from the same source.

Path tested: `POST /incidents`

Expected behavior: `202 Accepted` before the limit, then `429 Too Many Requests` after the limit.

Result:

- Virtual users: 1
- Iterations: 20
- Total requests: 20
- Accepted responses: 10
- Rate-limited responses: 10
- Check success rate: 100%
- p95 latency: 11.87ms
- Threshold: checks == 100%
- Outcome: PASS

Interpretation:

The rate limiter behaved as expected. Requests from the same source were accepted until the configured limit was reached, then rejected with `429 Too Many Requests`.

`http_req_failed` is expected to show failures for this test because k6 treats `429` responses as failed HTTP requests. The actual pass condition is the custom check that allows either `202` or `429`.

### Read/Search Test

Script: `load-tests/incidents-read-search.js`

Command: `ACCESS_TOKEN='...' k6 run load-tests/incidents-read-search.js`

Purpose:

Validate that authenticated users can search incidents through the Elasticsearch-backed read model under steady concurrent traffic.

Path tested: `GET /incidents/search?q=payment`

Expected behavior: `200 OK`

Result:

- Virtual users: 5
- Duration: 30 seconds
- Total requests: 150
- Failure rate: 0%
- p95 latency: 19.97ms
- Threshold: p95 latency < 400ms
- Outcome: PASS

Interpretation:

The search endpoint handled low steady read traffic successfully. At this load level, Elasticsearch-backed search stayed below the initial latency target.

## Observability Notes

During these tests, metrics were available through Prometheus and Grafana.

Metrics to observe during future tests:

- HTTP request rate and latency
- JVM heap and non-heap memory
- GC behavior
- Redis memory and command activity
- PostgreSQL transaction rate and connection usage
- Kafka consumer lag
- Elasticsearch JVM memory and document/index activity

Kafka consumer lag did not noticeably increase during the baseline write test. This likely indicates that the consumer was able to keep up with the current local load level.

## Initial SLO Targets

These initial targets are used as local baseline goals:

- `POST /incidents` p95 latency < 500ms
- `GET /incidents/search` p95 latency < 400ms
- Write-path error rate < 5%
- Read-path error rate < 5%
- Rate-limit behavior returns expected `202` or `429` responses

These are not final production SLOs. They are starting targets that will be refined after ramp, stress, soak, and failure-mode testing.

## Limitations

This test was executed on a local development machine, so results are affected by local CPU, memory, Docker resource limits, and background processes.

The tests used low traffic levels and short durations. They do not yet validate:

- Maximum throughput
- Long-running memory stability
- Recovery after dependency failures
- Kafka backlog recovery
- Database saturation behavior
- Elasticsearch indexing delay under sustained write load
- Multi-instance application behavior
- Kubernetes resource limits or autoscaling behavior

## Next Steps

- Add stress test to identify failure points
- Capture Grafana screenshots during load tests
- Define formal SLOs and error-budget policy
- Add Prometheus alert rules for latency, error rate, Kafka lag, JVM memory, and dependency health
- Add runbooks for common failure scenarios
- Add longer soak test in a production-like Kubernetes environment
