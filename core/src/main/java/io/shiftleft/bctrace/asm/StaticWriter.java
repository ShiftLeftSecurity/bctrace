/**
 * ASM tests Copyright (c) 2002-2005 France Telecom All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 1. Redistributions of source code must retain the
 * above copyright notice, this list of conditions and the following disclaimer. 2. Redistributions
 * in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
// Portions Copyright 2011 Google, Inc.
//
// This is an extracted version of the ClassInfo and ClassWriter
// portions of ClassWriterComputeFramesTest in the set of ASM tests.
// We have done a fair bit of rewriting for readability, and changed
// the comments.  The original author is Eric Bruneton.
package io.shiftleft.bctrace.asm;

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.spi.hierarchy.HierarchyClassInfo;
import java.io.IOException;
import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * A {@link ClassWriter} that looks for static class data in the classpath when
 * the classes are not available at runtime, using the HierarchyClassInfo tree.
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
class StaticClassWriter extends ClassWriter {

  /* The classloader that we use to look for the unloaded class */
  private final ClassLoader classLoader;

  /**
   * {@inheritDoc}
   *
   * @param classLoader the class loader that loaded this class
   */
  public StaticClassWriter(
      ClassReader classReader, int flags, ClassLoader classLoader) {
    super(classReader, flags);
    this.classLoader = classLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getCommonSuperClass(
      final String type1, final String type2) {
    HierarchyClassInfo ci1 = HierarchyClassInfo.from(type1.replace('/', '.'), classLoader);
    HierarchyClassInfo ci2 = HierarchyClassInfo.from(type2.replace('/', '.'), classLoader);
    if (ci1.isAssignableFrom(ci2)) {
      return type1;
    }
    if (ci2.isAssignableFrom(ci1)) {
      return type2;
    }
    if (ci1.isInterface() || ci2.isInterface()) {
      return "java/lang/Object";
    }

    do {
      // Should never be null, because if ci1 were the Object class
      // or an interface, it would have been caught above.
      ci1 = ci1.getSuperClass();
    } while (!ci1.isAssignableFrom(ci2));
    return ci1.getJVMName();
  }
}
