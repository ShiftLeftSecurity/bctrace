package io.shiftleft.bctrace.runtime.listener.direct;

/**
 * Invoked each time a instrumented call site is about to throw a {@link Throwable} (passed as an
 * argument to the listener method).
 * <br><br>
 * These method listeners are able to change the <code>Throwable</code> instance to be thrown, just
 * by returning that instance themselves.
 * <br><br>
 * Extending classes must define a <code>@ListenerMethod</code> as follows:
 * <br>
 * <ul>
 * <li> Return type: <code>Throwable</code>
 * <li> Return value: the instance to be effectively thrown in the execution</li>
 * <li> Arguments:  <code>(Class callerClass, Object caller, Object callee,
 * ${target_call_site_arguments}, Throwable originallyThrown)</code></li>
 * </ul>
 * <br>
 * If an exception is raised by the listener method, the bctrace runtime will log that exception and
 * will return the original value, so no undesired side-effects are introduced in the target
 * application
 */
public abstract class DirectCallSiteThrowableListener extends DirectCallSiteListener {

}