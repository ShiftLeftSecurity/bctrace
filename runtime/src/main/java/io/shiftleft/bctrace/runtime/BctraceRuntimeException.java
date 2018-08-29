package io.shiftleft.bctrace.runtime;


public class BctraceRuntimeException extends RuntimeException {

  private RuntimeException rex;

  public BctraceRuntimeException(RuntimeException rex) {
    super(rex);
    this.rex = rex;
  }

  public RuntimeException getWrappedException() {
    return rex;
  }
}
