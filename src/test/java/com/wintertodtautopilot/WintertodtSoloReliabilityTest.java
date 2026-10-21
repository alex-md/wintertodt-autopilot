package com.wintertodtautopilot;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtSoloReliabilityTest
{
    static final PlannerSettings SETTINGS = PlannerSettings.from(new WintertodtAutopilotConfig() { });

    static WintertodtSnapshot observed(int tick, int energy, int points, int roots, BrazierState fire)
    {
        List<CornerObservation> corners = new ArrayList<>();
        for (WorkingCorner c : WorkingCorner.values())
        {
            if (c == WorkingCorner.AUTOMATIC) { continue; }
            corners.add(new CornerObservation(c, c == WorkingCorner.SOUTHWEST ? fire : BrazierState.UNLIT,
                CornerObservation.PyromancerState.ALIVE, -1, CornerObservation.Source.DIRECT_SCENE,
                CornerObservation.Source.DIRECT_SCENE, tick, tick, true, true));
        }
        return WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true)
            .observationTick(tick).roundState(RoundState.ACTIVE).wintertodtEnergy(energy).points(points)
            .warmth(80).rawWarmth(800).brumaRoots(roots).freeSlots(6 - roots).rejuvenationDoses(24)
            .warmthItem(ItemID.WINT_POTION4).potionItem(ItemID.WINT_POTION4).hasKnife(true).hasHammer(true)
            .hasTinderbox(true).hasAxe(true).workingCorner(WorkingCorner.SOUTHWEST)
            .playerLocation(WorkingCorner.SOUTHWEST.location()).corners(corners).build();
    }

    @Test public void loadingAtEveryRecoveryBoundaryRequiresNewExitConfirmation()
    {
        for (int resumedEnergy : new int[]{9, 11, 12})
        {
            WintertodtSoloEngine engine = new WintertodtSoloEngine();
            engine.decide(observed(10, 9, 100, 0, BrazierState.UNLIT), SETTINGS);
            engine.decide(observed(11, 12, 100, 1, BrazierState.UNLIT), SETTINGS);
            engine.suspendObservations();
            assertTrue(engine.isAwaitingObservations());
            WintertodtSnapshot resumed = observed(20, resumedEnergy, 100, 2, BrazierState.UNLIT);
            assertEquals(ObjectiveType.CHOP, engine.decide(resumed, SETTINGS).getType());
            assertEquals(SoloPhase.RECOVER_ENERGY, engine.getPhase());
            assertEquals(6, engine.batchTarget());
            engine.decide(resumed, SETTINGS); // Duplicate observation cannot confirm recovery twice.
            engine.decide(observed(21, 12, 100, 3, BrazierState.UNLIT), SETTINGS);
            assertEquals(SoloPhase.RECOVER_ENERGY, engine.getPhase());
            engine.decide(observed(22, 12, 100, 4, BrazierState.UNLIT), SETTINGS);
            assertEquals(SoloPhase.RECOVER_ENERGY, engine.getPhase());
            WintertodtObjective relight = engine.decide(observed(23, 12, 100, 4, BrazierState.UNLIT), SETTINGS);
            assertEquals(SoloPhase.SUSTAIN, engine.getPhase());
            assertEquals(ObjectiveType.CHOP, relight.getType()); // Finish the original batch first.
            assertEquals(2, relight.getQuantity());
        }
    }

    @Test public void backwardsClientTickCannotClearRecovery()
    {
        WintertodtSoloEngine engine = new WintertodtSoloEngine();
        engine.decide(observed(20, 9, 100, 0, BrazierState.UNLIT), SETTINGS);
        engine.decide(observed(1, 11, 100, 1, BrazierState.UNLIT), SETTINGS);
        assertTrue(engine.recovering());
        assertNotEquals(ObjectiveType.LIGHT, engine.decide(observed(2, 11, 100, 2, BrazierState.UNLIT), SETTINGS).getType());
    }

    @Test public void missingScoreAfterReloadCannotApplyLatchedFinishToAnUnverifiedRound()
    {
        WintertodtSoloEngine engine = new WintertodtSoloEngine();
        engine.configure(5000, true);
        engine.decide(observed(1, 25, 1000, 0, BrazierState.UNLIT), SETTINGS);
        engine.suspendObservations();
        assertEquals(ObjectiveType.WAIT, engine.decide(observed(5, 25, -1, 0, BrazierState.UNLIT), SETTINGS).getType());
        assertTrue(engine.isFinishRequested()); assertTrue(engine.isAwaitingObservations());
        assertEquals(ObjectiveType.LIGHT, engine.decide(observed(6, 25, 1000, 0, BrazierState.UNLIT), SETTINGS).getType());
    }

    @Test public void lowEnergyObservedDuringWarmthRestorationStillLatchesRecovery()
    {
        WintertodtSoloEngine engine = new WintertodtSoloEngine();
        WintertodtSnapshot cold = observed(10, 9, 100, 0, BrazierState.UNLIT).toBuilder().warmth(15).build();
        assertEquals(ObjectiveType.RESTORE_WARMTH, engine.decide(cold, SETTINGS).getType());
        engine.decide(observed(11, 11, 100, 1, BrazierState.UNLIT), SETTINGS);
        assertTrue(engine.recovering());
        assertEquals(SoloPhase.RECOVER_ENERGY, engine.getPhase());
    }

    @Test public void reloadPreservesPartialFletchingAndFeedingCommitments()
    {
        WintertodtSoloEngine engine = new WintertodtSoloEngine();
        assertEquals(ObjectiveType.FLETCH, engine.decide(observed(1, 28, 100, 6, BrazierState.LIT), SETTINGS).getType());
        engine.suspendObservations();
        WintertodtSnapshot partial = observed(5, 26, 100, 3, BrazierState.LIT).toBuilder().brumaKindling(3).freeSlots(0).build();
        assertEquals(ObjectiveType.FLETCH, engine.decide(partial, SETTINGS).getType());
        WintertodtSnapshot fletched = observed(6, 26, 100, 0, BrazierState.LIT).toBuilder().brumaKindling(6).freeSlots(0).build();
        assertEquals(ObjectiveType.FEED, engine.decide(fletched, SETTINGS).getType());
        engine.suspendObservations();
        assertEquals(ObjectiveType.FEED, engine.decide(fletched.toBuilder().observationTick(8)
            .brumaKindling(5).points(125).freeSlots(1).build(), SETTINGS).getType());
    }

    @Test public void observationLossDoesNotChangeAnOpeningDestinationMidWalk()
    {
        WintertodtSoloEngine engine = new WintertodtSoloEngine();
        WintertodtObjective original = engine.decide(observed(1, 100, 100, 0, BrazierState.LIT), SETTINGS);
        assertNotEquals(WorkingCorner.SOUTHWEST, original.getDestination());
        engine.suspendObservations();
        WintertodtObjective resumed = engine.decide(observed(5, 99, 100, 0, BrazierState.LIT).toBuilder()
            .playerLocation(WorkingCorner.NORTHEAST.location()).build(), SETTINGS);
        assertEquals(original.getDestination(), resumed.getDestination());
    }

    @Test public void irrelevantMassSettingsAndWarmthThresholdsDoNotBreakSoloBatch()
    {
        WintertodtSoloEngine engine = new WintertodtSoloEngine();
        engine.decide(observed(1, 28, 100, 0, BrazierState.LIT), SETTINGS);
        PlannerSettings changed = new PlannerSettings(SETTINGS.getStrategy(), PointGoal.NEXT_THRESHOLD,
            SETTINGS.getFletchMode(), 60, 25);
        WintertodtSnapshot partial = observed(2, 26, 100, 2, BrazierState.LIT).toBuilder().brumaKindling(1).freeSlots(3).build();
        assertEquals(ObjectiveType.CHOP, engine.decide(partial, changed).getType());
        assertEquals(6, engine.batchTarget());
    }

    @Test public void regressedScoreCannotApplyOldFinishIntentToAnUnverifiedRound()
    {
        WintertodtSoloEngine engine = new WintertodtSoloEngine();
        engine.configure(5000, true);
        engine.decide(observed(1, 25, 1000, 0, BrazierState.UNLIT), SETTINGS);
        engine.suspendObservations();
        for (int tick = 3; tick < 7; tick++)
        {
            WintertodtObjective o = engine.decide(observed(tick, 90, 0, 0, BrazierState.UNLIT), SETTINGS);
            assertEquals("solo_score_changed", o.getReason());
            assertEquals(ObjectiveType.WAIT, o.getType());
        }
        assertTrue(engine.isFinishRequested());
        engine.decide(observed(7, 0, 0, 0, BrazierState.UNLIT).toBuilder().roundState(RoundState.COUNTDOWN).secondsToStart(50).build(), SETTINGS);
        assertFalse(engine.isFinishRequested());
        assertEquals(ObjectiveType.LIGHT, engine.decide(observed(8, 100, 0, 0, BrazierState.UNLIT), SETTINGS).getType());
    }

    @Test public void unknownContainersCannotInventMissingToolsOrCauseRoutineWork()
    {
        WintertodtSnapshot unknown = observed(1, 28, 100, 0, BrazierState.UNLIT).toBuilder()
            .inventoryKnown(false).hasAxe(false).hasKnife(false).hasHammer(false).build();
        assertEquals("supplies_unobserved", new WintertodtSoloEngine().decide(unknown, SETTINGS).getReason());
        assertEquals("supplies_unobserved", new WintertodtDecisionEngine().decide(unknown, SETTINGS).getReason());
        assertEquals("warmth_supplies_unknown", new WintertodtSoloEngine().decide(unknown.toBuilder().warmth(15).build(), SETTINGS).getReason());
        assertEquals("supplies_unobserved", new WintertodtSoloEngine().decide(unknown.toBuilder()
            .inventoryKnown(true).equipmentKnown(false).build(), SETTINGS).getReason());
    }

    @Test public void loadedFireCountDoesNotBecomeAnExactDrainCount()
    {
        WintertodtSnapshot all = observed(1, 28, 100, 0, BrazierState.LIT);
        assertEquals(1, all.observedFunctionalBraziers());
        CornerObservation home = all.corner(WorkingCorner.SOUTHWEST);
        WintertodtSnapshot unknown = all.toBuilder().clearCorners().corner(new CornerObservation(home.getCorner(),
            home.getBrazierState(), CornerObservation.PyromancerState.UNKNOWN, -1, home.getBrazierSource(),
            CornerObservation.Source.NONE, 1, -1, true, false)).build();
        assertEquals(-1, unknown.observedFunctionalBraziers());
    }
}
