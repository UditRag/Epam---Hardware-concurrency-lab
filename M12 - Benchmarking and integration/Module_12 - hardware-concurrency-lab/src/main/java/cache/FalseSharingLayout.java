package cache;

/**
 * Index mapping for a strided-counter experiment. This controls logical array
 * spacing only; it does not guarantee physical cache-line alignment.
 */
public final class FalseSharingLayout {
    public static final int ASSUMED_CACHE_LINE_BYTES = 64;
    public static final int LONG_BYTES = Long.BYTES;
    public static final int PADDED_STRIDE = 16; // 16 longs = 128-byte payload stride
    public static final int MAX_BENCHMARK_THREADS = 256;

    private FalseSharingLayout() { }

    public static int indexForWorker(int worker, int stride) {
        if (worker < 0 || worker >= MAX_BENCHMARK_THREADS) {
            throw new IllegalArgumentException("worker must be in [0, " + MAX_BENCHMARK_THREADS + ")");
        }
        if (stride != 1 && stride != PADDED_STRIDE) {
            throw new IllegalArgumentException("stride must be 1 (adjacent) or " + PADDED_STRIDE + " (padded)");
        }
        return worker * stride;
    }

    public static long[] newCounters(int stride) {
        if (stride != 1 && stride != PADDED_STRIDE) {
            throw new IllegalArgumentException("stride must be 1 or " + PADDED_STRIDE);
        }
        return new long[MAX_BENCHMARK_THREADS * stride];
    }
}
