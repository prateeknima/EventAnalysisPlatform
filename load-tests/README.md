# Load Tests

These k6 tests validate the production behavior of the Event Analysis Platform under traffic.

## Requirements

- Spring Boot app running on `localhost:8080`
- Docker Compose dependencies running
- Keycloak token with required roles:
    - `incidents.write` for write tests
    - `incidents.read` for read/search tests

## Run Write Path Test

```bash
ACCESS_TOKEN='paste-token-here' k6 run load-tests/incidents-write.js