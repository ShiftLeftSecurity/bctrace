package io.shiftleft.bctrace.runtime;

public class CallbackEnabled {

  public static final ThreadLocal<Boolean> NOTIFY_DISABLED_FLAG = new ThreadLocal<Boolean>();
  public static final ThreadLocal<Boolean> NOTIFYING_FLAG = new ThreadLocal<Boolean>();

  @SuppressWarnings("BoxedValueEquality")
  public static boolean isThreadNotificationEnabled() {
    return NOTIFY_DISABLED_FLAG.get() != Boolean.TRUE;
  }

  @SuppressWarnings("BoxedValueEquality")
  public static void enableThreadNotification() {
    NOTIFY_DISABLED_FLAG.set(Boolean.FALSE);
  }

  @SuppressWarnings("BoxedValueEquality")
  public static void disableThreadNotification() {
    NOTIFY_DISABLED_FLAG.set(Boolean.TRUE);
  }

}
