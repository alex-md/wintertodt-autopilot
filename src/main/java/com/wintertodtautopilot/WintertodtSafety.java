package com.wintertodtautopilot;

/** Shared safety priority for both planners. */
final class WintertodtSafety
{
    private WintertodtSafety() { }
    static WintertodtObjective objective(WintertodtSnapshot s, PlannerSettings settings)
    {
        if (s.getRoundState() == RoundState.OUTSIDE || s.getRoundState() == RoundState.LOBBY
            || s.getWarmth() < 0 || s.getWarmth() >= 100 || s.getWarmth() > settings.getRestoreWarmth()
            || s.getCurrentActivity() == PlayerActivity.RESTORING_WARMTH) { return null; }
        boolean emergency = s.getWarmth() <= settings.getEmergencyWarmth();
        if (s.getWarmthItem() >= 0)
        {
            return new WintertodtObjective(ObjectiveType.RESTORE_WARMTH,
                (emergency ? "LOW WARMTH - " : "") + (WintertodtConstants.doses(s.getWarmthItem()) > 0
                    ? "Drink rejuvenation potion" : "Eat food to restore warmth"),
                "warmth_low", TargetKind.NONE, 1, emergency, s.getWarmthItem());
        }
        return new WintertodtObjective(ObjectiveType.RESTORE_WARMTH,
            "Move to the safe lobby - no warmth supplies", "no_warmth_supplies", TargetKind.DOOR, 0, true);
    }
}
