package io.shiftleft.bctrace.hook;

import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.filter.Filter;
import io.shiftleft.bctrace.hierarchy.BctraceClass;
import io.shiftleft.bctrace.runtime.listener.generic.ReturnListener;
import java.security.ProtectionDomain;
import org.objectweb.asm.tree.MethodNode;

public abstract class MainMethodEndHook extends GenericHook<Filter, ReturnListener> {

  private volatile boolean active = true;
  private final Filter filter = new Filter() {
    @Override
    public boolean instrumentClass(String className, ProtectionDomain protectionDomain,
        ClassLoader cl) {
      return active;
    }

    @Override
    public boolean instrumentMethod(BctraceClass clazz, MethodNode mn) {
      return ASMUtils.isStatic(mn.access) && ASMUtils.isPublic(mn.access) && mn.name.equals("main")
          && mn.desc.equals("([Ljava/lang/String;)V");
    }
  };

  private final ReturnListener listener = new ReturnListener() {
    @Override
    public void onReturn(int methodId, Class clazz, Object instance, Object[] args, Object ret) {
      if (active) {
        onMainReturn();
        active = false;
      }
    }
  };

  @Override
  public final Filter getFilter() {
    return filter;
  }

  @Override
  public final ReturnListener getListener() {
    return listener;
  }

  protected abstract void onMainReturn();
}
