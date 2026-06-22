# Load Tests

These k6 tests validate local baseline behavior of the Event Analysis Platform under traffic.

## Requirements

- Spring Boot app running on `localhost:8080`
- Docker Compose dependencies running
- Keycloak token with required roles:
    - `incidents.write` for write tests
    - `incidents.read` for read/search tests

## Run Write Path Test

```bash
ACCESS_TOKEN='paste-token-here' k6 run load-tests/incidents-write.js
```

## Run Ramping Write Path Test

```bash
ACCESS_TOKEN='paste-token-here' k6 run load-tests/incidents-ramp.js
```

## Run Soak Write Path Test

```bash
ACCESS_TOKEN='paste-token-here' k6 run load-tests/incidents-soak.js
```

## Run Rate Limit Test

```bash
ACCESS_TOKEN='paste-token-here' k6 run load-tests/incidents-rate-limit.js
```

## Run Read/Search Test

```bash
ACCESS_TOKEN='paste-token-here' k6 run load-tests/incidents-read-search.js
```

## Notes

These scripts are local benchmark tools. Results depend on laptop resources, Docker limits, token lifetime, and background processes.
