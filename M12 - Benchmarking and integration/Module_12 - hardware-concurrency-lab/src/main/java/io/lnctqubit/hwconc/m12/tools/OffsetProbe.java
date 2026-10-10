package io.lnctqubit.hwconc.m12.tools;

import io.lnctqubit.hwconc.m11.PaddedSpscRingBuffer;
import io.lnctqubit.hwconc.m11.SpscRingBuffer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free field-offset probe (sun.misc.Unsafe.objectFieldOffset, jdk.unsupported module).
 * Reports the REAL offsets chosen by the running JVM, and whether the two hot volatile fields are
 * >= 64 bytes apart. Offsets are relative to the object start; object start alignment is only 8 bytes,
 * so only distance >= 64 bytes guarantees different cache lines. Does not prove false-sharing
 * behaviour (needs hardware counters). JOL (JolLayout) is the cross-check.
 */
public final class OffsetProbe {
    public static void main(String[] a) throws Exception {
        Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        f.setAccessible(true);
        sun.misc.Unsafe u = (sun.misc.Unsafe) f.get(null);
        System.out.println("java.vm=" + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version")
                + " os=" + System.getProperty("os.name") + " " + System.getProperty("os.arch")
                + " args=" + java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments());
        probe(u, SpscRingBuffer.class);
        probe(u, PaddedSpscRingBuffer.class);
    }

    @SuppressWarnings("deprecation")
    private static void probe(sun.misc.Unsafe u, Class<?> c) {
        List<Field> fs = new ArrayList<>();
        for (Field x : c.getDeclaredFields()) if (!Modifier.isStatic(x.getModifiers())) fs.add(x);
        fs.sort((x, y) -> Long.compare(u.objectFieldOffset(x), u.objectFieldOffset(y)));
        System.out.println("--- " + c.getSimpleName() + " (fields sorted by actual offset) ---");
        long prod = -1, cons = -1;
        for (Field x : fs) {
            long off = u.objectFieldOffset(x);
            System.out.printf("  off=%3d  %-8s %s%n", off, x.getType().getSimpleName(), x.getName());
            if (x.getName().equals("producerSequence")) prod = off;
            if (x.getName().equals("consumerSequence")) cons = off;
        }
        long d = Math.abs(prod - cons);
        System.out.println("  producerSequence<->consumerSequence distance = " + d + " bytes -> "
                + (d >= 64 ? ">=64: guaranteed different 64B lines (if line size is 64)"
                           : "<64: may share a cache line"));
    }
}
