package com.wintertodtautopilot;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtSoloEngineTest
{
    private final WintertodtSoloEngine engine = new WintertodtSoloEngine();
    private PlannerSettings settings = PlannerSettings.from(new WintertodtAutopilotConfig() { });
    private int tick;
    private int energy = 100, points, roots, kindling, doses = 24, free = 20;
    private final java.util.Map<WorkingCorner, BrazierState> states = new java.util.EnumMap<>(WorkingCorner.class);
    private WorkingCorner down = WorkingCorner.AUTOMATIC, missing = WorkingCorner.AUTOMATIC;
    private boolean remoteHud;

    private WintertodtSnapshot snapshot()
    {
        List<CornerObservation> corners = new ArrayList<>();
        for (WorkingCorner c : WorkingCorner.values())
        {
            if (c == WorkingCorner.AUTOMATIC) { continue; }
            boolean visible = !remoteHud || c == WorkingCorner.SOUTHWEST;
            corners.add(new CornerObservation(c, c == missing ? BrazierState.UNKNOWN : states.getOrDefault(c, BrazierState.UNLIT),
                c == down ? CornerObservation.PyromancerState.INCAPACITATED : CornerObservation.PyromancerState.ALIVE,
                c == down ? 0 : -1, c == missing ? CornerObservation.Source.NONE : visible ? CornerObservation.Source.DIRECT_SCENE : CornerObservation.Source.HUD_WIDGET,
                visible ? CornerObservation.Source.DIRECT_SCENE : CornerObservation.Source.NONE,
                c == missing ? -1 : tick, visible ? tick : -1, visible && c != missing, visible));
        }
        return WintertodtSnapshot.builder().observationTick(tick).roundState(RoundState.ACTIVE)
            .wintertodtEnergy(energy).points(points).warmth(80).rawWarmth(800).brumaRoots(roots).brumaKindling(kindling)
            .rejuvenationDoses(doses).potionItem(ItemID.WINT_POTION4).warmthItem(ItemID.WINT_POTION4)
            .freeSlots(free).hasAxe(true).hasHammer(true).hasKnife(true).hasTinderbox(true).canMixPotion(true)
            .workingCorner(WorkingCorner.SOUTHWEST).playerLocation(WorkingCorner.SOUTHWEST.location()).brazierDistance(0)
            .localBrazierState(states.getOrDefault(WorkingCorner.SOUTHWEST, BrazierState.UNLIT))
            .localPyromancerKnown(true).localPyromancerIncapacitated(down == WorkingCorner.SOUTHWEST).corners(corners).build();
    }
    private WintertodtObjective next() { ++tick; return engine.decide(snapshot(), settings); }
    private void sustain() { energy = 28; states.put(WorkingCorner.SOUTHWEST, BrazierState.LIT); next(); energy = 26; }

    @Test public void openingRoutesHomeThenRemoteAndStopsAtCutoff()
    {
        WintertodtObjective home = next();
        assertEquals(ObjectiveType.LIGHT, home.getType()); assertEquals(WorkingCorner.SOUTHWEST, home.getDestination());
        states.put(WorkingCorner.SOUTHWEST, BrazierState.LIT);
        WintertodtObjective remote = next();
        assertEquals(ObjectiveType.LIGHT, remote.getType()); assertNotEquals(WorkingCorner.SOUTHWEST, remote.getDestination());
        assertEquals(SoloPhase.REDUCE_ENERGY, engine.getPhase());
        energy = 28;
        assertEquals(ObjectiveType.CHOP, next().getType());
        assertEquals(SoloPhase.SUSTAIN, engine.getPhase());
    }
    @Test public void hudOnlyDestinationIsObservedBeforeAnyInteraction()
    {
        remoteHud = true; states.put(WorkingCorner.SOUTHWEST, BrazierState.LIT);
        WintertodtObjective o = next();
        assertEquals(ObjectiveType.POSITION, o.getType()); assertNotEquals(WorkingCorner.SOUTHWEST, o.getDestination());
    }
    @Test public void unknownGlobalCoverageSuppressesRelight()
    {
        missing = WorkingCorner.NORTHEAST;
        WintertodtObjective o = next();
        assertEquals(ObjectiveType.WAIT, o.getType()); assertEquals("solo_coverage_unknown", o.getReason());
        assertEquals(SoloPhase.REASSESS, engine.getPhase());
    }
    @Test public void fullRootBatchFletchesAndThenFeeds()
    {
        sustain(); roots = 20; free = 0;
        WintertodtObjective fletch = next();
        assertEquals(ObjectiveType.FLETCH, fletch.getType());
        assertTrue(fletch.getItemIds().contains(ItemID.FLETCHING_KNIFE));
        roots = 0; kindling = 20;
        assertEquals(ObjectiveType.FEED, next().getType());
        points = 500; kindling = 0; free = 20;
        assertEquals(ObjectiveType.CHOP, next().getType());
    }
    @Test public void soloIgnoresMassFinishForecast()
    {
        sustain();
        WintertodtSnapshot s = snapshot().toBuilder().observationTick(++tick).endUrgency(EndUrgency.IMMINENT)
            .estimatedSecondsLeft(1).roundBudgetSeconds(0).build();
        assertEquals(ObjectiveType.CHOP, engine.decide(s, settings).getType());
    }
    @Test public void recoveryHasHysteresisAndThreeObservedTicksAboveUpperBoundary()
    {
        sustain(); energy = 9; states.put(WorkingCorner.SOUTHWEST, BrazierState.BROKEN); roots = 10; free = 0;
        assertEquals(ObjectiveType.FLETCH, next().getType()); assertEquals(SoloPhase.RECOVER_ENERGY, engine.getPhase());
        energy = 11; next(); assertEquals(SoloPhase.RECOVER_ENERGY, engine.getPhase());
        energy = 12; next(); assertEquals(SoloPhase.RECOVER_ENERGY, engine.getPhase());
        next(); assertEquals(SoloPhase.RECOVER_ENERGY, engine.getPhase());
        assertEquals(ObjectiveType.REPAIR, next().getType()); assertEquals(SoloPhase.SUSTAIN, engine.getPhase());
    }
    @Test public void normalLowBandDoesNotWaitForTwentyFourAndCriticalSignalGapRemainsConservative()
    {
        energy = 22; assertEquals(ObjectiveType.LIGHT, next().getType());
        assertEquals(SoloPhase.SUSTAIN, engine.getPhase());
        engine.decide(snapshot().toBuilder().observationTick(++tick).wintertodtEnergy(-1).build(), settings);
        energy = 9; assertNotEquals(ObjectiveType.LIGHT, next().getType());
        assertEquals(SoloPhase.RECOVER_ENERGY, engine.getPhase());
    }
    @Test public void explicitFinishPermitsLowEnergyRelightsButSustainDoesNot()
    {
        energy = 9;
        assertNotEquals(ObjectiveType.LIGHT, next().getType());
        assertEquals(SoloPhase.RECOVER_ENERGY, engine.getPhase());
        engine.configure(5000, true);
        assertEquals(ObjectiveType.LIGHT, next().getType());
        assertEquals(SoloPhase.FINISH, engine.getPhase()); assertTrue(engine.isFinishRequested());
    }
    @Test public void recoveryCanFeedAnAlreadyLitBrazierWithoutAddingDrain()
    {
        sustain(); energy = 9; kindling = 10;
        assertEquals(ObjectiveType.FEED, next().getType());
        assertEquals(SoloPhase.REASSESS, engine.getPhase()); // Rapid drain takes precedence over the recovery label.
        states.put(WorkingCorner.SOUTHWEST, BrazierState.UNLIT); kindling = 20; free = 0;
        assertEquals(ObjectiveType.WAIT, next().getType());
    }
    @Test public void manualFinishInterruptsGatheringAndFeedsBeforeLightingMore()
    {
        sustain(); roots = 5; engine.configure(5000, true);
        WintertodtObjective o = next(); assertEquals(ObjectiveType.FEED, o.getType());
        assertEquals(SoloPhase.FINISH, engine.getPhase());
        engine.configure(5000, false); assertTrue(engine.isFinishRequested());
        roots = 0; free = 20;
        assertEquals(ObjectiveType.LIGHT, next().getType());
    }
    @Test public void projectedGoalIsCashedOutBeforeEarnedGoalStartsFinishing()
    {
        sustain(); engine.configure(500, false); points = 400; kindling = 4;
        assertEquals("solo_goal_feed", next().getReason()); assertEquals(SoloPhase.SUSTAIN, engine.getPhase());
        points = 500; kindling = 0;
        next(); assertEquals(SoloPhase.FINISH, engine.getPhase());
        engine.configure(1000, false); next(); assertEquals(SoloPhase.SUSTAIN, engine.getPhase());
    }
    @Test public void missingHudDoesNotClearFinishAndVerifiedCompletionDoes()
    {
        sustain(); engine.configure(5000, true); next();
        WintertodtSnapshot missingHud = snapshot().toBuilder().observationTick(++tick).roundState(RoundState.COUNTDOWN)
            .wintertodtEnergy(-1).secondsToStart(0).build();
        assertEquals(ObjectiveType.WAIT, engine.decide(missingHud, settings).getType());
        assertTrue(engine.isFinishRequested()); assertEquals(SoloPhase.REASSESS, engine.getPhase());
        engine.decide(snapshot().toBuilder().observationTick(++tick).roundState(RoundState.COMPLETE).wintertodtEnergy(0).build(), settings);
        assertFalse(engine.isFinishRequested());
        energy = 100; points = 0; next(); assertEquals(SoloPhase.REDUCE_ENERGY, engine.getPhase());
    }
    @Test public void lowWarmthPreemptsEveryPhaseAndNoSuppliesRetreats()
    {
        for (int e : new int[]{100, 28, 14})
        {
            energy = e; ++tick;
            WintertodtObjective o = engine.decide(snapshot().toBuilder().warmth(15).build(), settings);
            assertEquals(ObjectiveType.RESTORE_WARMTH, o.getType()); assertTrue(o.isUrgent());
            o = engine.decide(snapshot().toBuilder().warmth(15).warmthItem(-1).build(), settings);
            assertEquals(TargetKind.DOOR, o.getTarget());
        }
    }
    @Test public void downedHomePyromancerIsHealedBeforeRepair()
    {
        sustain(); states.put(WorkingCorner.SOUTHWEST, BrazierState.BROKEN); down = WorkingCorner.SOUTHWEST;
        WintertodtObjective o = next(); assertEquals(ObjectiveType.HEAL_PYROMANCER, o.getType());
        assertEquals(WorkingCorner.SOUTHWEST, o.getDestination());
        down = WorkingCorner.AUTOMATIC; assertEquals(ObjectiveType.REPAIR, next().getType());
        states.put(WorkingCorner.SOUTHWEST, BrazierState.UNLIT); assertEquals(ObjectiveType.LIGHT, next().getType());
    }
    @Test public void lowDosesTriggerRepeatedRestocksAndIngredientsCanBeMixedWithFullBag()
    {
        sustain(); doses = 3;
        WintertodtObjective o = next(); assertEquals(ObjectiveType.MAKE_REJUVENATION, o.getType());
        assertEquals(24, engine.getPreparationDoseTarget());
        WintertodtSnapshot s = snapshot().toBuilder().observationTick(++tick).unfinishedPotionCount(1).brumaHerbCount(1)
            .freeSlots(0).canMixPotion(false).build();
        o = engine.decide(s, settings); assertEquals(TargetKind.BREWMA, o.getTarget());
        doses = 24; assertEquals(ObjectiveType.CHOP, next().getType());
        doses = 2; assertEquals(ObjectiveType.MAKE_REJUVENATION, next().getType());
    }
    @Test public void fullInventoryFeedsFuelToMakePotionRoom()
    {
        sustain(); doses = 0; kindling = 10; free = 0;
        assertEquals("solo_supply_space", next().getReason());
        states.put(WorkingCorner.SOUTHWEST, BrazierState.UNLIT);
        assertEquals(ObjectiveType.LIGHT, next().getType()); // Relight so carried fuel can free ingredient slots.
        down = WorkingCorner.SOUTHWEST;
        assertEquals("supply_inventory_blocked", next().getReason()); // Cannot heal with zero doses.
        down = WorkingCorner.AUTOMATIC; energy = 9;
        assertEquals("supply_inventory_blocked", next().getReason()); // Do not restart drain just to free space.
    }
    @Test public void noKnifeInXpModeAndTorchCanReplaceTinderbox()
    {
        settings = PlannerSettings.from(new WintertodtAutopilotConfig() {
            @Override public WintertodtStrategy strategy() { return WintertodtStrategy.MASS_XP; }
        });
        ++tick;
        WintertodtObjective o = engine.decide(snapshot().toBuilder().hasKnife(false).hasTinderbox(false).hasBrumaTorch(true).build(), settings);
        assertEquals(ObjectiveType.LIGHT, o.getType());
    }
    @Test public void rapidUnexplainedDrainFeedsWithoutClaimingAnIntruder()
    {
        sustain(); kindling = 10; energy = 22;
        WintertodtObjective o = next(); assertEquals("solo_rapid_drain", o.getReason());
        assertEquals(ObjectiveType.FEED, o.getType()); assertEquals(SoloPhase.REASSESS, engine.getPhase());
        assertFalse(o.getInstruction().toLowerCase().contains("intruder"));
    }
    @Test public void remoteLightingPreventsAddingHomeDrain()
    {
        energy = 28; states.put(WorkingCorner.NORTHEAST, BrazierState.LIT);
        assertNotEquals(ObjectiveType.LIGHT, next().getType());
        assertEquals(SoloPhase.SUSTAIN, engine.getPhase());
    }
    @Test public void missingPointsAndStaleDestinationCannotProduceRelight()
    {
        points = -1; assertEquals(SoloPhase.REASSESS, phaseAfterNext());
        points = 0; ++tick;
        WintertodtSnapshot s = snapshot().toBuilder().observationTick(tick + 9).build();
        assertEquals(ObjectiveType.POSITION, engine.decide(s, settings).getType());
    }
    private SoloPhase phaseAfterNext() { next(); return engine.getPhase(); }
    @Test public void recoveryLoopNeverRecommendsMaintenanceAcrossEnergyOscillation()
    {
        sustain();
        for (int e : new int[]{10, 11, 9, 11, 12, 11, 12, 11, 9, 10, 11})
        {
            energy = e;
            WintertodtObjective o = next();
            assertNotEquals(ObjectiveType.LIGHT, o.getType()); assertNotEquals(ObjectiveType.REPAIR, o.getType());
            assertNotEquals(ObjectiveType.HEAL_PYROMANCER, o.getType());
            assertEquals(SoloPhase.RECOVER_ENERGY, engine.getPhase());
        }
    }
    @Test public void replayOpeningSustainRecoveryGoalAndNextRound()
    {
        engine.configure(1000, false);
        for (int i = 0; i < 4; i++)
        {
            WintertodtObjective light = next();
            assertEquals(ObjectiveType.LIGHT, light.getType());
            states.put(light.getDestination(), BrazierState.LIT);
        }
        assertEquals(ObjectiveType.CHOP, next().getType());
        energy = 28; tick += 20; next(); assertEquals(SoloPhase.SUSTAIN, engine.getPhase());
        for (WorkingCorner c : WorkingCorner.values())
        { if (c != WorkingCorner.SOUTHWEST) { states.put(c, BrazierState.UNLIT); } }
        roots = 20; free = 0; assertEquals(ObjectiveType.FLETCH, next().getType());
        roots = 0; kindling = 20; assertEquals(ObjectiveType.FEED, next().getType());
        points = 500; kindling = 0; free = 20; next();
        energy = 9; states.put(WorkingCorner.SOUTHWEST, BrazierState.UNLIT); roots = 10; free = 0;
        assertEquals(ObjectiveType.FLETCH, next().getType());
        assertEquals(SoloPhase.RECOVER_ENERGY, engine.getPhase());
        roots = 0; kindling = 10; energy = 12;
        next(); next(); assertEquals(ObjectiveType.LIGHT, next().getType());
        states.put(WorkingCorner.SOUTHWEST, BrazierState.LIT);
        assertEquals(ObjectiveType.FEED, next().getType());
        points = 750; kindling = 10; assertEquals("solo_goal_feed", next().getReason());
        points = 1000; kindling = 0; free = 20;
        for (int i = 0; i < 1; i++)
        {
            WintertodtObjective light = next(); assertEquals(ObjectiveType.LIGHT, light.getType());
            states.put(light.getDestination(), BrazierState.LIT);
        }
        assertEquals(SoloPhase.FINISH, engine.getPhase());
        assertEquals("solo_finish_drain", next().getReason());
        energy = 5; assertEquals("solo_finish_drain", next().getReason());
        WintertodtObjective complete = engine.decide(snapshot().toBuilder().observationTick(++tick)
            .roundState(RoundState.COMPLETE).wintertodtEnergy(0).build(), settings);
        assertEquals(ObjectiveType.ROUND_COMPLETE, complete.getType());
        energy = 100; points = 0; states.clear();
        assertEquals(ObjectiveType.LIGHT, next().getType()); assertEquals(SoloPhase.REDUCE_ENERGY, engine.getPhase());
    }

    @Test public void renewedOpeningDrainStaysLatchedUntilLowerCutoff()
    {
        sustain(); energy = 36; next(); assertEquals(SoloPhase.REDUCE_ENERGY, engine.getPhase());
        energy = 35; next(); assertEquals(SoloPhase.REDUCE_ENERGY, engine.getPhase());
        energy = 30; next(); assertEquals(SoloPhase.REDUCE_ENERGY, engine.getPhase());
        energy = 28; next(); assertEquals(SoloPhase.SUSTAIN, engine.getPhase());
    }

    @Test public void gatheringFletchingAndFeedingFollowTheNewWorkingCornerAfterRemoteLighting()
    {
        energy = 28;
        for (WorkingCorner c : WorkingCorner.values()) { states.put(c, BrazierState.LIT); }
        next();
        for (WorkingCorner c : new WorkingCorner[]{WorkingCorner.NORTHEAST, WorkingCorner.NORTHWEST, WorkingCorner.SOUTHEAST})
        {
            WintertodtSnapshot nearby = snapshot().toBuilder().observationTick(++tick)
                .workingCorner(c).playerLocation(c.location()).brazierDistance(0).build();
            WintertodtObjective chop = engine.decide(nearby, settings);
            assertEquals(ObjectiveType.CHOP, chop.getType());
            assertEquals(c, chop.getDestination());
            WintertodtSnapshot fullRoots = nearby.toBuilder().observationTick(++tick).brumaRoots(20).freeSlots(0).build();
            WintertodtObjective fletch = engine.decide(fullRoots, settings);
            assertEquals(ObjectiveType.FLETCH, fletch.getType());
            assertEquals(c, fletch.getDestination());
            WintertodtObjective position = engine.decide(fullRoots.toBuilder().observationTick(++tick).brazierDistance(5).build(), settings);
            assertEquals(ObjectiveType.FLETCH, position.getType()); // No trip beside a brazier is needed to fletch.
            assertEquals(c, position.getDestination());
            WintertodtObjective feed = engine.decide(fullRoots.toBuilder().observationTick(++tick).brumaRoots(0).brumaKindling(20).build(), settings);
            assertEquals(ObjectiveType.FEED, feed.getType());
            assertEquals(c, feed.getDestination());
        }
    }

    @Test public void explicitWorkingCornerRemainsPinnedEvenWhenPlayerIsAcrossArena()
    {
        energy = 28; states.put(WorkingCorner.SOUTHWEST, BrazierState.LIT);
        WintertodtObjective chop = engine.decide(snapshot().toBuilder().observationTick(++tick)
            .playerLocation(WorkingCorner.NORTHEAST.location()).workingCorner(WorkingCorner.SOUTHWEST).build(), settings);
        assertEquals(ObjectiveType.CHOP, chop.getType());
        assertEquals(WorkingCorner.SOUTHWEST, chop.getDestination());
    }

    @Test public void recoveryGatheringUsesCurrentCornerWithoutRelightingIt()
    {
        sustain(); energy = 9; states.clear();
        WintertodtObjective chop = engine.decide(snapshot().toBuilder().observationTick(++tick)
            .workingCorner(WorkingCorner.NORTHEAST).playerLocation(WorkingCorner.NORTHEAST.location()).build(), settings);
        assertEquals(ObjectiveType.CHOP, chop.getType());
        assertEquals(WorkingCorner.NORTHEAST, chop.getDestination());
        assertEquals(SoloPhase.RECOVER_ENERGY, engine.getPhase());
    }

    private WintertodtSnapshot health(WintertodtSnapshot snapshot, WorkingCorner target, int percent)
    {
        List<CornerObservation> observations = new ArrayList<>();
        for (CornerObservation c : snapshot.getCorners())
        {
            observations.add(c.getCorner() != target ? c : new CornerObservation(c.getCorner(), c.getBrazierState(),
                c.getPyromancerState(), percent, c.getBrazierSource(), c.getPyromancerSource(),
                c.getBrazierObservedTick(), c.getPyromancerObservedTick(), c.isBrazierVisible(), c.isPyromancerVisible()));
        }
        return snapshot.toBuilder().clearCorners().corners(observations).build();
    }

    @Test public void pointsHealsObservedDamageButXpWaitsForIncapacitation()
    {
        sustain();
        WintertodtSnapshot damaged = health(snapshot().toBuilder().observationTick(++tick)
            .brumaKindling(5).currentActivity(PlayerActivity.FEEDING).build(), WorkingCorner.SOUTHWEST, 50);
        WintertodtObjective o = engine.decide(damaged, settings);
        assertEquals("solo_points_heal", o.getReason());
        assertEquals(WorkingCorner.SOUTHWEST, o.getDestination());
        assertTrue(WintertodtHighlight.inventoryItem(damaged, o, ItemID.WINT_POTION4));
        assertFalse(WintertodtHighlight.inventoryItem(damaged, o, ItemID.WINT_BRUMA_KINDLING));
        assertFalse(WintertodtHighlight.inventoryItem(damaged, o, ItemID.WINT_BRUMA_ROOT));
        PlannerSettings xp = new PlannerSettings(WintertodtStrategy.MASS_XP, PointGoal.MINIMUM_500,
            FletchMode.AUTOMATIC, 50, 20);
        assertEquals(ObjectiveType.CHOP, engine.decide(damaged.toBuilder()
            .brumaKindling(0).currentActivity(PlayerActivity.IDLE).build(), xp).getType());
        down = WorkingCorner.SOUTHWEST;
        assertEquals(ObjectiveType.HEAL_PYROMANCER, engine.decide(snapshot().toBuilder().observationTick(++tick).build(), xp).getType());
    }

    @Test public void pointsUsesRemoteHealingOpportunitiesWhileFletchingButSkipsThemWithLowRun()
    {
        sustain(); roots = 18; free = 0;
        WintertodtSnapshot damaged = health(snapshot().toBuilder().observationTick(++tick).build(), WorkingCorner.NORTHEAST, 50);
        WintertodtObjective heal = engine.decide(damaged, settings);
        assertEquals("solo_points_heal", heal.getReason());
        assertEquals(WorkingCorner.NORTHEAST, heal.getDestination());
        assertEquals(ObjectiveType.FLETCH, engine.decide(damaged.toBuilder().runEnergyPercent(0).build(), settings).getType());
    }

    @Test public void remoteHealingDoesNotInterruptAChoppingBatch()
    {
        sustain(); roots = 3;
        WintertodtSnapshot damaged = health(snapshot().toBuilder().observationTick(++tick)
            .currentActivity(PlayerActivity.CHOPPING).build(), WorkingCorner.NORTHEAST, 50);
        assertEquals(ObjectiveType.CHOP, engine.decide(damaged, settings).getType());
    }

    @Test public void recoveryCanHealAnUnlitRemotePyromancerButCannotRestartLitDrain()
    {
        sustain(); energy = 9; states.clear(); roots = 10; free = 0; down = WorkingCorner.NORTHEAST;
        WintertodtObjective heal = next();
        assertEquals(ObjectiveType.HEAL_PYROMANCER, heal.getType());
        assertEquals(WorkingCorner.NORTHEAST, heal.getDestination());
        states.put(WorkingCorner.NORTHEAST, BrazierState.LIT);
        tick += 7; // Isolate healing policy from the earlier rapid-drain cash-out advisory.
        assertEquals(ObjectiveType.FLETCH, next().getType());
        missing = WorkingCorner.NORTHEAST;
        assertEquals(ObjectiveType.FLETCH, next().getType());
    }

    @Test public void finishMaintainsTwoAdjacentBraziersInsteadOfOpeningAllFour()
    {
        sustain(); engine.configure(5000, true);
        WintertodtObjective light = next();
        assertEquals(ObjectiveType.LIGHT, light.getType());
        WorkingCorner second = light.getDestination();
        assertNotEquals(WorkingCorner.NORTHEAST, second);
        states.put(second, BrazierState.LIT);
        assertEquals("solo_finish_drain", next().getReason());
        // Crossing into another automatic working corner must not grow the finish circuit.
        assertEquals("solo_finish_drain", engine.decide(snapshot().toBuilder().observationTick(++tick)
            .workingCorner(WorkingCorner.NORTHEAST).playerLocation(WorkingCorner.NORTHEAST.location()).build(), settings).getReason());
    }

    @Test public void exhaustedRunEnergyUsesOnlyTheNearbyBrazierForFinishing()
    {
        sustain(); engine.configure(5000, true);
        WintertodtSnapshot tired = snapshot().toBuilder().observationTick(++tick).runEnergyPercent(0).build();
        assertEquals("solo_finish_drain", engine.decide(tired, settings).getReason());
        states.put(WorkingCorner.SOUTHWEST, BrazierState.UNLIT);
        WintertodtObjective light = engine.decide(snapshot().toBuilder().observationTick(++tick).runEnergyPercent(0).build(), settings);
        assertEquals(ObjectiveType.LIGHT, light.getType());
        assertEquals(WorkingCorner.SOUTHWEST, light.getDestination());
    }

    @Test public void earnedGoalAlsoPermitsFinishingAtLowEnergyButStillRequiresGlobalEvidence()
    {
        energy = 5; points = 5000;
        assertEquals(ObjectiveType.LIGHT, next().getType());
        missing = WorkingCorner.NORTHEAST;
        assertEquals("solo_coverage_unknown", next().getReason());
    }

    @Test public void fletchingDoesNotRequireBrazierAdjacencyButMustLeaveTheSafeArea()
    {
        sustain(); roots = 20; free = 0;
        WintertodtSnapshot atRoots = snapshot().toBuilder().observationTick(++tick).brazierDistance(8).build();
        assertEquals(ObjectiveType.FLETCH, engine.decide(atRoots, settings).getType());
        WintertodtObjective position = engine.decide(atRoots.toBuilder().observationTick(++tick)
            .playerLocation(new net.runelite.api.coords.WorldPoint(1630, 3975, 0)).build(), settings);
        assertEquals(ObjectiveType.POSITION, position.getType());
        assertEquals("solo_fletch_inside", position.getReason());
    }

    @Test public void soloBatchUsesAvailableCapacityInsteadOfAnArbitraryTwentyRootCap()
    {
        energy = 28; free = 23; states.put(WorkingCorner.SOUTHWEST, BrazierState.LIT);
        assertEquals(23, next().getQuantity());
    }

    @Test public void automaticCornerChangesDoNotCancelAnOpeningDestinationMidWalk()
    {
        states.put(WorkingCorner.SOUTHWEST, BrazierState.LIT);
        WorkingCorner destination = next().getDestination();
        WintertodtObjective continued = engine.decide(snapshot().toBuilder().observationTick(++tick)
            .workingCorner(WorkingCorner.NORTHEAST).playerLocation(WorkingCorner.NORTHEAST.location()).build(), settings);
        assertEquals(destination, continued.getDestination());
    }

    @Test public void recoveryCollectsAFullBatchInsteadOfAlternatingOneRootAndOneFletch()
    {
        sustain(); energy = 9; states.clear(); roots = 3; free = 8;
        WintertodtObjective chop = next();
        assertEquals(ObjectiveType.CHOP, chop.getType()); assertEquals(8, chop.getQuantity());
        roots = 4; free = 7;
        assertEquals(ObjectiveType.CHOP, next().getType());
        roots = 11; free = 0;
        assertEquals(ObjectiveType.FLETCH, next().getType());
        roots = 10; kindling = 1;
        assertEquals(ObjectiveType.FLETCH, next().getType());
    }

}
