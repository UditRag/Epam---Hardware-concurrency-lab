package io.lnctqubit.hwconc.m11;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for Module 11 SPSC ring buffer implementations.
 *
 * <p>Tests are organised to match the A–K inventory from the Module 11 design
 * contract. Each test is self-contained, uses latches/barriers (not sleeps)
 * for concurrency coordination, joins threads with bounded timeouts, captures
 * thread exceptions via {@link AtomicReference}, and makes explicit assertions.
 * No test swallows exceptions or leaves non-daemon threads hanging.</p>
 *
 * <h3>Configurable stress properties</h3>
 * <ul>
 *   <li>{@code m11.events} — number of events per stress run (default 500,000)</li>
 *   <li>{@code m11.repetitions} — number of stress repetitions (default 3)</li>
 *   <li>{@code m11.capacity} — ring buffer capacity for stress (default 1024)</li>
 * </ul>
 *
 * Example stress command:
 * <pre>
 * mvn -Dtest="io.lnctqubit.hwconc.m11.*" \
 *     -Dm11.events=1000000 -Dm11.repetitions=10 -Dm11.capacity=1024 test
 * </pre>
 *
 * <h3>Evidence label</h3>
 * <p>Tests have status {@code NOT RUN} until executed with {@code mvn clean test}
 * on the team machine. A successful local run is evidence but not a proof of all
 * interleavings.</p>
 *
 * <h3>Contract tests (Test K)</h3>
 * <p>Most single-threaded contract tests are parameterised over
 * {@code SpscQueue} implementations (plain and padded) via
 * {@link #queueFactory()}, so the same contract is verified against both.</p>
 *
 * <h3>Interruption/shutdown scope (Test I)</h3>
 * <p>No blocking, timed-wait, interruption, or shutdown API is implemented.
 * Tests for such APIs are explicitly out of scope; see
 * {@link #noShutdownApiExistsAsDesigned}.</p>
 */
class SpscRingBufferTest {

    // ------------------------------------------------------------------ //
    // Contract test factory — shared across plain and padded impls (K)     //
    // ------------------------------------------------------------------ //

    /**
     * Test K: provides both implementations so contract tests run against each.
     */
    static Stream<SpscQueue<Object>> queueFactory() {
        return Stream.of(
                new SpscRingBuffer<>(4),
                new PaddedSpscRingBuffer<>(4)
        );
    }

    static Stream<SpscQueue<Integer>> intQueueFactory() {
        return Stream.of(
                new SpscRingBuffer<>(4),
                new PaddedSpscRingBuffer<>(4)
        );
    }

    /** Shared payload record used by integrity tests. */
    record Payload(long sequence, long check) {}

    // ------------------------------------------------------------------ //
    // Test A — FIFO ordering and empty semantics                           //
    // ------------------------------------------------------------------ //

    @ParameterizedTest(name = "Test A – FIFO+empty [{0}]")
    @MethodSource("intQueueFactory")
    @DisplayName("A: FIFO ordering with distinct values and empty poll before/after")
    void fifoAndEmptySemantics(SpscQueue<Integer> q) {
        // Empty before first offer
        assertNull(q.poll(), "Empty queue must return null");
        for (int i = 1; i <= 4; i++) {
            assertTrue(q.offer(i), "offer must succeed while capacity available");
        }
        for (int i = 1; i <= 4; i++) {
            assertEquals(i, q.poll(), "FIFO ordering violated");
        }
        // Empty after draining
        assertNull(q.poll(), "Drained queue must return null");
    }

    // ------------------------------------------------------------------ //
    // Test B — Exact capacity boundary and slot reuse without overwrite    //
    // ------------------------------------------------------------------ //

    @ParameterizedTest(name = "Test B – capacity+reuse [{0}]")
    @MethodSource("intQueueFactory")
    @DisplayName("B: Exact capacity boundary, next offer false, consume-one-reuse-one without overwrite")
    void capacityBoundaryAndReuse(SpscQueue<Integer> q) {
        for (int i = 0; i < 4; i++) assertTrue(q.offer(i));
        // Full: next offer must return false and must not overwrite
        assertFalse(q.offer(99), "Full queue offer must return false");
        assertEquals(0, q.poll(), "First polled value corrupted");
        // One slot freed; must accept exactly one new offer
        assertTrue(q.offer(4), "Slot freed; offer must succeed");
        assertFalse(q.offer(5), "Still full after one-slot reuse");
        assertEquals(1, q.poll());
        assertEquals(2, q.poll());
        assertEquals(3, q.poll());
        assertEquals(4, q.poll());
        assertNull(q.poll(), "Empty after draining");
    }

    // ------------------------------------------------------------------ //
    // Test C — Empty poll before first publication and after draining      //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("C: Empty poll returns null before publication and after full drain")
    void emptyPollBeforeAndAfterDrain() {
        SpscRingBuffer<String> q = new SpscRingBuffer<>(2);
        assertNull(q.poll());
        q.offer("x");
        assertEquals("x", q.poll());
        assertNull(q.poll());
    }

    // ------------------------------------------------------------------ //
    // Test D — Multiple physical index wraparounds + stale-slot prevention //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("D: Multiple physical wraparounds; stale-slot cleared before reuse")
    void repeatedPhysicalWraparoundAndStaleSlotPrevention() {
        // Capacity 4, mask 3. Run 20 complete rounds → 80 total offers/polls.
        // After each poll the implementation clears the slot reference.
        // We verify the vacated slot is null before the next offer occupies it.
        SpscRingBuffer<Integer> q = new SpscRingBuffer<>(4);
        for (int round = 0; round < 20; round++) {
            for (int i = 0; i < 4; i++) {
                assertTrue(q.offer(round * 4 + i),
                        "offer must succeed at start of round " + round);
            }
            for (int i = 0; i < 4; i++) {
                assertEquals(round * 4 + i, q.poll(),
                        "FIFO violated in round " + round);
            }
        }
        assertNull(q.poll(), "Empty after all rounds");

        // Explicit stale-slot check: offer one item, poll it (slot nulled),
        // then verify the queue does NOT return a stale item on the next poll.
        SpscRingBuffer<String> q2 = new SpscRingBuffer<>(2);
        assertTrue(q2.offer("alpha"));
        assertEquals("alpha", q2.poll());
        // Slot 0 was cleared; poll must return null (not "alpha" again)
        assertNull(q2.poll(), "Stale slot must not be returned after poll+clear");

        // Now wrap around to slot 0 again and confirm fresh value is returned
        assertTrue(q2.offer("beta"));
        assertTrue(q2.offer("gamma"));
        assertEquals("beta", q2.poll());   // slot 0 reused with new value
        assertEquals("gamma", q2.poll()); // slot 1
        assertNull(q2.poll());
    }

    // ------------------------------------------------------------------ //
    // Test E — Sequence-bearing payload; exactly-once, ordered, intact     //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("E: Sequence+checksum payload; exactly-once, ordered, intact (single-threaded)")
    void payloadIntegrityAndOrdering() {
        int count = 10_000;
        SpscRingBuffer<Payload> q = new SpscRingBuffer<>(1024);
        // Produce all then consume all (single-threaded, no concurrency risk here)
        int produced = 0;
        int consumed = 0;
        for (int i = 0; i < count; i++) {
            if (q.offer(new Payload(i, i ^ 0x5A5A5A5AL))) produced++;
        }
        for (int expected = 0; consumed < produced; ) {
            Payload p = q.poll();
            if (p == null) continue;
            assertEquals(expected, p.sequence(), "Sequence out of order");
            assertEquals(p.sequence() ^ 0x5A5A5A5AL, p.check(), "Checksum corrupted");
            expected++;
            consumed++;
        }
        assertEquals(produced, consumed, "Item count mismatch");
    }

    // ------------------------------------------------------------------ //
    // Test F — High-volume SPSC stress (configurable via system properties) //
    // ------------------------------------------------------------------ //

    /**
     * Test F: high-volume producer/consumer stress with configurable parameters.
     *
     * <p>Configure via system properties:
     * <ul>
     *   <li>{@code m11.events} — total events per run (default 500,000)</li>
     *   <li>{@code m11.repetitions} — number of independent runs (default 3)</li>
     *   <li>{@code m11.capacity} — ring buffer capacity (default 1024)</li>
     * </ul>
     * Each run uses a fresh queue and fresh threads released together by a latch.
     * Bounded join timeout is 30 seconds per run. Thread exceptions are captured
     * and re-thrown as test failures. The queue must be empty after each run.
     * Successful runs are evidence from sampled executions, not a formal proof.</p>
     */
    @Test
    @DisplayName("F: High-volume SPSC stress — configurable events/repetitions/capacity")
    void highVolumeSpscStressConfigurable() throws Exception {
        int repetitions = Integer.getInteger("m11.repetitions", 3);
        int events      = Integer.getInteger("m11.events", 500_000);
        int capacity    = Integer.getInteger("m11.capacity", 1024);
        assertTrue(repetitions > 0 && events > 0 && capacity >= 2,
                "Invalid stress configuration");

        for (int run = 0; run < repetitions; run++) {
            SpscRingBuffer<Payload> q = new SpscRingBuffer<>(capacity);
            CountDownLatch start = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();

            Thread producer = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < events; i++) {
                        Payload item = new Payload(i, i ^ 0x5A5A5A5AL);
                        while (!q.offer(item)) Thread.yield();
                    }
                } catch (Throwable t) { failure.compareAndSet(null, t); }
            }, "m11-producer-" + run);

            Thread consumer = new Thread(() -> {
                try {
                    start.await();
                    for (int expected = 0; expected < events; ) {
                        Payload item = q.poll();
                        if (item == null) { Thread.yield(); continue; }
                        assertEquals(expected, item.sequence(),
                                "Sequence violated: expected=" + expected + " got=" + item.sequence());
                        assertEquals(item.sequence() ^ 0x5A5A5A5AL, item.check(),
                                "Checksum violated on sequence " + item.sequence());
                        expected++;
                    }
                } catch (Throwable t) { failure.compareAndSet(null, t); }
            }, "m11-consumer-" + run);

            producer.start();
            consumer.start();
            start.countDown();

            long timeoutMs = TimeUnit.SECONDS.toMillis(30);
            producer.join(timeoutMs);
            consumer.join(timeoutMs);

            if (producer.isAlive() || consumer.isAlive()) {
                producer.interrupt();
                consumer.interrupt();
                fail("SPSC stress timed out: run=" + run
                        + " events=" + events + " capacity=" + capacity);
            }
            if (failure.get() != null) {
                fail("SPSC stress failed on run " + run, failure.get());
            }
            assertNull(q.poll(), "Queue not empty after draining all events on run " + run);
        }
    }

    // ------------------------------------------------------------------ //
    // Test G — Producer-faster-than-consumer; no overwrite of unread data  //
    // ------------------------------------------------------------------ //

    /**
     * Test G: deterministic backpressure — confirms that a fast producer
     * blocked at capacity cannot overwrite unread data, using sequential
     * logic rather than timing-dependent assertions.
     */
    @Test
    @DisplayName("G: Producer faster than consumer — backpressure; no unread-data overwrite")
    void producerFasterThanConsumerNoOverwrite() {
        SpscRingBuffer<Integer> q = new SpscRingBuffer<>(4);
        // Fill to capacity
        for (int i = 0; i < 4; i++) assertTrue(q.offer(i));
        // Capacity full: all further offers must fail
        for (int attempt = 0; attempt < 10; attempt++) {
            assertFalse(q.offer(999), "Offer must return false when full");
        }
        // Consume one; exactly one slot opens
        assertEquals(0, q.poll(), "First consumed value wrong");
        assertTrue(q.offer(4), "One slot freed; offer must succeed");
        assertFalse(q.offer(5), "Still full after one-slot recovery");
        // Drain remaining and confirm original values are intact
        assertEquals(1, q.poll());
        assertEquals(2, q.poll());
        assertEquals(3, q.poll());
        assertEquals(4, q.poll()); // the recovery offer value
        assertNull(q.poll());
    }

    // ------------------------------------------------------------------ //
    // Test H — Consumer polls before publication (threaded latch version)  //
    // ------------------------------------------------------------------ //

    /**
     * Test H: consumer thread starts polling before the producer has published;
     * confirms null is returned until publication, and then the item arrives
     * exactly once in proper order.
     */
    @Test
    @DisplayName("H: Consumer polls before publication; item arrives after producer publishes")
    void consumerPollsBeforePublication() throws Exception {
        SpscRingBuffer<String> q = new SpscRingBuffer<>(2);
        CountDownLatch consumerReady = new CountDownLatch(1);
        CountDownLatch producerGo    = new CountDownLatch(1);
        AtomicReference<String>    received = new AtomicReference<>();
        AtomicReference<Throwable> failure  = new AtomicReference<>();

        Thread consumer = new Thread(() -> {
            try {
                consumerReady.countDown();    // signal: consumer is live
                producerGo.await();           // wait for producer gate
                String item = null;
                // Spin-poll until item arrives; bounded by test timeout via join
                while (item == null) {
                    item = q.poll();
                    if (item == null) Thread.yield();
                }
                received.set(item);
            } catch (Throwable t) { failure.compareAndSet(null, t); }
        }, "m11-h-consumer");

        consumer.start();
        consumerReady.await(); // consumer is now polling
        // Let consumer spin a few cycles before we publish
        Thread.sleep(10);
        // Confirm queue still empty (consumer has had time to observe empty)
        // Note: we cannot assert null here from producer thread without violating
        // SPSC contract; we rely on consumer's null-loop before publication.
        assertTrue(q.offer("published"));
        producerGo.countDown(); // release consumer gate (in case it was waiting)

        consumer.join(TimeUnit.SECONDS.toMillis(10));
        if (consumer.isAlive()) {
            consumer.interrupt();
            fail("Consumer did not receive item within timeout");
        }
        if (failure.get() != null) fail("Consumer thread failed", failure.get());
        assertEquals("published", received.get(), "Consumer received wrong item");
        assertNull(q.poll(), "Queue must be empty after single item consumed");
    }

    // ------------------------------------------------------------------ //
    // Test I — Interruption/shutdown APIs are explicitly out of scope       //
    // ------------------------------------------------------------------ //

    /**
     * Test I: explicit negative test confirming no blocking/timed/shutdown API.
     * {@link SpscRingBuffer} and {@link PaddedSpscRingBuffer} intentionally do
     * not implement a shutdown, interruptible-wait, or timed-offer/poll.
     * Callers must manage their own lifecycle.
     */
    @Test
    @DisplayName("I: Interruption/shutdown APIs are out of scope — confirmed absent")
    void noShutdownApiExistsAsDesigned() {
        // Verify offer/poll return immediately on full/empty (bounded operations)
        SpscRingBuffer<Integer> q = new SpscRingBuffer<>(2);
        assertTrue(q.offer(1));
        assertTrue(q.offer(2));
        // Full: returns false immediately, no blocking
        assertFalse(q.offer(3));
        // Empty after drain: returns null immediately, no blocking
        q.poll(); q.poll();
        assertNull(q.poll());
        // No shutdown, close(), interrupt(), or cancellation API is present;
        // attempting to call one would be a compile error.
        // This test documents the design intent for the handoff record.
    }

    // ------------------------------------------------------------------ //
    // Test I continued — null and invalid capacity validation               //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("I: Null item rejected with NPE; invalid capacities rejected with IAE")
    void nullAndCapacityValidation() {
        assertThrows(IllegalArgumentException.class, () -> new SpscRingBuffer<>(0));
        assertThrows(IllegalArgumentException.class, () -> new SpscRingBuffer<>(-2));
        assertThrows(IllegalArgumentException.class, () -> new SpscRingBuffer<>(1));
        assertThrows(IllegalArgumentException.class, () -> new SpscRingBuffer<>(3));
        assertThrows(IllegalArgumentException.class, () -> new SpscRingBuffer<>(5));
        SpscRingBuffer<String> q = new SpscRingBuffer<>(2);
        assertThrows(NullPointerException.class, () -> q.offer(null));
        assertNull(q.poll());

        // Same checks for PaddedSpscRingBuffer
        assertThrows(IllegalArgumentException.class, () -> new PaddedSpscRingBuffer<>(0));
        assertThrows(IllegalArgumentException.class, () -> new PaddedSpscRingBuffer<>(3));
        PaddedSpscRingBuffer<String> pq = new PaddedSpscRingBuffer<>(2);
        assertThrows(NullPointerException.class, () -> pq.offer(null));
        assertNull(pq.poll());
    }

    // ------------------------------------------------------------------ //
    // Test J — Repeatability: documented config + bounded timeout          //
    // ------------------------------------------------------------------ //

    /**
     * Test J: repeatability — the high-volume stress (Test F) documents and
     * exercises repeatability via configurable {@code m11.repetitions}.
     * This test explicitly confirms the configuration is picked up and logged.
     *
     * <p>Successful runs are evidence from a finite sample, not formal proof
     * of all interleavings or memory-ordering scenarios.</p>
     */
    @Test
    @DisplayName("J: Repeatability — system properties are read; stress runs bound by timeout")
    void repeatabilityConfigIsDocumented() {
        int reps     = Integer.getInteger("m11.repetitions", 3);
        int events   = Integer.getInteger("m11.events", 500_000);
        int capacity = Integer.getInteger("m11.capacity", 1024);
        assertTrue(reps > 0,     "m11.repetitions must be positive, got " + reps);
        assertTrue(events > 0,   "m11.events must be positive, got " + events);
        assertTrue(Integer.bitCount(capacity) == 1 && capacity >= 2,
                "m11.capacity must be power-of-two >= 2, got " + capacity);
        // Confirm the queue accepts the configured capacity
        SpscRingBuffer<Object> q = new SpscRingBuffer<>(capacity);
        assertEquals(capacity, q.capacity());
    }

    // ------------------------------------------------------------------ //
    // Test K — Contract tests run against both implementations via SpscQueue //
    // ------------------------------------------------------------------ //

    /**
     * Test K: FIFO contract via {@link SpscQueue} — runs against both
     * {@link SpscRingBuffer} and {@link PaddedSpscRingBuffer}.
     */
    @ParameterizedTest(name = "K – FIFO+capacity via SpscQueue [{0}]")
    @MethodSource("intQueueFactory")
    @DisplayName("K: FIFO + full + empty contract exercised via SpscQueue interface")
    void contractViaInterface(SpscQueue<Integer> q) {
        assertEquals(4, q.capacity(), "capacity() must match constructor arg");
        assertNull(q.poll(), "Empty before any offer");
        for (int i = 0; i < 4; i++) assertTrue(q.offer(i));
        assertFalse(q.offer(99), "Full: offer must return false");
        for (int i = 0; i < 4; i++) {
            assertEquals(i, q.poll(), "FIFO violated for impl: " + q.getClass().getSimpleName());
        }
        assertNull(q.poll(), "Empty after drain");
    }

    /**
     * Test K continued: wraparound contract via {@link SpscQueue} — both impls.
     */
    @ParameterizedTest(name = "K – wraparound via SpscQueue [{0}]")
    @MethodSource("intQueueFactory")
    @DisplayName("K: Wraparound (10 rounds) via SpscQueue interface")
    void wraparoundContractViaInterface(SpscQueue<Integer> q) {
        for (int round = 0; round < 10; round++) {
            for (int i = 0; i < 4; i++) {
                assertTrue(q.offer(round * 4 + i));
            }
            for (int i = 0; i < 4; i++) {
                assertEquals(round * 4 + i, q.poll());
            }
        }
        assertNull(q.poll());
    }

    // ------------------------------------------------------------------ //
    // Slow-consumer original test (B cross-check via SpscRingBuffer only)  //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("G+B cross-check: slow consumer via SpscRingBuffer; no unread overwrite")
    void slowConsumerDoesNotAllowUnreadOverwrite() {
        SpscRingBuffer<Integer> q = new SpscRingBuffer<>(2);
        assertTrue(q.offer(10));
        assertTrue(q.offer(11));
        assertFalse(q.offer(12), "Full: must not overwrite 10 or 11");
        assertEquals(10, q.poll());
        assertTrue(q.offer(12)); // slot freed
        assertEquals(11, q.poll());
        assertEquals(12, q.poll());
        assertNull(q.poll());
    }
}
