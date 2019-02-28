package ${package}.${artifactIdUnhyphenated}.hooks;

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.filter.Filter;
import io.shiftleft.bctrace.hierarchy.BctraceClass;
import io.shiftleft.bctrace.hook.generic.GenericHook;
import io.shiftleft.bctrace.logging.Level;
import io.shiftleft.bctrace.runtime.listener.generic.FinishListener;
import java.security.ProtectionDomain;
import org.objectweb.asm.tree.MethodNode;

/**
 * A sample hook that logs a message every time a String constructor is invoked
 */
public class StringConstructorHook extends GenericHook<Filter, FinishListener> {

  @Override
  public Filter getFilter() {
    return new Filter() {
      @Override
      public boolean instrumentClass(String className, ProtectionDomain protectionDomain,
          ClassLoader cl) {
        return className.equals("java/lang/String");
      }

      @Override
      public boolean instrumentMethod(BctraceClass bctraceClass, MethodNode methodNode) {
        return methodNode.name.equals("<init>");
      }
    };
  }

  @Override
  public FinishListener getListener() {
    return new FinishListener() {
      @Override
      public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
          Object ret, Throwable throwable) {
        Bctrace.getAgentLogger().log(Level.INFO, "Created String instance: \"" + instance + "\"");
        return ret;
      }
    };
  }
}