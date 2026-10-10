package atomics.lockfree;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unbounded multi-producer/multi-consumer LIFO stack (Treiber, 1986).
 *
 * Progress: lock-free (system-wide progress), NOT wait-free. A failed CAS means another
 * thread's CAS on {@code top} succeeded in between. An individual thread may retry forever.
 * Assumptions: allocation and GC do not block indefinitely; nodes are never recycled (see ABA note).
 *
 * Linearization points:
 *  push            -> the successful compareAndSet(h, n)
 *  pop (non-empty) -> the successful compareAndSet(h, h.next)
 *  pop (empty)     -> the top.get() that returned null
 *
 * Visibility: Node fields are final and immutable (JLS 17.5), and the node is published by a
 * successful CAS, which has volatile read+write memory effects (java.util.concurrent.atomic docs).
 *
 * ABA: a fresh Node is allocated on every push and never reused. With GC, a Node a thread still
 * references cannot be reallocated, so the address-reuse ABA cannot occur. If you add a node pool,
 * this argument no longer holds (needs tagged/stamped references).
 */
public final class TreiberStack<E> {

    private static final class Node<E> {
        final E item;
        final Node<E> next;
        Node(E item, Node<E> next) { this.item = item; this.next = next; }
    }

    private final AtomicReference<Node<E>> top = new AtomicReference<>();

    public void push(E item) {
        Objects.requireNonNull(item, "null items not allowed");
        Node<E> head;
        Node<E> node;
        do {
            head = top.get();
            node = new Node<>(item, head);   // allocation inside retry loop (limits any "no allocation" claim)
        } while (!top.compareAndSet(head, node));
    }

    /** @return popped item, or null if the stack was empty at the linearization point. */
    public E pop() {
        Node<E> head;
        do {
            head = top.get();
            if (head == null) return null;
        } while (!top.compareAndSet(head, head.next));
        return head.item;
    }

    public E peek() { Node<E> h = top.get(); return h == null ? null : h.item; }

    /** Weakly consistent snapshot; may be stale immediately. */
    public boolean isEmpty() { return top.get() == null; }
}
