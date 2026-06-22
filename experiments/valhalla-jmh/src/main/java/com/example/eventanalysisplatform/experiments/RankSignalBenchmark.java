package com.example.eventanalysisplatform.experiments;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgsAppend = {"--enable-preview", "-Xmx1g"})
@State(Scope.Thread)
public class RankSignalBenchmark {

    @Param({"5000000", "10000000"})
    public int size;

    private int[] sortKeys;

    @Setup
    public void setUp() {
        sortKeys = new int[size];

        for (int i = 0; i < size; i++) {
            sortKeys[i] = pack(i % 101, i % 256, i % 10_000);
        }
    }

    @Benchmark
    public long packedRecord(Blackhole blackhole) {
        RankSignalRecord[] signals = new RankSignalRecord[size];

        for (int i = 0; i < size; i++) {
            signals[i] = new RankSignalRecord(sortKeys[i]);
        }

        long checksum = 0;
        for (RankSignalRecord signal : signals) {
            checksum += signal.priorityScore();
            checksum += signal.affectedServiceCount();
            checksum += signal.searchScoreBasisPoints();
        }

        blackhole.consume(signals);
        return checksum;
    }

    @Benchmark
    public long packedValueRecord(Blackhole blackhole) {
        RankSignalValueRecord[] signals = new RankSignalValueRecord[size];

        for (int i = 0; i < size; i++) {
            signals[i] = new RankSignalValueRecord(sortKeys[i]);
        }

        long checksum = 0;
        for (RankSignalValueRecord signal : signals) {
            checksum += signal.priorityScore();
            checksum += signal.affectedServiceCount();
            checksum += signal.searchScoreBasisPoints();
        }

        blackhole.consume(signals);
        return checksum;
    }

    private static int pack(
            int priorityScore,
            int affectedServiceCount,
            int searchScoreBasisPoints
    ) {
        return (priorityScore << 24)
                | (affectedServiceCount << 16)
                | (searchScoreBasisPoints & 0xFFFF);
    }

    private record RankSignalRecord(int sortKey) {
        int priorityScore() {
            return (sortKey >>> 24) & 0x7F;
        }

        int affectedServiceCount() {
            return (sortKey >>> 16) & 0xFF;
        }

        int searchScoreBasisPoints() {
            return sortKey & 0xFFFF;
        }
    }

    private value record RankSignalValueRecord(int sortKey) {
        int priorityScore() {
            return (sortKey >>> 24) & 0x7F;
        }

        int affectedServiceCount() {
            return (sortKey >>> 16) & 0xFF;
        }

        int searchScoreBasisPoints() {
            return sortKey & 0xFFFF;
        }
    }
}
