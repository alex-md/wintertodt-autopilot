package com.wintertodtautopilot;

import lombok.Value;

@Value
@lombok.AllArgsConstructor
public class PlannerSettings
{
    WintertodtStrategy strategy;
    PointGoal pointGoal;
    FletchMode fletchMode;
    int restoreWarmth;
    int emergencyWarmth;
    SoloEffort soloEffort;
    public PlannerSettings(WintertodtStrategy strategy, PointGoal goal, FletchMode fletch, int restore, int emergency)
    { this(strategy, goal, fletch, restore, emergency, SoloEffort.COMFORTABLE); }
    public SoloEffort getSoloEffort() { return soloEffort == null ? SoloEffort.COMFORTABLE : soloEffort; }
    public static PlannerSettings from(WintertodtAutopilotConfig config)
    {
        int restore = Math.max(1, Math.min(100, config.restoreWarmth()));
        WintertodtStrategy strategy = config.mode() == WintertodtMode.SOLO
            ? config.soloStrategy() == SoloStrategy.FIREMAKING_XP ? WintertodtStrategy.MASS_XP : WintertodtStrategy.MASS_POINTS
            : config.strategy();
        return new PlannerSettings(strategy, config.pointGoal(), config.fletchMode(),
            restore, Math.max(1, Math.min(restore, config.emergencyWarmth())), config.soloEffort());
    }
    public boolean fletch(WintertodtSnapshot s)
    {
        return s.isHasKnife() && fletchMode != FletchMode.NEVER
            && (fletchMode == FletchMode.ALWAYS || strategy != WintertodtStrategy.MASS_XP);
    }
}
