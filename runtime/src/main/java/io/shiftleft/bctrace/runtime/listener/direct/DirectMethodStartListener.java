package io.shiftleft.bctrace.runtime.listener.direct;

/**
 * Invoked each time the instrumented methods associated with it are executed, and before any of the
 * instructions in that method.
 * <br><br>
 * These method listeners can also change the value of an argument, see {@link
 * #getMutableArgumentIndex()} for more details.
 * <br><br>
 * Extending classes must define a <code>@ListenerMethod</code> as follows:
 * <br>
 * <ul>
 * <li> Return type: <code>void</code>(default) or the type of i-th argument to modify </li>
 * <li> Return value: the value to be passed as i-th argument</li>
 * <li> Arguments:  <code>(Class callerClass, Object instance, ${target_method_arguments})</code></li>
 * </ul>
 * <br>
 * If an exception is raised by the listener method, the bctrace runtime will log that exception and
 * will return the original value, so no undesired side-effects are introduced in the target
 * application
 */
public abstract class DirectMethodStartListener extends DirectMethodListener implements
    MutableArgumentsListener {

  /**
   * Returns the index of the instrumented method argument (0-based) this listener will change its
   * value. If overwritten, the listener method return type must match that argument type,
   * otherwise, it returns -1, meaning that the listener does not change any argument value
   */
  public int getMutableArgumentIndex() {
    return -1; // none by default
  }
}
