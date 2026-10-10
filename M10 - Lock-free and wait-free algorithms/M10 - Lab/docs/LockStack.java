package atomics.lockfree;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/** Blocking baseline with the same semantics as TreiberStack (LIFO, null on empty, no null items). */
public final class LockStack<E> {
    private final ReentrantLock lock = new ReentrantLock();
    private final ArrayDeque<E> deque = new ArrayDeque<>();

    public void push(E item) {
        Objects.requireNonNull(item);
        lock.lock();
        try { deque.addFirst(item); } finally { lock.unlock(); }
    }
    public E pop() {
        lock.lock();
        try { return deque.pollFirst(); } finally { lock.unlock(); }
    }
}
