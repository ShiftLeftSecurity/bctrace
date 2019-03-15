package ${package}.${artifactIdUnhyphenated}.hooks;

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.filter.MethodFilter;
import io.shiftleft.bctrace.hook.direct.MethodHook;
import io.shiftleft.bctrace.logging.Level;
import io.shiftleft.bctrace.runtime.listener.direct.DirectListener;

public class StringBuilderAppendHook extends MethodHook {

  public StringBuilderAppendHook() {
    super(new MethodFilter(
            "java/lang/StringBuilder",
            "append",
            "(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
        new DirectListener() {
          @ListenerMethod(type = ListenerType.onStart)
          public void append(Class clazz, Object instance, String str) {
            Bctrace.getAgentLogger().log(Level.INFO, "Appending \"" + instance + "\" + \"" + str + "\"");
          }
        }
    );
  }
}
