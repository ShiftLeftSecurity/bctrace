package io.shiftleft.bctrace.runtime.listener.generic;

import io.shiftleft.bctrace.runtime.listener.Listener;

public abstract class GenericListener implements Listener {


  GenericListener() {
  }

  /**
   * Declares if arguments must to be passed or not in onStart() notifications. Default
   * implementation returns <code>true</code>.
   *
   * Override and return <code>false</code> to save
   * unnecessary arguments array creation and primitive boxing
   */
  public boolean requiresArguments() {
    return true;
  }
}
