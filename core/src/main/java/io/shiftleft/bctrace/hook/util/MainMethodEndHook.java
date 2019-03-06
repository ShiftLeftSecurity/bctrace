package io.shiftleft.bctrace.hook.util;

import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.filter.Filter;
import io.shiftleft.bctrace.hierarchy.BctraceClass;
import io.shiftleft.bctrace.hook.GenericHook;
import io.shiftleft.bctrace.runtime.listener.generic.FinishListener;
import java.security.ProtectionDomain;
import org.objectweb.asm.tree.MethodNode;

public abstract class MainMethodEndHook extends GenericHook<Filter, FinishListener> {

  private volatile boolean active = true;
  private final Filter filter = new Filter() {
    @Override
    public boolean instrumentClass(String className, ProtectionDomain protectionDomain,
        ClassLoader cl) {
      return active;
    }

    @Override
    public boolean instrumentMethod(BctraceClass clazz, MethodNode mn) {
      return ASMUtils.isStatic(mn.access) && ASMUtils.isPublic(mn.access) && mn.name.equals("util")
          && mn.desc.equals("([Ljava/lang/String;)V");
    }
  };

  private final FinishListener listener = new FinishListener() {
    @Override
    public Object onFinish(int methodId, Class clazz, Object instance, Object[] args, Object ret,
        Throwable th) {
      if (active) {
        onMainReturn(clazz.getName());
        active = false;
      }
      return ret;
    }
  };

  @Override
  public final Filter getFilter() {
    return filter;
  }

  @Override
  public final FinishListener getListener() {
    return listener;
  }

  protected abstract void onMainReturn(String className);
}
