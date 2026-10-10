package io.lnctqubit.hwconc.m11;

import java.util.Objects;

/**
 * Educational bounded single-producer/single-consumer ring buffer.
 *
 * <p>Exactly one producer thread may call {@link #offer(Object)} and exactly one
 * consumer thread may call {@link #poll()}. Calls from additional or reentrant
 * producer/consumer threads are unsupported. Null items are prohibited; null
 * from poll therefore unambiguously means empty.</p>
 *
 * <p>Storage is allocated once. Each operation has bounded work, does not block,
 * spin, lock, or allocate. A full offer returns false; an empty poll returns null.
 * Accepted events are never overwritten before consumption.</p>
 *
 * <p>Items are transferred by reference. The producer must not mutate an item
 * after successful publication until the consumer has finished with it; this
 * class does not make later mutations of the referenced object thread-safe.</p>
 */
public final class SpscRingBuffer<T> implements SpscQueue<T> {
    private final Object[] entries;
    private final int mask;

    // Written only by producer; read by consumer. Volatile write publishes entry.
    private volatile long producerSequence;
    // Written only by consumer; read by producer. Volatile write releases slot.
    private volatile long consumerSequence;

    /** Capacity must be a power of two and at least two. */
    public SpscRingBuffer(int capacity) {
        if (capacity < 2 || (capacity & (capacity - 1)) != 0) {
            throw new IllegalArgumentException("capacity must be a power of two >= 2");
        }
        this.entries = new Object[capacity];
        this.mask = capacity - 1;
    }

    /**
     * Attempts to publish an item from the sole producer thread.
     * @return true if accepted; false if full (no waiting or overwrite)
     * @throws NullPointerException if item is null
     */
    @Override
    public boolean offer(T item) {
        Objects.requireNonNull(item, "item");
        long p = producerSequence; // producer-owned sequence
        long c = consumerSequence; // volatile acquire: observe released slots
        if (p - c >= entries.length) {
            return false;
        }
        entries[(int) (p & mask)] = item;
        producerSequence = p + 1; // volatile release: publish slot contents
        return true;
    }

    /**
     * Attempts to consume from the sole consumer thread.
     * @return next item in FIFO order, or null when empty
     */
    @SuppressWarnings("unchecked")
    @Override
    public T poll() {
        long c = consumerSequence; // consumer-owned sequence
        long p = producerSequence; // volatile acquire: observe published entry
        if (c == p) {
            return null;
        }
        int slot = (int) (c & mask);
        T item = (T) entries[slot];
        entries[slot] = null; // release reference; not the publication barrier
        consumerSequence = c + 1; // volatile release: producer may safely reuse slot
        return item;
    }

    /** Fixed number of preallocated slots. */
    @Override
    public int capacity() {
        return entries.length;
    }

    /**
     * Best-effort concurrent diagnostic snapshot, not a linearizable size query.
     * Use only for monitoring; never use it to decide whether offer/poll is safe.
     */
    public int size() {
        long c = consumerSequence;
        long p = producerSequence;
        long observed = p - c;
        if (observed <= 0) return 0;
        return (int) Math.min(observed, entries.length);
    }
}
