# Valhalla JMH Benchmark

This is a separate JMH benchmark for the ranked-search score shape used by the main service.

The main Spring Boot application stays on stable Java. This experiment requires the Project Valhalla early-access JDK because it compares:

- a normal Java `record`
- a Valhalla `value record`

Both versions store the same packed `int sortKey`:

```text
priorityScore | affectedServiceCount | searchScoreBasisPoints
```

## Why JMH

The manual benchmark in `experiments/valhalla` is useful for quick checks, but it is still hand-written timing code.

JMH is the standard Java microbenchmark harness. It gives us:

- warmup iterations
- measured iterations
- forked JVM runs
- dead-code protection through `Blackhole`
- optional GC/allocation profiling

This makes the benchmark more defensible than one manual timing run.

## Build

Run from this folder:

```bash
cd experiments/valhalla-jmh
JAVA_HOME=../../.tools/valhalla/jdk-27.jdk/Contents/Home ../../mvnw clean package
```

## Run

```bash
../../.tools/valhalla/jdk-27.jdk/Contents/Home/bin/java \
  --enable-preview \
  -jar target/valhalla-jmh-1.0.0.jar \
  RankSignalBenchmark
```

To include allocation/GC profiling:

```bash
../../.tools/valhalla/jdk-27.jdk/Contents/Home/bin/java \
  --enable-preview \
  -jar target/valhalla-jmh-1.0.0.jar \
  RankSignalBenchmark \
  -prof gc
```

## What To Compare

Compare these benchmarks:

```text
RankSignalBenchmark.packedRecord
RankSignalBenchmark.packedValueRecord
```

The important fields are:

- average time per operation
- allocation rate from `-prof gc`
- GC count and GC time

This still does not prove end-to-end API latency. It only tests the JVM cost of creating and scanning many ranked-search score values.

Recorded results are in:

```text
experiments/valhalla-jmh/results.md
```

The raw JMH JSON output is in:

```text
experiments/valhalla-jmh/jmh-results.json
```
