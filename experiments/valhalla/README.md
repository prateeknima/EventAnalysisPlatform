# Project Valhalla Experiment

This folder contains a small JVM performance experiment inspired by Project Valhalla.

Project Valhalla is an OpenJDK project focused on improving Java's object model with value objects. The important idea is that some domain objects do not need identity. They are only meaningful because of their field values.

In this project, examples of value-like data are:

- incident priority scores
- search ranking metadata
- enrichment result summaries
- small immutable values created during high-volume processing

These objects are good candidates for Valhalla-style modelling because the application cares about the value, not the object's identity.

## Why This Is Separate From The Main App

The main Spring Boot service should stay on stable Java features.

Valhalla features require an early-access JDK and preview flags, so this experiment is intentionally separate from:

- `pom.xml`
- Spring Boot runtime
- CI
- Kafka/Jackson/jOOQ models
- production Kubernetes deployment

This keeps the production app stable while still showing JVM performance exploration.

## Production Use Case

The production application now has a ranked search path:

```text
GET /incidents/search/ranked?q=timeout&limit=100
```

That flow is:

```text
Elasticsearch returns candidate incidents
↓
Java computes an IncidentRankScore for each hit
↓
Java sorts candidates by operational priority and search relevance
↓
API returns ranked incident results
```

The stable production code uses:

```text
src/main/java/com/example/eventanalysisplatform/search/IncidentRankScore.java
```

This is the real use case where Valhalla could matter later: high-volume in-memory ranking where many small immutable score objects are created and scanned.

The production score is intentionally compact:

```java
record IncidentRankScore(int sortKey)
```

The `sortKey` packs ranking fields into one primitive value. This keeps the stable production implementation compact today and gives the Valhalla experiment a fairer shape to test because the value object is small and identity-free.

## Baseline Benchmark

`RecordIncidentScoreBenchmark.java` uses a normal Java record:

```java
record IncidentScore(int severityRank, int affectedServices, long timestampBucket)
```

It creates many immutable score objects, scans them, calculates a checksum, and prints approximate timing and memory information.

Run from the repository root:

```bash
javac experiments/valhalla/RecordIncidentScoreBenchmark.java
java -Xmx512m -Xlog:gc -cp experiments/valhalla RecordIncidentScoreBenchmark
```

Run with a larger object count:

```bash
java -Xmx512m -Xlog:gc -Dbenchmark.size=5000000 -cp experiments/valhalla RecordIncidentScoreBenchmark
```

## What To Observe

Look at:

- warmup size and iteration count
- allocation time
- scan time
- approximate memory delta
- GC logs
- checksum stability

The checksum is included so the JVM cannot simply ignore the work.
The manual benchmarks perform a short warmup before measurement so the JVM has a chance to load classes, collect runtime profiles, and JIT-compile the hot loops.

Recorded results are in:

```text
experiments/valhalla/results.md
```

## Valhalla Comparison

With a Project Valhalla early-access JDK, the comparison uses a value-object version of the same type.

`ValhallaIncidentScoreBenchmark.java` uses:

```java
value record IncidentScore(int severityRank, int affectedServices, long timestampBucket)
```

Run it only with a Valhalla early-access JDK:

```bash
<valhalla-jdk>/bin/javac --enable-preview --release 27 experiments/valhalla/ValhallaIncidentScoreBenchmark.java
<valhalla-jdk>/bin/java --enable-preview -Xmx512m -Xlog:gc -cp experiments/valhalla ValhallaIncidentScoreBenchmark
```

Use the same size for the record and value-record runs:

```bash
java -Xmx512m -Xlog:gc -Dbenchmark.size=5000000 -cp experiments/valhalla RecordIncidentScoreBenchmark
<valhalla-jdk>/bin/java --enable-preview -Xmx512m -Xlog:gc -Dbenchmark.size=5000000 -cp experiments/valhalla ValhallaIncidentScoreBenchmark
```

Compare:

- Does memory usage decrease?
- Does allocation pressure decrease?
- Do GC pauses change?
- Does scanning large arrays become faster?

The value-record source is intentionally not compiled by Maven or CI.

## Packed Rank Signal Benchmark

The more production-relevant benchmark is:

```text
PackedRecordRankSignalBenchmark.java
PackedValhallaRankSignalBenchmark.java
```

These compare a compact rank signal with one primitive `int sortKey`, matching the production `IncidentRankScore` shape.

Run:

```bash
<valhalla-jdk>/bin/javac --enable-preview --release 27 -d .tools/valhalla/classes \
  experiments/valhalla/PackedRecordRankSignalBenchmark.java \
  experiments/valhalla/PackedValhallaRankSignalBenchmark.java

<valhalla-jdk>/bin/java --enable-preview -Xmx512m -Xlog:gc \
  -Dbenchmark.size=5000000 \
  -cp .tools/valhalla/classes PackedRecordRankSignalBenchmark

<valhalla-jdk>/bin/java --enable-preview -Xmx512m -Xlog:gc \
  -Dbenchmark.size=5000000 \
  -cp .tools/valhalla/classes PackedValhallaRankSignalBenchmark
```

To check whether the result scales, run the same benchmark with 10 million values and a larger heap:

```bash
<valhalla-jdk>/bin/java --enable-preview -Xmx1g -Xlog:gc \
  -Dbenchmark.size=10000000 \
  -cp .tools/valhalla/classes PackedRecordRankSignalBenchmark

<valhalla-jdk>/bin/java --enable-preview -Xmx1g -Xlog:gc \
  -Dbenchmark.size=10000000 \
  -cp .tools/valhalla/classes PackedValhallaRankSignalBenchmark
```

If the Java source changes, compile again before running. The `java` command runs compiled `.class` files, not the `.java` source directly.

Warmup can be tuned with:

```bash
-Dbenchmark.warmup.iterations=3
-Dbenchmark.warmup.size=1000000
```

The warmup is intentionally separate from the measured allocation and scan phases. This keeps the result easy to explain:

```text
warmup first
then measured allocation
then measured scan
```

## Production Relevance

This experiment compares ordinary Java records with the kind of identity-free value objects Project Valhalla is designed to support. It is relevant because incident scoring and search ranking can create many small immutable objects, and those objects are often limited by allocation pressure, heap layout, and garbage collection behavior.

The production application does not depend on Valhalla. The experiment is isolated so the main service remains stable while still demonstrating JVM performance awareness.
