# Valhalla JMH Results

Date: 2026-06-22

Runtime:

```text
JDK 27-jep401ea3
OpenJDK 64-Bit Server VM 27-jep401ea3+1-1
JMH 1.37
```

Command:

```bash
../../.tools/valhalla/jdk-27.jdk/Contents/Home/bin/java \
  --enable-preview \
  -jar target/valhalla-jmh-1.0.0.jar \
  RankSignalBenchmark \
  -prof gc \
  -rf json \
  -rff jmh-results.json
```

## Benchmark Shape

Both benchmarks create and scan ranked-search score values.

The normal record benchmark uses:

```java
record RankSignalRecord(int sortKey)
```

The Valhalla benchmark uses:

```java
value record RankSignalValueRecord(int sortKey)
```

Both store the same packed `int sortKey`:

```text
priorityScore | affectedServiceCount | searchScoreBasisPoints
```

## Summary

| Benchmark | Size | Average Time | Normalized Allocation | GC Time |
| --- | ---: | ---: | ---: | ---: |
| packed record | 5,000,000 | 28.782ms/op | 100,000,214 B/op | 2854ms |
| packed value record | 5,000,000 | 6.464ms/op | 40,000,061 B/op | 70ms |
| packed record | 10,000,000 | 57.459ms/op | 200,000,409 B/op | 2927ms |
| packed value record | 10,000,000 | 13.097ms/op | 80,000,107 B/op | 76ms |

## Interpretation

The JMH benchmark shows a stronger Valhalla result than the manual benchmark:

```text
5M values:
normal record allocated about 100MB/op
value record allocated about 40MB/op

10M values:
normal record allocated about 200MB/op
value record allocated about 80MB/op
```

The value-record version also completed the full benchmark operation faster.

Important detail: this JMH benchmark measures allocation and scanning together as one operation. The manual benchmark separates allocation time and scan time. That is why the manual benchmark can show slower scan time for value records while JMH still shows a faster total operation.

## Conclusion

For this compact ranked-search score shape, JMH supports the same production-relevant conclusion:

```text
Valhalla value records reduced allocation pressure and total benchmark time
for high-volume ranking-score values.
```

This does not prove end-to-end API latency improvement. It shows that the JVM-level ranking-score object shape is a plausible future Valhalla optimization candidate.
