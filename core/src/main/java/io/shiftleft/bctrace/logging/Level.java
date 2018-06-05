package io.shiftleft.bctrace.logging;

public enum Level {

  /**
   * Log everything. Useful for technical debugging. Not for use in production environments
   */
  TRACE(1),
  /**
   * Debugging level of logging. Not for use in production environments
   */
  DEBUG(2),
  /**
   * Reasonable informative level of logging. Returns information interesting for the user
   */
  INFO(3),
  /**
   * Log only warning and errors
   */
  WARNING(4),
  /**
   * Log only errors <code>ERROR = 5;</code>
   */
  ERROR(5),
  /**
   * Disable logging
   */
  QUIET(6);

  final int value;

  Level(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
