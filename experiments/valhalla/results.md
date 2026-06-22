# Project Valhalla Benchmark Results

Date: 2026-06-22

Runtime:

```text
openjdk version "27-jep401ea3" 2026-09-15
OpenJDK Runtime Environment (build 27-jep401ea3+1-1)
OpenJDK 64-Bit Server VM (build 27-jep401ea3+1-1, mixed mode, sharing)
```

Source:

```text
https://jdk.java.net/valhalla/
```

## Benchmark

The experiment compares:

- `RecordIncidentScoreBenchmark`
- `ValhallaIncidentScoreBenchmark`

Both create and scan many immutable incident score objects.

The production app now has the same style of workload in ranked search:

```text
GET /incidents/search/ranked?q=timeout&limit=100
```

The production path uses stable Java records. The Valhalla benchmark explores whether the ranking score object would be a candidate for value-object optimization in the future.

JVM flags:

```text
--enable-preview -Xmx1g
```

Manual benchmark warmup:

```text
warmupIterations=3
warmupSize=1000000
```

## Results

### Production-Shaped Packed Rank Signal Benchmark

This benchmark uses one primitive `int sortKey`, matching the production `IncidentRankScore` shape.
The packed score contains priority score, affected service count, and Elasticsearch search score basis points.

5,000,000 objects:

| Type | Allocation | Scan | Approx Memory Delta |
| --- | ---: | ---: | ---: |
| packed record | 60ms | 7ms | 99MB |
| packed value record | 18ms | 17ms | 43MB |

10,000,000 objects:

| Type | Allocation | Scan | Approx Memory Delta |
| --- | ---: | ---: | ---: |
| packed record | 137ms | 14ms | 197MB |
| packed value record | 31ms | 27ms | 81MB |

Outcome:

```text
At 5M values, the packed value record reduced approximate memory usage
from 99MB to 43MB and reduced allocation time from 60ms to 18ms.

At 10M values, the packed value record reduced approximate memory usage
from 197MB to 81MB and reduced allocation time from 137ms to 31ms.
```

This is the first benchmark shape that showed a clear Valhalla advantage in this project.
The tradeoff is that scan time was slower for the packed value record in this manual run.

### Earlier Non-Packed Incident Score Benchmark

This was the first benchmark shape. It used a larger incident score object, not the packed `IncidentRankScore`
shape used by the ranked-search code. These numbers are kept as a control result because they show that
Valhalla does not automatically improve every object layout.

2,000,000 objects:

| Type | Allocation | Scan | Approx Memory Delta |
| --- | ---: | ---: | ---: |
| record | 67ms | 9ms | 72MB |
| value record | 46ms | 10ms | 86MB |

5,000,000 objects:

| Type | Allocation | Scan | Approx Memory Delta |
| --- | ---: | ---: | ---: |
| record | 118ms | 20ms | 178MB |
| value record | 137ms | 16ms | 218MB |

## Interpretation

The value-record version compiled and ran successfully on the Valhalla early-access JDK.

The original larger `IncidentScore` benchmark did not prove a memory reduction. The value-record version was faster for allocation in the 2 million object run, but slower in the 5 million object run. Scan time was similar.

The packed rank signal benchmark did show a clear advantage for Valhalla in allocation time and approximate memory usage. The important difference is object shape: the packed benchmark stores ranking data in one primitive `int`, making it much easier for the EA JVM to optimize.

The benchmark did not show a scan-time win. For this project, the useful Valhalla result is lower allocation pressure and lower heap usage, not faster reads.

This result is still useful because it shows an important production lesson: using `value record` does not automatically guarantee better performance for every workload.

JEP 401 explains that heap flattening and scalarization are JVM optimizations, not direct language guarantees. The JVM decides when it can apply them. Future null-restricted value class types are expected to enable denser heap flattening in fields and arrays.

## Conclusion

Project Valhalla was tested with a real early-access JDK.

The experiment shows in this environment:

- the value-record source compiles with the Valhalla EA compiler
- the value-record benchmark runs with preview features enabled
- the output can be compared against a normal record baseline
- compact value records reduced allocation time and memory usage for this ranking-score shape in the 5M and 10M manual runs

The experiment does not prove:

- guaranteed memory reduction for all object shapes
- guaranteed latency improvement
- faster scan/read time
- production readiness for the Spring Boot service

The production application should continue to use stable Java features. Valhalla remains isolated as a JVM performance experiment.
