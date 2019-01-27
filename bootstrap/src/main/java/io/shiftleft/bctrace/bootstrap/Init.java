package io.shiftleft.bctrace.bootstrap;

import java.lang.instrument.Instrumentation;

/**
 * Framework entry point.
 *
 * @author Ignacio del Valle Alles idelvall@shiftleft.io
 */
public class Init {

  private static final ClassLoader AGENT_CLASS_LOADER = new BctraceClassLoader();

  public static void premain(final String arg, Instrumentation inst) throws Exception {
    ClassLoader initialContextClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(AGENT_CLASS_LOADER);
    Class<?> initClass = AGENT_CLASS_LOADER.loadClass("io.shiftleft.bctrace.Init");
    initClass.getMethod("premain", String.class, Instrumentation.class).invoke(null, arg, inst);
    Thread.currentThread().setContextClassLoader(initialContextClassLoader);
  }

  public static void main(final String[] args) throws Exception {
    ClassLoader initialContextClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(AGENT_CLASS_LOADER);
    Class<?> initClass = AGENT_CLASS_LOADER.loadClass("io.shiftleft.bctrace.Init");
    initClass.getMethod("main", String[].class).invoke(null, new Object[]{args});
    Thread.currentThread().setContextClassLoader(initialContextClassLoader);
  }

}
