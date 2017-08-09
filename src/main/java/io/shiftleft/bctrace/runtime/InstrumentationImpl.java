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
package io.shiftleft.bctrace.runtime;

import io.shiftleft.bctrace.spi.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Single implementation of the {@link Instrumentation Instrumentation}
 * interface.
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public final class InstrumentationImpl implements Instrumentation {

    private final java.lang.instrument.Instrumentation javaInstrumentation;
    private final Set<String> transformedClassNames = new HashSet<String>();

    private boolean stale = true;
    private Class[] transformedClasses;

    public InstrumentationImpl(java.lang.instrument.Instrumentation javaInstrumentation) {
        this.javaInstrumentation = javaInstrumentation;
    }

    @Override
    public boolean isRetransformClassesSupported() {
        return javaInstrumentation.isRetransformClassesSupported();
    }

    @Override
    public boolean isModifiableClass(Class<?> clazz) {
        return javaInstrumentation.isModifiableClass(clazz);
    }

    @Override
    public Class[] getTransformedClasses() {
        if (stale) {
            List<Class> list = new LinkedList<Class>();
            Class[] loaded = getAllLoadedClasses();
            for (Class clazz : loaded) {
                if (transformedClassNames.contains(clazz.getName())) {
                    list.add(clazz);
                }
            }
            transformedClasses = list.toArray(new Class[list.size()]);
            stale = false;
        }
        return transformedClasses;
    }

    @Override
    public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {
        if (classes != null) {
            javaInstrumentation.retransformClasses(classes);
        }
    }

    @Override
    public Class[] getAllLoadedClasses() {
        return javaInstrumentation.getAllLoadedClasses();
    }

    public void removeTransformedClass(String className) {
        transformedClassNames.remove(className);
        stale = true;
    }

    public void addTransformedClass(String className) {
        transformedClassNames.add(className);
        stale = true;
    }
}
