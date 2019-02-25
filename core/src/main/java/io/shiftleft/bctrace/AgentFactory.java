package io.shiftleft.bctrace;

public interface AgentFactory {

  public Agent createAgent();

  public AgentHelp createHelp();
}
