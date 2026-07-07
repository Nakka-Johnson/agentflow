package com.nakka.agentflow.agent;

public interface Planner {

    Orchestrator.ToolPlan plan(String query);
}