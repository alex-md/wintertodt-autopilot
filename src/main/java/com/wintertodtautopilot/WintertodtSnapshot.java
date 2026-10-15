package com.wintertodtautopilot;

import lombok.Builder;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/** One immutable observation. -1 denotes an unavailable numeric server/widget signal. */
@Value
@Builder(toBuilder = true)
public class WintertodtSnapshot
{
    @Builder.Default int observationTick = -1;
    @lombok.Singular private java.util.List<CornerObservation> corners;
    @Builder.Default RoundState roundState = RoundState.OUTSIDE;
    @Builder.Default int wintertodtEnergy = -1;
    @Builder.Default int warmth = -1;
    @Builder.Default int rawWarmth = -1;
    @Builder.Default int points = -1;
    @Builder.Default int secondsToStart = -1;
    int brumaRoots;
    int brumaKindling;
    @Builder.Default int firemakingLevel = 50;
    @Builder.Default int warmClothingCount = -1;
    int foodCount;
    int rejuvenationDoses;
    int unfinishedPotionCount;
    int brumaHerbCount;
    int freeSlots;
    @Builder.Default int runEnergyPercent = -1;
    @Builder.Default int warmthItem = -1;
    @Builder.Default int potionItem = -1;
    boolean hasKnife;
    boolean hasHammer;
    boolean hasTinderbox;
    boolean hasBrumaTorch;
    boolean hasAxe;
    boolean canMixPotion;
    boolean localPyromancerKnown;
    boolean localPyromancerIncapacitated;
    @Builder.Default BrazierState localBrazierState = BrazierState.UNKNOWN;
    @Builder.Default PlayerActivity currentActivity = PlayerActivity.IDLE;
    boolean playerIdle;
    boolean interrupted;
    int inactiveTicks;
    WorldPoint playerLocation;
    @Builder.Default WorkingCorner workingCorner = WorkingCorner.AUTOMATIC;
    @Builder.Default EndUrgency endUrgency = EndUrgency.NORMAL;
    int litBraziers;
    @Builder.Default int estimatedSecondsLeft = -1;
    @Builder.Default int roundBudgetSeconds = -1;
    /** Walking tile distance to a usable adjacent tile; -1 if not observed. */
    @Builder.Default int brazierDistance = -1;
    @Builder.Default String interaction = "None";
    /** -1 means incomplete evidence; a loaded-object count is not global coverage. */
    public int observedGlobalLitBraziers()
    {
        java.util.EnumSet<WorkingCorner> seen = java.util.EnumSet.noneOf(WorkingCorner.class);
        int lit = 0;
        for (CornerObservation c : corners)
        {
            if (c.getCorner() == WorkingCorner.AUTOMATIC || !seen.add(c.getCorner())
                || c.getBrazierSource() == CornerObservation.Source.NONE || !c.brazierFresh(observationTick)
                || c.getBrazierState() == BrazierState.UNKNOWN || c.getBrazierState() == BrazierState.UNAVAILABLE)
            { return -1; }
            if (c.getBrazierState() == BrazierState.LIT) { ++lit; }
        }
        return seen.size() == 4 ? lit : -1;
    }
    public CornerObservation corner(WorkingCorner corner)
    {
        for (CornerObservation c : corners) { if (c.getCorner() == corner) { return c; } }
        return null;
    }
    public int inventoryPoints() { return brumaRoots * WintertodtConstants.ROOT_POINTS + brumaKindling * WintertodtConstants.KINDLING_POINTS; }
    public int potentialPointsIfFletched() { return (brumaRoots + brumaKindling) * WintertodtConstants.KINDLING_POINTS; }
    public int projectedPoints() { return points < 0 ? -1 : points + inventoryPoints(); }
    public boolean rewardThresholdReached() { return points >= 500; }
    public boolean active() { return roundState == RoundState.ACTIVE || roundState == RoundState.ENDING; }
}
