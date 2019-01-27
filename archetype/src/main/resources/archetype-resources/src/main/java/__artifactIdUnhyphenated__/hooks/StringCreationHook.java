package ${package}.${artifactIdUnhyphenated}.hooks;

import io.shiftleft.bctrace.filter.Filter;
import io.shiftleft.bctrace.hierarchy.BctraceClass;
import io.shiftleft.bctrace.hook.generic.GenericHook;
import io.shiftleft.bctrace.runtime.listener.generic.FinishListener;
import java.security.ProtectionDomain;
import org.objectweb.asm.tree.MethodNode;

public class StringCreationHook extends GenericHook {
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
  public Object getListener() {
    return new FinishListener() {
      @Override
      public Object onFinish(int methodId, Class clazz, Object instance, Object[] args,
          Object ret,
          Throwable throwable) {
        System.out.println("Created String instance: \"" + instance + "\"");
        return ret;
      }
    };
  }
}
