package com.wintertodtautopilot;

import net.runelite.api.gameval.ItemID;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtDecisionEngineTest
{
    private WintertodtDecisionEngine engine;
    private PlannerSettings settings;
    @Before public void setup()
    {
        engine = new WintertodtDecisionEngine();
        settings = new PlannerSettings(WintertodtStrategy.MASS_BALANCED, PointGoal.MINIMUM_500, FletchMode.AUTOMATIC, 35, 20);
    }
    private WintertodtSnapshot.WintertodtSnapshotBuilder active()
    {
        return WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).wintertodtEnergy(80).warmth(90).points(0)
            .localBrazierState(BrazierState.LIT).localPyromancerKnown(true)
            .firemakingLevel(99).warmClothingCount(4).rejuvenationDoses(4).potionItem(ItemID.WINT_POTION4)
            .hasHammer(true).hasKnife(true).hasTinderbox(true).hasAxe(true).freeSlots(24).workingCorner(WorkingCorner.SOUTHEAST);
    }
    private WintertodtObjective decide(WintertodtSnapshot.WintertodtSnapshotBuilder s) { return engine.decide(s.build(), settings); }
    @Test public void warmthPreemptsBrokenBrazier()
    {
        WintertodtObjective result = decide(active().warmth(18).warmthItem(ItemID.CAKE).localBrazierState(BrazierState.BROKEN));
        assertEquals(ObjectiveType.RESTORE_WARMTH, result.getType());
        assertTrue(result.isUrgent()); assertTrue(result.getItemIds().contains(ItemID.CAKE));
    }
    @Test public void noWarmthSuppliesRequiresSafeLobby()
    {
        WintertodtObjective result = decide(active().warmth(18));
        assertEquals(TargetKind.DOOR, result.getTarget()); assertTrue(result.isUrgent());
    }
    @Test public void consumingClearsWarning()
    {
        assertEquals(ObjectiveType.CHOP, decide(active().warmth(18).warmthItem(ItemID.CAKE)
            .currentActivity(PlayerActivity.RESTORING_WARMTH)).getType());
    }
    @Test public void brokenBrazierRequiresRepair()
    {
        assertEquals(ObjectiveType.REPAIR, decide(active().localBrazierState(BrazierState.BROKEN)).getType());
    }
    @Test public void missingHammerGuidesToCrate()
    {
        assertEquals(TargetKind.HAMMER_CRATE, decide(active().localBrazierState(BrazierState.BROKEN).hasHammer(false)).getTarget());
    }
    @Test public void imminentRewardsFeedBeforeChoppingOrFletching()
    {
        WintertodtObjective result = decide(active().points(450).brumaKindling(3).wintertodtEnergy(2).endUrgency(EndUrgency.IMMINENT));
        assertEquals(ObjectiveType.FEED, result.getType()); assertTrue(result.isUrgent());
    }
    @Test public void enoughKindlingDoesNotChop()
    {
        settings = new PlannerSettings(WintertodtStrategy.MASS_POINTS, PointGoal.POINTS_1000, FletchMode.AUTOMATIC, 35, 20);
        assertEquals(ObjectiveType.FEED, decide(active().points(700).brumaKindling(12)).getType());
    }
    @Test public void fletchingResumesAfterColdAndRepairAndWarmth()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().points(25).brumaRoots(19).freeSlots(5);
        assertEquals(ObjectiveType.FLETCH, decide(s).getType());
        assertEquals(ObjectiveType.FLETCH, decide(s.playerIdle(true).interrupted(true)).getType());
        assertEquals(ObjectiveType.REPAIR, decide(s.localBrazierState(BrazierState.BROKEN)).getType());
        assertEquals(ObjectiveType.FLETCH, decide(s.localBrazierState(BrazierState.LIT)).getType());
        assertEquals(ObjectiveType.RESTORE_WARMTH, decide(s.warmth(30).warmthItem(ItemID.CAKE)).getType());
        assertEquals(ObjectiveType.FLETCH, decide(s.warmth(65)).getType());
    }
    @Test public void downPyromancerBeforeRelight()
    {
        assertEquals(ObjectiveType.HEAL_PYROMANCER, decide(active().localBrazierState(BrazierState.UNLIT)
            .localPyromancerIncapacitated(true).rejuvenationDoses(2).potionItem(ItemID.WINT_POTION2)).getType());
    }
    @Test public void potionPreparationGuidesEachSubstep()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().rejuvenationDoses(0).localBrazierState(BrazierState.UNLIT).localPyromancerIncapacitated(true);
        assertEquals(TargetKind.POTION_CRATE, decide(s).getTarget());
        assertEquals(TargetKind.HERBS, decide(s.unfinishedPotionCount(1)).getTarget());
        assertEquals(TargetKind.NONE, decide(s.brumaHerbCount(1).canMixPotion(true)).getTarget());
        assertEquals(TargetKind.BREWMA, decide(s.canMixPotion(false)).getTarget());
    }
    @Test public void fullBagDoesNotRecommendUnobtainablePotion()
    {
        WintertodtObjective result = decide(active().localBrazierState(BrazierState.UNLIT)
            .localPyromancerIncapacitated(true).rejuvenationDoses(0).freeSlots(0));
        assertEquals(ObjectiveType.WAIT, result.getType());
    }
    @Test public void unknownPyromancerDoesNotRecommendLighting()
    {
        assertEquals(ObjectiveType.POSITION, decide(active().localPyromancerKnown(false).localBrazierState(BrazierState.UNLIT)).getType());
    }
    @Test public void missingTinderboxGuidesToCrateButTorchWorks()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().localBrazierState(BrazierState.UNLIT).hasTinderbox(false);
        assertEquals(TargetKind.TINDERBOX_CRATE, decide(s).getTarget());
        assertEquals(ObjectiveType.LIGHT, decide(s.hasBrumaTorch(true)).getType());
    }
    @Test public void xpStrategyFeedsRawRoots()
    {
        settings = new PlannerSettings(WintertodtStrategy.MASS_XP, PointGoal.MINIMUM_500, FletchMode.AUTOMATIC, 35, 20);
        assertEquals(ObjectiveType.FEED, decide(active().brumaRoots(24).freeSlots(0)).getType());
    }
    @Test public void neverFletchOverridesPointsStrategy()
    {
        settings = new PlannerSettings(WintertodtStrategy.MASS_POINTS, PointGoal.MINIMUM_500, FletchMode.NEVER, 35, 20);
        assertEquals(ObjectiveType.FEED, decide(active().brumaRoots(24).freeSlots(0)).getType());
    }
    @Test public void alwaysFletchesEvenIfRawRootsAlreadySecureGoal()
    {
        settings = new PlannerSettings(WintertodtStrategy.MASS_XP, PointGoal.MINIMUM_500, FletchMode.ALWAYS, 35, 20);
        WintertodtObjective result = decide(active().points(450).brumaRoots(10));
        assertEquals(ObjectiveType.FLETCH, result.getType()); assertEquals(10, result.getQuantity());
    }
    @Test public void maximumPracticalFletchesEntireBatch()
    {
        settings = new PlannerSettings(WintertodtStrategy.MASS_POINTS, PointGoal.MAXIMUM_PRACTICAL, FletchMode.AUTOMATIC, 35, 20);
        WintertodtObjective result = decide(active().points(450).brumaRoots(24).freeSlots(0));
        assertEquals(ObjectiveType.FLETCH, result.getType()); assertEquals(24, result.getQuantity());
    }
    @Test public void afkUsesFullBatch()
    {
        settings = new PlannerSettings(WintertodtStrategy.LOW_ATTENTION, PointGoal.MINIMUM_500, FletchMode.AUTOMATIC, 35, 20);
        assertEquals(24, decide(active()).getQuantity());
    }
    @Test public void choppingBatchDoesNotFlicker()
    {
        assertEquals(20, decide(active()).getQuantity());
        for (int i = 1; i < 20; i++)
        {
            WintertodtObjective result = decide(active().brumaRoots(i).freeSlots(24-i));
            assertEquals(ObjectiveType.CHOP, result.getType()); assertEquals(20-i, result.getQuantity());
        }
        assertEquals(ObjectiveType.FLETCH, decide(active().brumaRoots(20).freeSlots(4)).getType());
    }
    @Test public void balancedBatchProcessesEvenWhenGoalExceedsOneBatch()
    {
        settings = new PlannerSettings(WintertodtStrategy.MASS_BALANCED, PointGoal.POINTS_1000, FletchMode.AUTOMATIC, 35, 20);
        assertEquals(ObjectiveType.CHOP, decide(active()).getType());
        assertEquals(ObjectiveType.FLETCH, decide(active().brumaRoots(20).freeSlots(4)).getType());
    }
    @Test public void smartFletchStopsAtUsefulUpgradeCount()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().points(350).brumaRoots(10);
        WintertodtObjective result = decide(s);
        assertEquals(ObjectiveType.FLETCH, result.getType()); assertEquals(4, result.getQuantity());
        assertEquals(ObjectiveType.FEED, decide(s.brumaRoots(6).brumaKindling(4)).getType());
    }
    @Test public void feedingBatchPersistsAcrossPointThresholds()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().points(450).brumaKindling(4);
        assertEquals(ObjectiveType.FEED, decide(s).getType());
        assertEquals(ObjectiveType.FEED, decide(s.points(500).brumaKindling(2)).getType());
        assertEquals(ObjectiveType.CHOP, decide(s.points(550).brumaKindling(0)).getType());
    }
    @Test public void endingPreemptsExistingFletch()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().points(25).brumaRoots(19);
        assertEquals(ObjectiveType.FLETCH, decide(s).getType());
        assertEquals(ObjectiveType.FEED, decide(s.endUrgency(EndUrgency.IMMINENT)).getType());
    }
    @Test public void endingAvoidsFullBatch()
    {
        assertEquals(3, decide(active().points(450).endUrgency(EndUrgency.ENDING_SOON)).getQuantity());
    }
    @Test public void imminentAndSecuredWaitsWithoutInventory()
    {
        assertEquals(ObjectiveType.WAIT, decide(active().points(500).endUrgency(EndUrgency.IMMINENT)).getType());
    }
    @Test public void unavailableBrazierIsNotAssumedLit()
    {
        assertEquals(ObjectiveType.POSITION, decide(active().localBrazierState(BrazierState.UNKNOWN)).getType());
    }
    @Test public void roundTransitionsResetBatch()
    {
        assertEquals(ObjectiveType.CHOP, decide(active()).getType());
        assertEquals(ObjectiveType.ROUND_COMPLETE, decide(active().roundState(RoundState.COMPLETE)).getType());
        assertEquals(ObjectiveType.POSITION, decide(active().roundState(RoundState.COUNTDOWN).secondsToStart(8)).getType());
        engine.reset();
        assertEquals(ObjectiveType.FLETCH, decide(active().brumaRoots(20)).getType());
    }
    @Test public void pointsAndGoalsAreArithmeticNotGuaranteedRewards()
    {
        WintertodtSnapshot s = active().points(425).brumaRoots(4).brumaKindling(3).build();
        assertEquals(115, s.inventoryPoints()); assertEquals(175, s.potentialPointsIfFletched());
        assertEquals(540, s.projectedPoints()); assertEquals(1000, PointGoal.NEXT_THRESHOLD.target(500));
    }

    @Test public void entryPreparesPotionsBeforeChoppingEvenWithHealthyBrazier()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().rejuvenationDoses(0);
        assertEquals(TargetKind.POTION_CRATE, decide(s).getTarget());
        assertEquals(4, engine.getPreparationDoseTarget());
        assertEquals(TargetKind.HERBS, decide(s.unfinishedPotionCount(1)).getTarget());
        assertEquals(ObjectiveType.MAKE_REJUVENATION, decide(s.brumaHerbCount(1)).getType());
        assertEquals(ObjectiveType.CHOP, decide(s.rejuvenationDoses(4).unfinishedPotionCount(0).brumaHerbCount(0)).getType());
    }
    @Test public void requiredToolsAreCollectedBeforeRoots()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().hasAxe(false).hasHammer(false).hasKnife(false).hasTinderbox(false);
        assertEquals(TargetKind.AXE_CRATE, decide(s).getTarget());
        assertEquals(TargetKind.HAMMER_CRATE, decide(s.hasAxe(true)).getTarget());
        assertEquals(TargetKind.KNIFE_CRATE, decide(s.hasHammer(true)).getTarget());
        assertEquals(TargetKind.TINDERBOX_CRATE, decide(s.hasKnife(true)).getTarget());
        assertEquals(ObjectiveType.CHOP, decide(s.hasTinderbox(true)).getType());
    }
    @Test public void consumedDoseAndCornerChangeDoNotTriggerMidRoundResupply()
    {
        assertEquals(ObjectiveType.CHOP, decide(active()).getType());
        assertEquals(ObjectiveType.CHOP, decide(active().rejuvenationDoses(1).workingCorner(WorkingCorner.SOUTHWEST)).getType());
        assertEquals(TargetKind.POTION_CRATE, decide(active().rejuvenationDoses(0)).getTarget());
    }
    @Test public void betweenRoundsRestocksPartialPotions()
    {
        decide(active());
        assertEquals(ObjectiveType.CHOP, decide(active().rejuvenationDoses(1)).getType());
        assertEquals(TargetKind.POTION_CRATE, decide(active().roundState(RoundState.COMPLETE).rejuvenationDoses(1)).getTarget());
        assertEquals(TargetKind.HERBS, decide(active().roundState(RoundState.COUNTDOWN).rejuvenationDoses(1).unfinishedPotionCount(1)).getTarget());
    }
    @Test public void reserveIsLatchedWhileCollectingIngredients()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().firemakingLevel(50).warmClothingCount(0).wintertodtEnergy(100).rejuvenationDoses(0);
        assertEquals(3, decide(s).getQuantity());
        assertEquals(12, engine.getPreparationDoseTarget());
        decide(s.wintertodtEnergy(40).warmth(100));
        assertEquals(12, engine.getPreparationDoseTarget());
    }
    @Test public void gatheringLeavesMixingSpaceAndFullFuelBagCanBeEmptied()
    {
        assertEquals(1, decide(active().rejuvenationDoses(0).firemakingLevel(50).warmClothingCount(0).freeSlots(2)).getQuantity());
        assertEquals(TargetKind.HERBS, decide(active().rejuvenationDoses(0).unfinishedPotionCount(1).freeSlots(1)).getTarget());
        assertEquals(ObjectiveType.FEED, decide(active().rejuvenationDoses(0).freeSlots(0).brumaRoots(20)).getType());
        assertEquals(ObjectiveType.WAIT, decide(active().rejuvenationDoses(0).freeSlots(1)).getType());
    }

    @Test public void brokenBrazierDoesNotPullPlayerAwayFromChopping()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().currentActivity(PlayerActivity.CHOPPING);
        assertEquals(ObjectiveType.CHOP, decide(s).getType());
        assertEquals(ObjectiveType.CHOP, decide(s.brumaRoots(8).localBrazierState(BrazierState.BROKEN)).getType());
        assertEquals(ObjectiveType.FLETCH, decide(s.brumaRoots(20)).getType());
        assertEquals(ObjectiveType.REPAIR, decide(s.currentActivity(PlayerActivity.IDLE)).getType());
    }
    @Test public void choppingStillYieldsToWarmthAndRoundEnding()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().currentActivity(PlayerActivity.CHOPPING).brumaRoots(10);
        assertEquals(ObjectiveType.RESTORE_WARMTH, decide(s.warmth(18).warmthItem(ItemID.WINT_POTION4)).getType());
        assertEquals(ObjectiveType.FEED, decide(s.warmth(90).endUrgency(EndUrgency.IMMINENT)).getType());
    }
    @Test public void coldResumePreservesFletchingAndFeedingBatches()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().brumaRoots(20);
        assertEquals(ObjectiveType.FLETCH, decide(s).getType());
        WintertodtObjective resume = decide(s.interrupted(true).playerIdle(false).currentActivity(PlayerActivity.IDLE));
        assertEquals(ObjectiveType.FLETCH, resume.getType()); assertEquals("Continue fletching", resume.getInstruction());
        assertEquals(20, resume.getQuantity());
        decide(s.brumaRoots(0).brumaKindling(20).interrupted(false));
        resume = decide(s.interrupted(true));
        assertEquals(ObjectiveType.FEED, resume.getType()); assertEquals("Continue feeding", resume.getInstruction());
    }
}
