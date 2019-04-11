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
 * of this source code, which includes information that is confidential and/or proprietary, and
 * is a trade secret, of ShiftLeft, Inc.
 *
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY
 * OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF ShiftLeft, Inc.
 * IS STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT
 * CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS
 * CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package io.shiftleft.bctrace.asm.helper.generic.method;

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.MethodInfo;
import io.shiftleft.bctrace.MethodRegistry;
import io.shiftleft.bctrace.asm.helper.Helper;
import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.logging.Level;
import io.shiftleft.bctrace.runtime.listener.generic.GenericMethodThrowableListener;
import java.util.ArrayList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Inserts the bytecode instructions within method node, needed to handle the return listeners
 * registered.
 *
 * This helper turns the method node instructions of a method like this:
 * <br><pre>{@code
 * public Object foo(Object arg){
 *   return void(arg);
 * }
 * }
 * </pre>
 * Into that:
 * <br><pre>{@code
 * public Object foo(Object arg){
 *   try{
 *     Object ret = void(arg);
 *     // Notify listeners that apply to this method
 *     ret = Callback.onFinish(ret, ret, null, clazz, this, 0, arg);
 *     ret = Callback.onFinish(ret, ret, null, clazz, this, 2, arg);
 *     ret = Callback.onFinish(ret, ret, null, clazz, this, 10, arg);
 *     // Return the original value
 *     return ret;
 *   } catch (Throwable th){
 *     th = Callback.onFinish(th, null, th, clazz, this, 0, arg);
 *     th = Callback.onFinish(th, null, th, clazz, this, 2, arg);
 *     th = Callback.onFinish(th, null, th, clazz, this, 10, arg);
 *     throw th;
 *   }
 * }
 * </pre>
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class GenericMethodThrowableHelper extends Helper {


  public boolean addByteCodeInstructions(ClassNode cn, MethodNode mn,
      ArrayList<Integer> hooksToUse) {

    ArrayList<Integer> listenersToUse = getListenersOfType(hooksToUse,
        GenericMethodThrowableListener.class);

    if (!isInstrumentationNeeded(listenersToUse)) {
      return false;
    }

    addTryCatchInstructions(cn, mn, listenersToUse);
    return true;
  }

  private boolean addTryCatchInstructions(ClassNode cn, MethodNode mn,
      ArrayList<Integer> listenersToUse) {

    LabelNode startNode = getStartNodeForGlobalTryCatch(mn);
    if (startNode == null) {
      // Weird constructor, not generated from Java Language
      Bctrace.getAgentLogger().log(Level.WARNING,
          "Could not add try/catch handler to constructor " + cn.name + "." + mn.name + mn.desc);
      return false;
    }

    Integer methodId = MethodRegistry.getInstance().registerMethodId(MethodInfo.from(cn.name, mn));

    LabelNode endNode = new LabelNode();
    mn.instructions.add(endNode);

    InsnList il = new InsnList();
    Object[] topLocals = ASMUtils.getTopLocals(cn, mn);
    il.add(new FrameNode(Opcodes.F_FULL, topLocals.length, topLocals, 1,
        new Object[]{"java/lang/Throwable"}));

    LabelNode handlerNode = new LabelNode();
    il.add(handlerNode);

    for (int i = 0; i < listenersToUse.size(); i++) {
      Integer index = listenersToUse.get(i);
      GenericMethodThrowableListener listener = (GenericMethodThrowableListener) bctrace
          .getHooks()[index].getListener();
      il.add(new InsnNode(Opcodes.DUP)); //throwable
      il.add(ASMUtils.getPushInstruction(methodId)); // method id
      il.add(getClassConstantReference(Type.getObjectType(cn.name), cn.version));
      pushInstance(il, mn); // current instance
      il.add(ASMUtils.getPushInstruction(index)); // hook id
      if (listener.requiresArguments()) {
        pushMethodArgsArray(il, mn);
      } else {
        il.add(new InsnNode(Opcodes.ACONST_NULL));
      }
      il.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "io/shiftleft/bctrace/runtime/Callback", "onThrow",
          "(Ljava/lang/Throwable;ILjava/lang/Class;Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Throwable;",
          false));

    }

    il.add(new InsnNode(Opcodes.ATHROW));

    TryCatchBlockNode blockNode = new TryCatchBlockNode(startNode, endNode, handlerNode, null);

    mn.tryCatchBlocks.add(blockNode);
    mn.instructions.add(il);
    return true;
  }
}
