public class RecordIncidentScoreBenchmark {

    private static volatile long warmupSink;

    private record IncidentScore(
            int severityRank,
            int affectedServices,
            long timestampBucket
    ) {
        int riskScore() {
            return Math.min(100, severityRank + Math.min(affectedServices, 10));
        }
    }

    public static void main(String[] args) {
        int size = Integer.getInteger("benchmark.size", 2_000_000);
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

        IncidentScore[] scores = new IncidentScore[size];

        for (int i = 0; i < size; i++) {
            scores[i] = new IncidentScore(
                    severityRank(i),
                    i % 12,
                    i / 1_000
            );
        }

        long allocationFinishedNanos = System.nanoTime();

        long checksum = 0;
        for (IncidentScore score : scores) {
            checksum += score.riskScore();
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
            IncidentScore[] scores = new IncidentScore[size];

            for (int i = 0; i < size; i++) {
                scores[i] = new IncidentScore(
                        severityRank(i),
                        i % 12,
                        i / 1_000
                );
            }

            for (IncidentScore score : scores) {
                checksum += score.riskScore();
            }
        }

        warmupSink = checksum;
    }

    private static int severityRank(int value) {
        return switch (value % 4) {
            case 0 -> 10;
            case 1 -> 40;
            case 2 -> 70;
            default -> 90;
        };
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
        System.out.println("type=record");
        System.out.println("size=" + size);
        System.out.println("warmupIterations=" + warmupIterations);
        System.out.println("warmupSize=" + warmupSize);
        System.out.println("checksum=" + checksum);
        System.out.println("allocationMs=" + allocationNanos / 1_000_000);
        System.out.println("scanMs=" + scanNanos / 1_000_000);
        System.out.println("approxMemoryDeltaMb=" + memoryDeltaBytes / (1024 * 1024));
    }
}
