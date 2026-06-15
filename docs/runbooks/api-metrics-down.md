# Runbook: Event Analysis API Metrics Down

## Alert

`EventAnalysisApiMetricsDown`

## Severity

Critical

## Meaning

Prometheus cannot scrape the Spring Boot actuator metrics endpoint.

This means the service may still be running, but Prometheus cannot collect API, JVM, HTTP, and runtime metrics from it.

## Impact

- Loss of observability for the API service
- JVM memory, GC, HTTP latency, and request metrics may be unavailable
- Other alerts depending on application metrics may stop working

## First Checks

Check Prometheus targets:

`http://localhost:9090/targets`

Find:

`event-analysis-platform`

Check:

- State
- Last scrape
- Last error

## Verify App Metrics Endpoint

From the host machine:

`curl http://localhost:8080/actuator/prometheus`

Expected:

`Prometheus-formatted metrics output`

From inside the Prometheus container:

`docker exec -it prometheus wget -qO- http://host.docker.internal:8080/actuator/prometheus`

Expected:

`Prometheus-formatted metrics output`

## Common Causes

### Spring Boot App Is Not Running

Symptoms:

- `connection refused`
- `up{job="event-analysis-platform"} == 0`

Mitigation:

- Start the application from IntelliJ
- Confirm it is listening on port `8080`

### Actuator Prometheus Endpoint Is Not Exposed

Symptoms:

- `404 Not Found` from `/actuator/prometheus`

Check `application.properties`:

`management.endpoints.web.exposure.include=health,prometheus`

Mitigation:

- Expose the Prometheus actuator endpoint
- Restart the application

### Metrics Endpoint Is Blocked By Security

Symptoms:

- `401 Unauthorized`
- `403 Forbidden`

Check `SecurityConfig`.

For local development, `/actuator/prometheus` should be scrapeable by Prometheus:

`.requestMatchers("/actuator/prometheus").permitAll()`

Mitigation:

- Update security config
- Restart the application
- Confirm Prometheus target becomes `UP`

### Prometheus Target Is Wrong

Symptoms:

- Prometheus target points to the wrong host or port
- `connection refused`
- `context deadline exceeded`

Check `monitoring/prometheus.yml`:

`job_name: "event-analysis-platform"`

`metrics_path: "/actuator/prometheus"`

`targets: ["host.docker.internal:8080"]`

Mitigation:

- Fix target host/port
- Restart Prometheus with `docker compose up -d prometheus`

## Prometheus Queries

Check scrape health:

`up{job="event-analysis-platform"}`

Expected healthy value:

`1`

Check if alert condition is active:

`up{job="event-analysis-platform"} == 0`

Expected healthy result:

`empty result`

## Resolution Criteria

The incident is resolved when:

- `/actuator/prometheus` returns metrics from the host
- Prometheus target `event-analysis-platform` is `UP`
- `EventAnalysisApiMetricsDown` alert is `Inactive`

## Follow-Up Actions

- Add dashboard panel for target scrape health
- Confirm `/actuator/prometheus` is intentionally accessible to Prometheus
- In production, protect metrics through private networking, mTLS, or service mesh policy