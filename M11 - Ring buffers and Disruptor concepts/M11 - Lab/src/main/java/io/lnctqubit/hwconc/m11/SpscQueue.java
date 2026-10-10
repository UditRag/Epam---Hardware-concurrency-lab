package io.lnctqubit.hwconc.m11;

/**
 * Minimal contract for a single-producer / single-consumer bounded queue.
 *
 * <p>Exactly one producer thread may call {@link #offer} and exactly one
 * consumer thread may call {@link #poll}. Null items are rejected; null from
 * {@link #poll} unambiguously means empty. Full/empty conditions return
 * immediately without blocking, spinning, or allocation.</p>
 *
 * <p>This interface exists solely to allow shared contract-test helpers and
 * benchmark harnesses to run against multiple implementations (e.g.
 * {@link SpscRingBuffer} and {@link PaddedSpscRingBuffer}) without
 * duplicating test logic. It is intentionally minimal and does not attempt
 * to replicate {@code java.util.Queue} semantics.</p>
 *
 * <p><strong>Progress note:</strong> implementations of this interface are
 * expected to execute {@link #offer} and {@link #poll} in a bounded number
 * of steps under the stated SPSC ownership contract. "Bounded-step" describes
 * the method operations, not the caller's overall progress when retrying.</p>
 */
public interface SpscQueue<T> {

    /**
     * Attempts to publish one item from the sole producer thread.
     *
     * @param item non-null item to publish
     * @return {@code true} if accepted; {@code false} if the queue is full
     * @throws NullPointerException if {@code item} is null
     */
    boolean offer(T item);

    /**
     * Attempts to consume the next item from the sole consumer thread.
     *
     * @return the next FIFO item, or {@code null} if the queue is empty
     */
    T poll();

    /**
     * Returns the fixed preallocated capacity of this queue.
     *
     * @return immutable capacity (power of two, at least 2)
     */
    int capacity();
}
