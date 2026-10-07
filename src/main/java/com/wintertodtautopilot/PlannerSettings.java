package com.wintertodtautopilot;

import lombok.Value;

@Value
public class PlannerSettings
{
    WintertodtStrategy strategy;
    PointGoal pointGoal;
    FletchMode fletchMode;
    int restoreWarmth;
    int emergencyWarmth;
    public static PlannerSettings from(WintertodtAutopilotConfig config)
    {
        int restore = Math.max(1, Math.min(100, config.restoreWarmth()));
        return new PlannerSettings(config.strategy(), config.pointGoal(), config.fletchMode(),
            restore, Math.max(1, Math.min(restore, config.emergencyWarmth())));
    }
    public boolean fletch(WintertodtSnapshot s)
    {
        return s.isHasKnife() && fletchMode != FletchMode.NEVER
            && (fletchMode == FletchMode.ALWAYS || strategy != WintertodtStrategy.MASS_XP);
    }
}
