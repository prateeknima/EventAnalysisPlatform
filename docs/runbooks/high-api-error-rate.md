# Runbook: High API Error Rate

## Alert

`EventAnalysisApiHighErrorRate`

## Meaning

The API is returning too many `5xx` responses.

`5xx` means the server failed while handling a request.

This is different from `4xx`, where the client usually sent a bad request, invalid token, or request that was not allowed.

## Why It Matters

High `5xx` errors mean users may not be able to create, read, or search incidents.

It can also burn the service error budget.

## First Checks

Check Prometheus:

`http://localhost:9090/alerts`

Check API error rate:

`sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))`

Check which status codes are happening:

`sum by (status) (rate(http_server_requests_seconds_count[5m]))`

Check Spring Boot logs in IntelliJ.

Look for:

- stack traces
- repeated exceptions
- dependency connection errors

## Common Causes

### Dependency Is Down

The API may fail if one of these is down:

- PostgreSQL
- Redis
- Kafka
- Elasticsearch

Check Prometheus targets:

`http://localhost:9090/targets`

Fix:

- Start the failing service
- Restart the app if needed
- Confirm the target becomes `UP`

### Application Bug

A controller, service, repository, or consumer may be throwing an exception.

Fix:

- Find the stack trace
- Identify the failing class
- Reproduce the request
- Fix the code
- Add a test for the failure

### Bad Configuration

The app may have wrong config.

Examples:

- wrong database URL
- wrong Kafka bootstrap server
- wrong Redis host
- wrong Keycloak issuer

Fix:

- Check `application.properties`
- Check `docker-compose.yml`
- Restart the app after changes

### Too Much Traffic

The app may be overloaded.

Check:

- JVM heap
- Kafka lag
- Postgres connections
- Redis command activity

Fix:

- Reduce traffic
- Increase capacity
- Tune the bottleneck
- Add rate limiting if needed

## Resolution Criteria

The issue is resolved when:

- API `5xx` error rate drops below the alert threshold
- Valid requests succeed again
- Logs stop showing repeated errors
- Required dependencies are healthy

## Follow-Up

After fixing:

- Write down the root cause
- Add a test if this was a code bug
- Update this runbook if the fix was not obvious