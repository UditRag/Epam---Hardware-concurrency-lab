package memorymodel;

/**
 * SAFE publication through monitor (synchronized) locking.
 *
 * <p>Both the writer's block and every reader poll lock the SAME monitor ({@code this}). The edge is:
 * <pre>
 *   writer: data = v; ready = true; unlock(this)
 *   unlock(this)  --synchronizes-with-->  every LATER lock(this) in synchronization order   (JLS 17.4.4)
 *   reader: lock(this); read ready; read data; unlock(this)
 * </pre>
 * If the reader's lock acquisition comes after the writer's unlock in the synchronization order, then
 * everything the writer did before the unlock happens-before everything the reader does after the lock
 * (JLS 17.4.5). If the reader's lock comes BEFORE the writer's lock, it simply sees {@code ready == false}
 * and polls again; it never sees a half-published state, because the writer's two writes are inside one
 * critical section.
 *
 * <p>Correctness condition: ALL accesses to {@code data} and {@code ready} must use the same lock.
 * One unsynchronized reader reintroduces the data race.
 */
public final class SynchronizedBox implements SharedBox {
    private int data;
    private boolean ready;

    @Override
    public synchronized void write(int value) {
        data = value;
        ready = true;
    }

    /** Returns the payload if published, otherwise NOT_SEEN. Locks the monitor on every call. */
    private synchronized int pollOnce() {
        return ready ? data : NOT_SEEN;
    }

    @Override
    public int tryRead(int maxSpins) {
        for (int i = 0; i < maxSpins; i++) {
            int v = pollOnce();
            if (v != NOT_SEEN) {
                return v;
            }
            Thread.onSpinWait();
        }
        return NOT_SEEN;
    }

    @Override
    public String name() {
        return "SynchronizedBox (safe: monitor unlock/lock edge)";
    }
}
