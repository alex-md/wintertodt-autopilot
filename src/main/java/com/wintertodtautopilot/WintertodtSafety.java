package com.wintertodtautopilot;

/** Shared safety priority for both planners. */
final class WintertodtSafety
{
    private WintertodtSafety() { }
    static WintertodtObjective objective(WintertodtSnapshot s, PlannerSettings settings)
    {
        if (s.getRoundState() == RoundState.OUTSIDE || s.getRoundState() == RoundState.LOBBY
            || s.getWarmth() < 0 || s.getWarmth() >= 100 || s.getWarmth() > settings.getRestoreWarmth()) { return null; }
        boolean emergency = s.getWarmth() <= settings.getEmergencyWarmth();
        if (!s.isInventoryKnown())
        {
            return new WintertodtObjective(ObjectiveType.RESTORE_WARMTH,
                "LOW WARMTH - check supplies or move to the safe lobby", "warmth_supplies_unknown", TargetKind.DOOR, 0, true);
        }
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

    static WintertodtObjective missingSupplies(WintertodtSnapshot s)
    {
        if (s.getRoundState() == RoundState.OUTSIDE || s.getRoundState() == RoundState.LOBBY
            || s.isInventoryKnown() && s.isEquipmentKnown()) { return null; }
        return new WintertodtObjective(ObjectiveType.WAIT, "Waiting for inventory and equipment observations",
            "supplies_unobserved", TargetKind.NONE, 0, false);
    }
}
