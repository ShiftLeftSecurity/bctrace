/*
 * ShiftLeft, Inc. CONFIDENTIAL
 * Unpublished Copyright (c) 2017 ShiftLeft, Inc., All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property of ShiftLeft, Inc.
 * The intellectual and technical concepts contained herein are proprietary to ShiftLeft, Inc.
 * and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this
 * material is strictly forbidden unless prior written permission is obtained
 * from ShiftLeft, Inc. Access to the source code contained herein is hereby forbidden to
 * anyone except current ShiftLeft, Inc. employees, managers or contractors who have executed
 * Confidentiality and Non-disclosure agreements explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication or disclosure
 * of this source code, which includeas information that is confidential and/or proprietary, and
 * is a trade secret, of ShiftLeft, Inc.
 *
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY
 * OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF ShiftLeft, Inc.
 * IS STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT
 * CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS
 * CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package io.shiftleft.bctrace;

import io.shiftleft.bctrace.runtime.FrameData;
import io.shiftleft.bctrace.spi.Filter;
import io.shiftleft.bctrace.spi.Hook;
import io.shiftleft.bctrace.spi.Listener;
import io.shiftleft.bctrace.spi.impl.AllFilter;
import io.shiftleft.bctrace.spi.impl.VoidListener;
import org.junit.Test;

/**
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class PerformanceTest extends BcTraceTest {

    private static final Hook[] HOOKS = new Hook[]{new Hook() {

        @Override
        public Filter getFilter() {
            return new AllFilter();
        }

        @Override
        public Listener getListener() {
            return new VoidListener() {
                @Override
                public Object onStart(FrameData fd) {
                    if (((Long) fd.args[0]) % 2 == 0) {
                        System.nanoTime();
                    }
                    return null;
                }
            };
        }
    }};

    @Test
    public void testMinimimOverheadPrimitive() throws Exception {
        int stackDepth = 2000;
        int times = 10000;

        long nano = System.nanoTime();
        for (int i = 0; i < times; i++) {
            TestClass.fact(stackDepth);
        }
        long normalElapse = (System.nanoTime() - nano) / times;

        Class clazz = getInstrumentClass(TestClass.class, HOOKS);
        nano = System.nanoTime();
        for (int i = 0; i < times; i++) {
            clazz.getMethod("fact", long.class).invoke(null, stackDepth);
        }
        long instrumentedElapse = (System.nanoTime() - nano) / times;

        System.out.println("Normal (primitive): " + normalElapse / 1e6 + " ms");
        System.out.println("Instrumented (primitive): " + instrumentedElapse / 1e6 + " ms");
    }

    @Test
    public void testMinimimOverheadWrapper() throws Exception {
        long stackDepth = 2000;
        int times = 10000;

        long nano = System.nanoTime();
        for (int i = 0; i < times; i++) {
            TestClass.factWrapper(stackDepth);
        }
        long normalElapse = (System.nanoTime() - nano) / times;

        Class clazz = getInstrumentClass(TestClass.class, HOOKS);
        nano = System.nanoTime();
        for (int i = 0; i < times; i++) {
            clazz.getMethod("factWrapper", Long.class).invoke(null, stackDepth);
        }
        long instrumentedElapse = (System.nanoTime() - nano) / times;

        System.out.println("Normal (wrapper): " + normalElapse / 1e6 + " ms");
        System.out.println("Instrumented (wrapper): " + instrumentedElapse / 1e6 + " ms");

    }
}
