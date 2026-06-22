public class PackedRecordRankSignalBenchmark {

    private static volatile long warmupSink;

    private record RankSignal(int sortKey) {
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

    public static void main(String[] args) {
        int size = Integer.getInteger("benchmark.size", 10_000_000);
        int warmupIterations = Integer.getInteger("benchmark.warmup.iterations", 3);
        int warmupSize = Integer.getInteger(
                "benchmark.warmup.size",
                Math.min(size, 1_000_000)
        );

        Runtime runtime = Runtime.getRuntime();

        warmUp(warmupSize, warmupIterations);
        runtime.gc();

        long memoryBefore = usedMemory(runtime);
        long startNanos = System.nanoTime();

        RankSignal[] signals = new RankSignal[size];

        for (int i = 0; i < size; i++) {
            signals[i] = new RankSignal(pack(i % 101, i % 256, i % 10_000));
        }

        long allocationFinishedNanos = System.nanoTime();

        long checksum = 0;
        for (RankSignal signal : signals) {
            checksum += signal.priorityScore();
            checksum += signal.affectedServiceCount();
            checksum += signal.searchScoreBasisPoints();
        }

        long endNanos = System.nanoTime();
        long memoryAfter = usedMemory(runtime);

        printResult(
                size,
                warmupIterations,
                warmupSize,
                checksum,
                allocationFinishedNanos - startNanos,
                endNanos - allocationFinishedNanos,
                memoryAfter - memoryBefore
        );
    }

    private static void warmUp(
            int size,
            int iterations
    ) {
        long checksum = 0;

        for (int iteration = 0; iteration < iterations; iteration++) {
            RankSignal[] signals = new RankSignal[size];

            for (int i = 0; i < size; i++) {
                signals[i] = new RankSignal(pack(i % 101, i % 256, i % 10_000));
            }

            for (RankSignal signal : signals) {
                checksum += signal.priorityScore();
                checksum += signal.affectedServiceCount();
                checksum += signal.searchScoreBasisPoints();
            }
        }

        warmupSink = checksum;
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

    private static long usedMemory(Runtime runtime) {
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static void printResult(
            int size,
            int warmupIterations,
            int warmupSize,
            long checksum,
            long allocationNanos,
            long scanNanos,
            long memoryDeltaBytes
    ) {
        System.out.println("type=packed-record");
        System.out.println("size=" + size);
        System.out.println("warmupIterations=" + warmupIterations);
        System.out.println("warmupSize=" + warmupSize);
        System.out.println("checksum=" + checksum);
        System.out.println("allocationMs=" + allocationNanos / 1_000_000);
        System.out.println("scanMs=" + scanNanos / 1_000_000);
        System.out.println("approxMemoryDeltaMb=" + memoryDeltaBytes / (1024 * 1024));
    }
}
