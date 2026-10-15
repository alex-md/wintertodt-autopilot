package com.wintertodtautopilot;

/** Stable key/value trace for live observation runs; only formatted when debug is enabled. */
final class WintertodtObservationTrace
{
    private WintertodtObservationTrace() { }

    static String format(WintertodtSnapshot s, WintertodtObjective objective)
    {
        return format(s, objective, WintertodtMode.MASS, null, "UNKNOWN", "none", false);
    }
    static String format(WintertodtSnapshot s, WintertodtObjective objective, WintertodtMode mode,
        SoloPhase phase, String trend, String transitionReason, boolean finishRequested)
    {
        StringBuilder trace = new StringBuilder("solo_observation_v1");
        trace.append(" tick=").append(s.getObservationTick())
            .append(" planner=").append(mode.name()).append(" soloPhase=").append(phase == null ? "NOT_RUNNING" : phase.name())
            .append(" round=").append(s.getRoundState())
            .append(" trend=").append(trend).append(" transitionReason=").append(transitionReason)
            .append(" finishRequested=").append(finishRequested)
            .append(" energy=").append(s.getWintertodtEnergy())
            .append(" warmth=").append(s.getWarmth()).append(" rawWarmth=").append(s.getRawWarmth())
            .append(" home=").append(s.getWorkingCorner().name())
            .append(" destination=").append(destination(s, objective))
            .append(" action=").append(objective.getType()).append(" reason=").append(objective.getReason())
            .append(" activity=").append(s.getCurrentActivity())
            .append(" doses=").append(s.getRejuvenationDoses())
            .append(" unfinished=").append(s.getUnfinishedPotionCount()).append(" herbs=").append(s.getBrumaHerbCount())
            .append(" freeSlots=").append(s.getFreeSlots()).append(" runEnergy=").append(s.getRunEnergyPercent())
            .append(" earned=").append(s.getPoints()).append(" carried=").append(s.inventoryPoints())
            .append(" roots=").append(s.getBrumaRoots()).append(" kindling=").append(s.getBrumaKindling())
            .append(" loadedLit=").append(s.getLitBraziers())
            .append(" observedGlobalLit=").append(s.observedGlobalLitBraziers());
        for (CornerObservation c : s.getCorners())
        {
            String prefix = " " + c.getCorner().name() + ".";
            trace.append(prefix).append("brazier=").append(c.getBrazierState())
                .append(prefix).append("brazierSource=").append(c.getBrazierSource())
                .append(prefix).append("brazierVisible=").append(c.isBrazierVisible())
                .append(prefix).append("brazierTick=").append(c.getBrazierObservedTick())
                .append(prefix).append("brazierFresh=").append(c.brazierFresh(s.getObservationTick()))
                .append(prefix).append("pyromancer=").append(c.getPyromancerState())
                .append(prefix).append("healthPercent=").append(c.getPyromancerHealthPercent())
                .append(prefix).append("pyromancerSource=").append(c.getPyromancerSource())
                .append(prefix).append("pyromancerVisible=").append(c.isPyromancerVisible())
                .append(prefix).append("pyromancerTick=").append(c.getPyromancerObservedTick())
                .append(prefix).append("pyromancerFresh=").append(c.pyromancerFresh(s.getObservationTick()));
        }
        return trace.toString();
    }

    private static String destination(WintertodtSnapshot s, WintertodtObjective objective)
    {
        switch (objective.getTarget())
        {
            case BRAZIER:
            case PYROMANCER:
            case ROOTS: return (objective.getDestination() == WorkingCorner.AUTOMATIC
                ? s.getWorkingCorner() : objective.getDestination()).name();
            default: return "NONE";
        }
    }
}
