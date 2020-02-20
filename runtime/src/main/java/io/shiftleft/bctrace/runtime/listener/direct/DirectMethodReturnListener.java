package io.shiftleft.bctrace.runtime.listener.direct;

/**
 * Invoked each time the instrumented methods associated with it are about to return a value (passed
 * as an argument to the listener method),
 * <br><br>
 * These method listeners are able to change the value to be returned by the instrumented method,
 * just by returning that value themselves.
 * <br><br>
 * Extending classes must define a <code>@ListenerMethod</code> as follows:
 * <br>
 * <ul>
 * <li> Return type: ${target_method_return_type}, return type of the called method.
 * <li> Return value: the value to be effectively used in the execution</li>
 * <li> Arguments:  <code>(Class callerClass, Object instance, ${target_method_arguments},
 * ${target_method_return_type} originalReturnedValue)</code></li>
 * </ul>
 * <br>
 * If an exception is raised by the listener method, the bctrace runtime will log that exception and
 * will return the original value, so no undesired side-effects are introduced in the target
 * application
 */
public abstract class DirectMethodReturnListener extends DirectMethodListener {

}
