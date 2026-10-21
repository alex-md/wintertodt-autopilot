package com.wintertodtautopilot;

import lombok.Value;

/** Facts captured by the selected decision branch, not inferred from the displayed phase. */
@Value
public class SoloAdvice
{
    public enum Reason { OPENING, RECOVERY, REMOTE_FIRE, GOAL_FEED, FINISH, ON_THE_WAY, LOCAL_MAINTENANCE }
    Reason reason;
    int pointGoal;
    int earnedPoints;
    int energy;
    int litBraziers;
    boolean manualFinish;
    WorkingCorner deferredDestination;
}
