package io.shiftleft.bctrace;

public class TestSuperClass {
  private final int i;
  private final String s;

  public TestSuperClass() {
    this(5);
  }

  public TestSuperClass(int i) {
    this.i = i;
    this.s = null;
  }

  public TestSuperClass(String s) {
    this.i = 0;
    this.s = s;
  }
}
