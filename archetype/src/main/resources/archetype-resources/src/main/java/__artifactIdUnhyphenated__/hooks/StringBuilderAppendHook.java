package ${package}.${artifactIdUnhyphenated}.hooks;

import io.shiftleft.bctrace.Bctrace;
import io.shiftleft.bctrace.filter.MethodFilter.DirectMethodFilter;
import io.shiftleft.bctrace.hook.DirectMethodHook;
import io.shiftleft.bctrace.logging.Level;
import io.shiftleft.bctrace.runtime.listener.direct.DirectMethodStartListener;

public class StringBuilderAppendHook extends DirectMethodHook {

  public StringBuilderAppendHook() {
    super(
        new DirectMethodFilter(
            "java/lang/StringBuilder",
            "append",
            "(Ljava/lang/String;)Ljava/lang/StringBuilder;"),
        new DirectMethodStartListener() {
          @ListenerMethod
          public void append(Class clazz, Object instance, String str) {
            Bctrace.getAgentLogger().log(Level.INFO, "Appending \"" + instance + "\" + \"" + str + "\"");
          }
        }
    );
  }
}
