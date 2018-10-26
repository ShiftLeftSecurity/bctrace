package io.shiftleft.bctrace.runtime.listener.generic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Disables a listener parameter. This saves adding bytecode to the target method to report this
 * argument at runtime
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Disabled {

}
