# JVM Performance Tuning

This service runs as a Java application inside a Kubernetes container. That means JVM memory tuning must be aligned with the pod memory limit, otherwise the JVM can use too much memory and Kubernetes can terminate the pod with `OOMKilled`.

The API deployment currently sets:

- CPU request: `250m`
- CPU limit: `1`
- Memory request: `512Mi`
- Memory limit: `1Gi`

The JVM options are configured in `k8s/api-service/configmap.yml` using `JAVA_TOOL_OPTIONS`.

## Current JVM Options

`-XX:+UseG1GC`

Uses the G1 garbage collector. G1 is a good general-purpose collector for service applications because it balances throughput with predictable pause times.

`-XX:MaxRAMPercentage=75`

Allows the Java heap to use up to 75% of the container memory limit. With a `1Gi` pod memory limit, the heap can grow to roughly `768Mi`.

The remaining memory is left for non-heap JVM memory, thread stacks, metaspace, native memory, direct buffers, and the application runtime.

`-XX:+HeapDumpOnOutOfMemoryError`

Creates a heap dump if the JVM runs out of memory. This is useful during debugging because it lets us inspect what objects were consuming memory.

`-XX:HeapDumpPath=/tmp`

Writes heap dumps to `/tmp` inside the container. This is acceptable for local Kubernetes testing. In production, heap dumps should usually be written to persistent storage or exported before the pod is replaced.

`-Xlog:gc*:stdout:time,level,tags`

Writes garbage collection logs to standard output. Kubernetes can collect these logs using `kubectl logs`, and production log collectors can ship them to a central logging platform.

## Why This Matters

Without container-aware JVM tuning, the service can look healthy during light testing but fail under memory pressure. This tuning makes memory behavior easier to reason about:

- Kubernetes controls the pod memory limit.
- The JVM sizes its heap inside that limit.
- GC behavior is visible in logs.
- Grafana shows heap, non-heap, and GC metrics.
- Load tests can be compared against JVM memory behavior.

## How To Apply

Apply the updated ConfigMap and restart the API deployment:

`kubectl apply -f k8s/api-service/configmap.yml`

`kubectl rollout restart deployment/api-service`

`kubectl rollout status deployment/api-service`

## How To Verify

Check that the application picked up the JVM options:

`kubectl logs deployment/api-service | grep JAVA_TOOL_OPTIONS`

Check for GC log output:

`kubectl logs deployment/api-service | grep "\\[gc"`

Check pod health:

`kubectl get pods`

`curl http://localhost:8080/actuator/health`

In Grafana, observe:

- JVM heap used
- JVM non-heap used
- GC pause count
- GC pause duration
- HTTP latency during load tests