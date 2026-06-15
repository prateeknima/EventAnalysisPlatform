# Runbook: High JVM Heap Usage

## Alert

`EventAnalysisJvmHeapHigh`

## Meaning

The Spring Boot application is using a high percentage of its JVM heap memory.

Heap is where Java stores application objects at runtime.

High heap usage can lead to more frequent garbage collection, slower requests, or eventually an `OutOfMemoryError`.

## Why It Matters

If heap stays high for too long:

- API latency may increase
- Kafka consumers may slow down
- Garbage collection pauses may become longer
- The application may become unstable

## First Checks

Check Prometheus:

`sum(jvm_memory_used_bytes{area="heap"}) / sum(jvm_memory_max_bytes{area="heap"})`

Check heap areas:

`jvm_memory_used_bytes{area="heap"}`

Check GC activity:

`jvm_gc_pause_seconds_count`

`jvm_gc_pause_seconds_sum`

Check Spring Boot logs in IntelliJ.

Look for:

- `OutOfMemoryError`
- long GC pauses
- repeated exceptions
- increasing request latency

## Common Causes

### Traffic Increase

More requests can create more objects in memory.

Check:

- API request rate
- k6 test activity
- Kafka message volume

Fix:

- Reduce traffic if needed
- Scale the app
- Tune rate limits
- Investigate expensive endpoints

### Memory Leak

Objects may be kept in memory longer than expected.

Possible causes:

- unbounded maps or lists
- caches without TTL
- large objects stored in memory
- background tasks retaining references

Fix:

- Inspect recent code changes
- Check custom caches or collections
- Add limits or TTLs
- Use heap dump analysis if needed

### Large Payloads Or Queries

Large request bodies, search results, or database results can increase memory usage.

Fix:

- Add pagination
- Limit response sizes
- Avoid loading too much data at once

### JVM Heap Too Small

The configured heap may be too small for the workload.

Fix:

- Review container memory limits
- Review JVM options
- Consider tuning `-XX:MaxRAMPercentage`
- Keep using G1GC unless measurements show a reason to change

## Resolution Criteria

The issue is resolved when:

- Heap usage drops below the alert threshold
- GC activity returns to normal
- API latency is stable
- No `OutOfMemoryError` is present
- The app continues processing Kafka messages normally

## Follow-Up

After fixing:

- Document the root cause
- Add a test or limit if code caused memory growth
- Update dashboards if memory behavior was hard to see
- Revisit JVM heap settings after load or soak testing