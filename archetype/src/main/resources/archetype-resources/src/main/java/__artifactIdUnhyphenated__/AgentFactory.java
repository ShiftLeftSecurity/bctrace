package ${package}.${artifactIdUnhyphenated};

public class AgentFactory implements io.shiftleft.bctrace.AgentFactory {

  @Override
  public io.shiftleft.bctrace.Agent createAgent() {
    return Agent.getInstance();
  }

  @Override
  public io.shiftleft.bctrace.AgentHelp createHelp() {
    return new AgentHelp();
  }
}
