package io.lnctqubit.hwconc.m12.tools;

import io.lnctqubit.hwconc.m11.PaddedSpscRingBuffer;
import io.lnctqubit.hwconc.m11.SpscRingBuffer;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.VM;

/** JOL cross-check of OffsetProbe. Output goes to results/m12/m12_jol_layout.txt. */
public final class JolLayout {
    public static void main(String[] a) {
        System.out.println(VM.current().details());
        System.out.println(ClassLayout.parseClass(SpscRingBuffer.class).toPrintable());
        System.out.println(ClassLayout.parseClass(PaddedSpscRingBuffer.class).toPrintable());
    }
}
