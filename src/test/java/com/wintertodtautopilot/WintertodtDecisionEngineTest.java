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
        return WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).roundState(RoundState.ACTIVE).wintertodtEnergy(80).warmth(90).points(0)
            .localBrazierState(BrazierState.LIT).localPyromancerKnown(true)
            .firemakingLevel(99).warmClothingCount(4).rejuvenationDoses(4).potionItem(ItemID.WINT_POTION4)
            .brazierDistance(0).hasHammer(true).hasKnife(true).hasTinderbox(true).hasAxe(true).freeSlots(24).workingCorner(WorkingCorner.SOUTHEAST);
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
    @Test public void consumingCannotClearWarningBeforeWarmthRecovers()
    {
        assertEquals(ObjectiveType.RESTORE_WARMTH, decide(active().warmth(18).warmthItem(ItemID.CAKE)
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
    @Test public void fletchingHighlightsEitherKnife()
    {
        WintertodtObjective result = decide(active().points(25).brumaRoots(19).freeSlots(5));
        assertEquals(ObjectiveType.FLETCH, result.getType());
        assertTrue(result.getItemIds().contains(ItemID.KNIFE));
        assertTrue(result.getItemIds().contains(ItemID.FLETCHING_KNIFE));
        assertTrue(result.getItemIds().contains(ItemID.WINT_BRUMA_ROOT));
    }
    @Test public void downPyromancerBeforeRelight()
    {
        WintertodtSnapshot s = active().localBrazierState(BrazierState.UNLIT)
            .localPyromancerIncapacitated(true).rejuvenationDoses(2).potionItem(ItemID.WINT_POTION2)
            .brumaRoots(3).brumaKindling(5).currentActivity(PlayerActivity.FEEDING).build();
        WintertodtObjective heal = engine.decide(s, settings);
        assertEquals(ObjectiveType.HEAL_PYROMANCER, heal.getType());
        assertFalse(WintertodtHighlight.inventoryItem(s, heal, ItemID.WINT_POTION2));
        assertFalse(WintertodtHighlight.inventoryItem(s, heal, ItemID.WINT_BRUMA_KINDLING));
        assertFalse(WintertodtHighlight.inventoryItem(s, heal, ItemID.WINT_BRUMA_ROOT));
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
    @Test public void fletchingMovesFromRootsToBrazierFirst()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().brumaRoots(20).brazierDistance(8);
        WintertodtObjective move = decide(s);
        assertEquals(ObjectiveType.POSITION, move.getType());
        assertEquals(TargetKind.BRAZIER, move.getTarget());
        assertEquals("fletch_at_brazier", move.getReason());
        assertTrue(move.getItemIds().isEmpty());
        assertEquals(ObjectiveType.POSITION, decide(s.brazierDistance(3).currentActivity(PlayerActivity.MOVING)).getType());
        assertEquals(ObjectiveType.FLETCH, decide(s.brazierDistance(0).currentActivity(PlayerActivity.IDLE)).getType());
        assertEquals(ObjectiveType.REPAIR, decide(s.localBrazierState(BrazierState.BROKEN)).getType());
    }
    @Test public void finishingTrimsFletchingAndThenFeedsAllFuel()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().points(0).brumaRoots(8)
            .roundBudgetSeconds(30).estimatedSecondsLeft(43).currentActivity(PlayerActivity.CHOPPING);
        WintertodtObjective finish = decide(s);
        assertEquals(ObjectiveType.FLETCH, finish.getType());
        assertEquals("finish_fletch", finish.getReason());
        assertEquals(8, finish.getQuantity());
        // Faster drain now leaves time for only two upgrades, not the original eight.
        finish = decide(s.roundBudgetSeconds(20));
        assertEquals(2, finish.getQuantity());
        finish = decide(s.brumaRoots(6).brumaKindling(2).roundBudgetSeconds(17));
        assertEquals(ObjectiveType.FEED, finish.getType());
        assertEquals("finish_feed", finish.getReason());
        assertEquals(8, finish.getQuantity());
    }
    @Test public void travelCostStopsChoppingBeforeFeedTimeRunsOut()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().points(450).brumaRoots(10)
            .brazierDistance(10).roundBudgetSeconds(24).currentActivity(PlayerActivity.CHOPPING);
        WintertodtObjective finish = decide(s);
        assertEquals(ObjectiveType.FEED, finish.getType());
        assertTrue(finish.isUrgent());
    }
    @Test public void lowEnergyCanStillAllowASmallUsefulFletchBatch()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().points(475).brumaRoots(1)
            .wintertodtEnergy(3).endUrgency(EndUrgency.IMMINENT).roundBudgetSeconds(12);
        assertEquals(ObjectiveType.FLETCH, decide(s).getType());
        assertEquals(ObjectiveType.FEED, decide(s.brumaRoots(0).brumaKindling(1).roundBudgetSeconds(9)).getType());
    }
    @Test public void committedFeedDoesNotReturnToChoppingWhenForecastChanges()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().points(500).brumaRoots(10).roundBudgetSeconds(18);
        assertEquals(ObjectiveType.FEED, decide(s).getType());
        assertEquals(ObjectiveType.FEED, decide(s.roundBudgetSeconds(70)).getType());
        assertEquals(ObjectiveType.FEED, decide(s.roundBudgetSeconds(-1)).getType());
        assertEquals(ObjectiveType.CHOP, decide(s.brumaRoots(0).roundBudgetSeconds(100)).getType());
    }
    @Test public void predictedFinishPreemptsAlwaysFletchButWarmthStillWins()
    {
        settings = new PlannerSettings(WintertodtStrategy.MASS_POINTS, PointGoal.MAXIMUM_PRACTICAL, FletchMode.ALWAYS, 35, 20);
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().brumaRoots(20).roundBudgetSeconds(30);
        assertEquals(ObjectiveType.FEED, decide(s).getType());
        assertEquals(ObjectiveType.RESTORE_WARMTH, decide(s.warmth(18).warmthItem(ItemID.WINT_POTION4)).getType());
    }
    @Test public void timedBatchIsSmallEnoughToProcessAndFeed()
    {
        settings = new PlannerSettings(WintertodtStrategy.LOW_ATTENTION, PointGoal.MINIMUM_500, FletchMode.AUTOMATIC, 35, 20);
        WintertodtObjective result = decide(active().roundBudgetSeconds(60));
        assertEquals(ObjectiveType.CHOP, result.getType());
        assertTrue(result.getQuantity() < 24);
        assertTrue(result.getQuantity() * (WintertodtFinishPlan.CHOP_SECONDS + WintertodtFinishPlan.FLETCH_SECONDS
            + WintertodtFinishPlan.FEED_SECONDS) + 1.2 <= 60);
    }
    @Test public void xpAutomaticDoesNotCollectUnusedKnife()
    {
        settings = new PlannerSettings(WintertodtStrategy.MASS_XP, PointGoal.MINIMUM_500, FletchMode.AUTOMATIC, 35, 20);
        assertEquals(ObjectiveType.CHOP, decide(active().hasKnife(false)).getType());
    }

    @Test public void shortTimeWindowDoesNotForceTinyCarriedBatchToFeed()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().brumaRoots(2).roundBudgetSeconds(40).currentActivity(PlayerActivity.CHOPPING);
        WintertodtObjective o = decide(s);
        assertEquals(ObjectiveType.CHOP, o.getType());
        assertTrue(o.getQuantity() > 1);
        assertEquals(ObjectiveType.CHOP, decide(s.roundBudgetSeconds(39)).getType());
    }
    @Test public void securedRewardsAvoidAnotherTinyTripButNearThresholdStillAllowsIt()
    {
        assertEquals(ObjectiveType.WAIT, decide(active().points(600).roundBudgetSeconds(30)).getType());
        engine.reset();
        WintertodtObjective worthwhile = decide(active().points(975).roundBudgetSeconds(15));
        assertEquals(ObjectiveType.CHOP, worthwhile.getType());
        assertEquals(1, worthwhile.getQuantity());
    }
    @Test public void explicitPotionInstructionPreemptsChoppingAndMissingHud()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().warmth(35).warmthItem(ItemID.WINT_POTION2)
            .currentActivity(PlayerActivity.CHOPPING);
        WintertodtObjective drink = decide(s);
        assertEquals(ObjectiveType.RESTORE_WARMTH, drink.getType());
        assertEquals("Drink rejuvenation potion", drink.getInstruction());
        assertTrue(drink.getItemIds().contains(ItemID.WINT_POTION2));
        assertEquals(ObjectiveType.RESTORE_WARMTH, decide(s.roundState(RoundState.COUNTDOWN).wintertodtEnergy(-1).secondsToStart(0)).getType());
    }

    @Test public void relightingPreemptsReserveRestocking()
    {
        decide(active());
        WintertodtObjective o = decide(active().rejuvenationDoses(0).brumaKindling(10)
            .localBrazierState(BrazierState.UNLIT).currentActivity(PlayerActivity.FEEDING));
        assertEquals(ObjectiveType.LIGHT, o.getType());
        assertTrue(WintertodtHighlight.needsAttention(active().currentActivity(PlayerActivity.FEEDING).build(), o));
    }

    @Test public void correctedForecastDoesNotLeaveATinyBatchPermanentlyLatched()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().roundBudgetSeconds(60);
        assertEquals(8, decide(s).getQuantity());
        WintertodtObjective corrected = decide(s.brumaRoots(8).freeSlots(16).roundBudgetSeconds(120));
        assertEquals(ObjectiveType.CHOP, corrected.getType());
        assertEquals(12, corrected.getQuantity());
    }

    @Test public void midRoundEntryWithPartialReserveDoesNotMakeALateSupplyTrip()
    {
        WintertodtObjective o = decide(active().rejuvenationDoses(3).points(350).brumaRoots(8)
            .roundBudgetSeconds(35).estimatedSecondsLeft(43).wintertodtEnergy(8).endUrgency(EndUrgency.ENDING_SOON));
        assertEquals(ObjectiveType.FLETCH, o.getType());
        assertEquals(5, o.getQuantity());
        engine.reset();
        o = decide(active().rejuvenationDoses(0).brumaRoots(8).roundBudgetSeconds(35).endUrgency(EndUrgency.ENDING_SOON));
        assertEquals(ObjectiveType.FLETCH, o.getType());
        assertEquals(8, o.getQuantity());
        o = decide(active().rejuvenationDoses(0).brumaRoots(8).roundBudgetSeconds(35).warmth(30));
        assertEquals(ObjectiveType.RESTORE_WARMTH, o.getType());
        assertEquals(TargetKind.DOOR, o.getTarget());
    }

    @Test public void xpKeepsChoppingSmallBatchesAtEightPercentAfterRelighting()
    {
        settings = new PlannerSettings(WintertodtStrategy.MASS_XP, PointGoal.MINIMUM_500, FletchMode.AUTOMATIC, 35, 20);
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().points(650).wintertodtEnergy(8)
            .endUrgency(EndUrgency.ENDING_SOON).roundBudgetSeconds(18).localBrazierState(BrazierState.UNLIT);
        assertEquals(ObjectiveType.LIGHT, decide(s).getType());
        WintertodtObjective chop = decide(s.localBrazierState(BrazierState.LIT));
        assertEquals(ObjectiveType.CHOP, chop.getType()); assertEquals(3, chop.getQuantity());
        assertEquals(ObjectiveType.FEED, decide(s.brumaRoots(3).roundBudgetSeconds(8)).getType());
        // A corrected/slow pace leaves room for two further roots, even after the final fuel was fed.
        chop = decide(s.brumaRoots(0).points(680).roundBudgetSeconds(14));
        assertEquals(ObjectiveType.CHOP, chop.getType()); assertEquals(2, chop.getQuantity());
        assertEquals(ObjectiveType.WAIT, decide(s.roundBudgetSeconds(5)).getType());
    }
    @Test public void xpDoesNotStayDoneWhenRelightingTemporarilyLosesForecast()
    {
        settings = new PlannerSettings(WintertodtStrategy.MASS_XP, PointGoal.MINIMUM_500, FletchMode.AUTOMATIC, 35, 20);
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().points(650).brumaRoots(3).roundBudgetSeconds(8)
            .wintertodtEnergy(10).endUrgency(EndUrgency.ENDING_SOON);
        assertEquals(ObjectiveType.FEED, decide(s).getType());
        assertEquals(ObjectiveType.WAIT, decide(s.brumaRoots(0).roundBudgetSeconds(5)).getType());
        assertEquals(ObjectiveType.LIGHT, decide(s.roundBudgetSeconds(-1).localBrazierState(BrazierState.UNLIT)).getType());
        WintertodtObjective chop = decide(s.localBrazierState(BrazierState.LIT));
        assertEquals(ObjectiveType.CHOP, chop.getType()); assertEquals(3, chop.getQuantity());
        // Unknown timing at truly imminent energy remains conservative.
        assertEquals(ObjectiveType.WAIT, decide(s.wintertodtEnergy(2).endUrgency(EndUrgency.IMMINENT)).getType());
    }

    @Test public void supplyInstructionsTrackRemainingIngredientsAndMixing()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = active().rejuvenationDoses(0)
            .firemakingLevel(50).warmClothingCount(0).wintertodtEnergy(100);
        assertEquals("Take 3 unfinished potions", decide(s).getInstruction());
        WintertodtObjective two = decide(s.unfinishedPotionCount(1));
        assertEquals(2, two.getQuantity());
        assertEquals("Take 2 unfinished potions", two.getInstruction());
        assertEquals("Take 1 unfinished potion", decide(s.unfinishedPotionCount(2)).getInstruction());
        assertEquals("Pick 3 bruma herbs", decide(s.unfinishedPotionCount(3)).getInstruction());
        assertEquals("Mix 1 potion with Brew'ma", decide(s.brumaHerbCount(1).canMixPotion(false)).getInstruction());
        assertEquals("Mix 1 potion", decide(s.canMixPotion(true)).getInstruction());
    }

    @Test public void missingToolsHaveOneItemAndAnExplicitInstruction()
    {
        WintertodtSnapshot[] missing = {
            active().hasAxe(false).build(), active().hasHammer(false).build(),
            active().hasKnife(false).build(), active().hasTinderbox(false).build()
        };
        String[] labels = {"Take 1 axe", "Take 1 hammer", "Take 1 knife", "Take 1 tinderbox"};
        for (int i = 0; i < missing.length; i++)
        {
            WintertodtObjective o = engine.decide(missing[i], settings);
            assertEquals(ObjectiveType.GET_TOOL, o.getType());
            assertEquals(1, o.getQuantity());
            assertEquals(labels[i], o.getInstruction());
        }
    }

    @Test public void sixRootMassBatchChopsThenFletchesThenFeedsWithoutAlternating()
    {
        assertEquals(6, decide(active().points(350)).getQuantity());
        for (int roots = 1; roots < 6; roots++)
        {
            WintertodtObjective chop = decide(active().points(350).brumaRoots(roots).freeSlots(24 - roots));
            assertEquals(ObjectiveType.CHOP, chop.getType());
            assertEquals(6 - roots, chop.getQuantity());
        }
        for (int roots = 6; roots > 0; roots--)
        {
            WintertodtObjective fletch = decide(active().points(350).brumaRoots(roots)
                .brumaKindling(6 - roots).freeSlots(18));
            assertEquals(ObjectiveType.FLETCH, fletch.getType());
            assertEquals(roots, fletch.getQuantity());
        }
        assertEquals(ObjectiveType.FEED, decide(active().points(350).brumaKindling(6).freeSlots(18)).getType());
    }

    @Test public void animationGapsDoNotSendAnUnfinishedChoppingBatchBackForMaintenance()
    {
        assertEquals(ObjectiveType.CHOP, decide(active().freeSlots(6)).getType());
        for (int count = 1; count < 6; count++)
        {
            assertEquals(ObjectiveType.CHOP, decide(active().brumaRoots(count).freeSlots(6 - count)
                .currentActivity(PlayerActivity.IDLE).localBrazierState(BrazierState.UNLIT)).getType());
        }
        assertEquals(ObjectiveType.LIGHT, decide(active().brumaRoots(6).freeSlots(0)
            .localBrazierState(BrazierState.UNLIT)).getType());
    }

    @Test public void massHealingUsesEveryFinishedPotionVariantAndNeverIngredientsOrFuel()
    {
        for (int potion : new int[]{ItemID.WINT_POTION1, ItemID.WINT_POTION2, ItemID.WINT_POTION3, ItemID.WINT_POTION4})
        {
            WintertodtSnapshot s = active().localBrazierState(BrazierState.UNLIT)
                .localPyromancerIncapacitated(true).potionItem(potion)
                .brumaRoots(5).brumaKindling(5).build();
            WintertodtObjective heal = new WintertodtDecisionEngine().decide(s, settings);
            assertEquals(ObjectiveType.HEAL_PYROMANCER, heal.getType());
            assertEquals(java.util.Collections.singleton(potion), heal.getItemIds());
            assertFalse(WintertodtHighlight.inventoryItem(s, heal, potion));
            for (int irrelevant : new int[]{ItemID.WINT_BRUMA_ROOT, ItemID.WINT_BRUMA_KINDLING, ItemID.WINT_VIAL, ItemID.WINT_HERB})
            { assertFalse(WintertodtHighlight.inventoryItem(s, heal, irrelevant)); }
        }
    }

}
