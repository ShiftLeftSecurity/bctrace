package io.shiftleft.bctrace.runtime.listener.direct;

/**
 * Notified before an instrumented class site is invoked.
 * <br><br>
 * These class site listeners can also change the value of an argument to be passed to the call
 * site, see {@link #getMutableArgumentIndex()} for more details.
 * <br><br>
 * Extending classes must define a <code>@ListenerMethod</code> as follows:
 * <br>
 * <ul>
 * <li> Return type: <code>void</code>(default) or the type of i-th argument to modify </li>
 * <li> Return value: the value to be passed as i-th argument</li>
 * <li> Arguments:  <code>(Class callerClass, Object caller, Object callee,
 * ${call_site_arguments})</code></li>
 * </ul>
 * <br>
 * If an exception is raised by the listener method, the bctrace runtime will log that exception and
 * will return the original value, so no undesired side-effects are introduced in the target
 * application
 */
public abstract class DirectCallSiteStartListener extends DirectCallSiteListener implements
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
