package com.wintertodtautopilot;

import lombok.Builder;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/** One immutable observation. -1 denotes an unavailable numeric server/widget signal. */
@Value
@Builder(toBuilder = true)
public class WintertodtSnapshot
{
    @Builder.Default RoundState roundState = RoundState.OUTSIDE;
    @Builder.Default int wintertodtEnergy = -1;
    @Builder.Default int warmth = -1;
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
    @Builder.Default String interaction = "None";
    public int inventoryPoints() { return brumaRoots * WintertodtConstants.ROOT_POINTS + brumaKindling * WintertodtConstants.KINDLING_POINTS; }
    public int potentialPointsIfFletched() { return (brumaRoots + brumaKindling) * WintertodtConstants.KINDLING_POINTS; }
    public int projectedPoints() { return points < 0 ? -1 : points + inventoryPoints(); }
    public boolean rewardThresholdReached() { return points >= 500; }
    public boolean active() { return roundState == RoundState.ACTIVE || roundState == RoundState.ENDING; }
}
