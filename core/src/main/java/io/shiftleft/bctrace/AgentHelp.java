package io.shiftleft.bctrace;

public interface AgentHelp {

  /**
   * This String will be printed to stderr when the agent jar is directly executed as java -jar
   */
  public String getHelp();
}
