package io.lnctqubit.hwconc.m11.tools;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Minimal layout diagnostic for Module 11 ring buffer classes.
 *
 * <h3>Purpose</h3>
 * <p>Prints the declared fields of {@code SpscRingBuffer} and
 * {@code PaddedSpscRingBuffer} so that the Module 11 team can observe which
 * fields exist and, on HotSpot JVMs that expose it, inspect object sizes.</p>
 *
 * <h3>JOL dependency not added</h3>
 * <p>The Java Object Layout (JOL) library gives precise field offsets but
 * requires an additional Maven dependency. To keep {@code pom.xml} unchanged
 * and avoid dependency risk, this probe uses only the JDK's reflection API.
 * Reflection reports declared fields in source order in most JVM
 * implementations, but it does NOT guarantee physical offset order.
 * If exact offsets are required, add JOL as a {@code provided}-scope tool
 * dependency and run {@code ClassLayout.parseClass(...).toPrintable()}.</p>
 *
 * <h3>Status</h3>
 * <p>{@code PENDING LOCAL EXECUTION} — run on the team machine and save
 * output to {@code results/raw/m11_layout_probe.txt}. Record exact JDK
 * version, JVM flags (especially {@code -XX:+/-UseCompressedOops}) and OS.</p>
 *
 * <h3>Run</h3>
 * <pre>
 * # After mvn compile:
 * java -cp target\classes io.lnctqubit.hwconc.m11.tools.LayoutProbe ^
 *      > results\raw\m11_layout_probe.txt
 * </pre>
 */
public final class LayoutProbe {

    private LayoutProbe() {}

    public static void main(String[] args) {
        System.out.println("=== Module 11 Layout Probe ===");
        System.out.println("JVM: " + System.getProperty("java.vm.name")
                + " " + System.getProperty("java.vm.version"));
        System.out.println("JRE: " + System.getProperty("java.version"));
        System.out.println("OS:  " + System.getProperty("os.name")
                + " " + System.getProperty("os.arch"));
        System.out.println();
        System.out.println("NOTE: field offsets require JOL; only declared fields shown here.");
        System.out.println("Layout is JVM-implementation-specific; do not treat field order as");
        System.out.println("physical layout without running JOL with exact JVM flags.");
        System.out.println();

        printFields("SpscRingBuffer",
                io.lnctqubit.hwconc.m11.SpscRingBuffer.class);
        printFields("PaddedSpscRingBuffer",
                io.lnctqubit.hwconc.m11.PaddedSpscRingBuffer.class);
    }

    private static void printFields(String label, Class<?> clazz) {
        System.out.println("--- " + label + " declared fields ---");
        Field[] fields = clazz.getDeclaredFields();
        for (Field f : fields) {
            f.setAccessible(true);
            boolean isVolatile = Modifier.isVolatile(f.getModifiers());
            boolean isFinal   = Modifier.isFinal(f.getModifiers());
            System.out.printf("  %-30s  type=%-12s  volatile=%-5b  final=%b%n",
                    f.getName(),
                    f.getType().getSimpleName(),
                    isVolatile,
                    isFinal);
        }
        System.out.printf("  Total declared fields: %d%n%n", fields.length);
    }
}
