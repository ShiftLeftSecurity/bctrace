package io.shiftleft.bctrace.hook.util;

import io.shiftleft.bctrace.asm.util.ASMUtils;
import io.shiftleft.bctrace.filter.Filter;
import io.shiftleft.bctrace.hierarchy.BctraceClass;
import io.shiftleft.bctrace.hook.GenericHook;
import io.shiftleft.bctrace.runtime.listener.generic.StartListener;
import java.security.ProtectionDomain;
import org.objectweb.asm.tree.MethodNode;

public abstract class MainMethodStartHook extends GenericHook<Filter, StartListener> {

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

  private final StartListener listener = new StartListener() {
    @Override
    public synchronized void onStart(int methodId, Class clazz, Object instance, Object[] args) {
      if (active) {
        onMainStart(clazz.getName(), (String[]) args[0]);
        active = false;
      }
    }
  };

  @Override
  public final Filter getFilter() {
    return filter;
  }

  @Override
  public final StartListener getListener() {
    return listener;
  }

  protected abstract void onMainStart(String className, String[] args);
}
