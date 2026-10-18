package com.wintertodtautopilot;

interface WintertodtPlanner
{
    WintertodtObjective decide(WintertodtSnapshot snapshot, PlannerSettings settings);
    int getPreparationDoseTarget();
    void reset();
}
