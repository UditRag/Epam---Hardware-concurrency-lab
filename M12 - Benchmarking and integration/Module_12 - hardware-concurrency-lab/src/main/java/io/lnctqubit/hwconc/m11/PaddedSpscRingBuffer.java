package io.lnctqubit.hwconc.m11;

import java.util.Objects;

/**
 * Padded variant of the educational bounded SPSC ring buffer.
 *
 * <p>This class has the same correctness contract and public API as
 * {@link SpscRingBuffer}. Both implement {@link SpscQueue}; this variant
 * exists only as a <em>layout hypothesis</em>: surrounding each hot volatile
 * sequence field with enough padding longs to span a typical 64-byte cache
 * line may reduce false sharing between the producer and consumer on hardware
 * where their sequences land on the same cache line.</p>
 *
 * <h3>JVM layout caveat</h3>
 * <p>Java object/field layout is JVM-implementation-specific and version-
 * specific. HotSpot may reorder fields, insert/remove header words, or apply
 * compressed references in ways that interact with explicit padding. Padding
 * longs declared in source do <em>not</em> universally guarantee physical
 * cache-line separation. Any performance difference between this variant and
 * the plain {@code SpscRingBuffer} requires a controlled JMH experiment with
 * full environment documentation; timing alone does not prove false sharing.</p>
 *
 * <h3>Padding layout intent</h3>
 * <pre>
 * [ object header (16 bytes typical with OOPS) ]
 * [ p1..p7 : 56 bytes padding before producerSequence ]
 * [ producerSequence : 8 bytes (volatile long) ]
 * [ q1..q7 : 56 bytes padding after producerSequence ]
 * [ c1..c7 : 56 bytes padding before consumerSequence ]
 * [ consumerSequence : 8 bytes (volatile long) ]
 * [ r1..r7 : 56 bytes padding after consumerSequence ]
 * </pre>
 * <p>The intent is that each sequence occupies its own 64-byte region.
 * Actual layout: <strong>UNVERIFIED</strong> — see {@code tools/LayoutProbe}.</p>
 *
 * <p>Semantic ordering is identical to {@link SpscRingBuffer}: no change to
 * the JMM publication/reclamation protocol or the SPSC ownership contract.</p>
 */
public final class PaddedSpscRingBuffer<T> implements SpscQueue<T> {

    private final Object[] entries;
    private final int mask;

    // --- padding before producerSequence ---
    @SuppressWarnings("unused")
    private long p1, p2, p3, p4, p5, p6, p7;

    /** Written only by producer; volatile write publishes entry. */
    private volatile long producerSequence;

    // --- padding after producerSequence / before consumerSequence ---
    @SuppressWarnings("unused")
    private long q1, q2, q3, q4, q5, q6, q7;
    @SuppressWarnings("unused")
    private long c1, c2, c3, c4, c5, c6, c7;

    /** Written only by consumer; volatile write releases slot. */
    private volatile long consumerSequence;

    // --- padding after consumerSequence ---
    @SuppressWarnings("unused")
    private long r1, r2, r3, r4, r5, r6, r7;

    /**
     * Capacity must be a power of two and at least two.
     *
     * @param capacity power-of-two value &ge; 2
     * @throws IllegalArgumentException if capacity is invalid
     */
    public PaddedSpscRingBuffer(int capacity) {
        if (capacity < 2 || (capacity & (capacity - 1)) != 0) {
            throw new IllegalArgumentException(
                    "capacity must be a power of two >= 2, got: " + capacity);
        }
        this.entries = new Object[capacity];
        this.mask = capacity - 1;
    }

    /**
     * Attempts to publish an item from the sole producer thread.
     *
     * @param item non-null item; null throws {@link NullPointerException}
     * @return {@code true} if accepted; {@code false} if full
     */
    @Override
    public boolean offer(T item) {
        Objects.requireNonNull(item, "item");
        long p = producerSequence;        // producer-local read
        long c = consumerSequence;        // volatile acquire — observe released slots
        if (p - c >= entries.length) {
            return false;                 // full; do not overwrite
        }
        entries[(int) (p & mask)] = item;
        producerSequence = p + 1;         // volatile release — publish slot
        return true;
    }

    /**
     * Attempts to consume from the sole consumer thread.
     *
     * @return next FIFO item, or {@code null} if empty
     */
    @Override
    @SuppressWarnings("unchecked")
    public T poll() {
        long c = consumerSequence;        // consumer-local read
        long p = producerSequence;        // volatile acquire — observe published entry
        if (c == p) {
            return null;                  // empty
        }
        int slot = (int) (c & mask);
        T item = (T) entries[slot];
        entries[slot] = null;             // clear reference; sequenced before release
        consumerSequence = c + 1;         // volatile release — slot may safely be reused
        return item;
    }

    /** Fixed number of preallocated slots. */
    @Override
    public int capacity() {
        return entries.length;
    }

    /**
     * Best-effort concurrent diagnostic snapshot.
     * Never use this value to drive correctness decisions.
     */
    public int size() {
        long c = consumerSequence;
        long p = producerSequence;
        long observed = p - c;
        if (observed <= 0) return 0;
        return (int) Math.min(observed, entries.length);
    }
}
