package io.shiftleft.bctrace.runtime.listener.direct;

/**
 * Invoked after an instrumented call site has been successfully executed.
 * <br><br>
 * These method listeners are able to change the returned value just by returning a new value
 * different than the original one (passed as an argument).
 * <br><br>
 * Extending classes must define a <code>@ListenerMethod</code> as follows:
 * <br>
 * <ul>
 * <li> Return type: ${call_site_return_type}, return type of the called method.
 * <li> Return value: the value to be effectively used in the execution</li>
 * <li> Arguments:  <code>(Class callerClass, Object caller, Object callee,
 * ${call_site_arguments}, ${call_site_return_type} originalReturnedValue)</code></li>
 * </ul>
 * <br>
 * If an exception is raised by the listener method, the bctrace runtime will log that exception and
 * will return the original value, so no undesired side-effects are introduced in the target
 * application
 */
public abstract class DirectCallSiteReturnListener extends DirectCallSiteListener {

}
