package com.wintertodtautopilot;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtEffortPolicyTest
{
    static final PlannerSettings COMFORTABLE = new PlannerSettings(WintertodtStrategy.MASS_POINTS,
        PointGoal.MINIMUM_500, FletchMode.AUTOMATIC, 50, 20, SoloEffort.COMFORTABLE);
    static final PlannerSettings ACTIVE = new PlannerSettings(WintertodtStrategy.MASS_POINTS,
        PointGoal.MINIMUM_500, FletchMode.AUTOMATIC, 50, 20, SoloEffort.ACTIVE);
    static WintertodtSnapshot scene(int tick, int energy, int doses)
    {
        return WintertodtSoloReliabilityTest.observed(tick, energy, 100, 0, BrazierState.LIT).toBuilder()
            .rejuvenationDoses(doses).runEnergyPercent(100).build();
    }
    static WintertodtSnapshot corner(WintertodtSnapshot s, WorkingCorner target, BrazierState fire,
        CornerObservation.PyromancerState pyro, int health, int healthTick)
    {
        List<CornerObservation> result = new ArrayList<>();
        for (CornerObservation c : s.getCorners())
        {
            result.add(c.getCorner() != target ? c : new CornerObservation(target, fire, pyro, health,
                CornerObservation.Source.DIRECT_SCENE, CornerObservation.Source.DIRECT_SCENE,
                s.getObservationTick(), s.getObservationTick(), true, true, healthTick));
        }
        return s.toBuilder().clearCorners().corners(result).build();
    }
    @Test public void newPreferenceDefaultsToComfortableWithoutChangingSavedStrategyOrGoal()
    {
        WintertodtAutopilotConfig config = new WintertodtAutopilotConfig() {
            public WintertodtMode mode() { return WintertodtMode.SOLO; }
            public SoloStrategy soloStrategy() { return SoloStrategy.FIREMAKING_XP; }
            public int soloPointGoal() { return 13500; }
        };
        assertEquals(SoloEffort.COMFORTABLE, PlannerSettings.from(config).getSoloEffort());
        assertEquals(WintertodtStrategy.MASS_XP, PlannerSettings.from(config).getStrategy());
        assertEquals(13500, config.soloPointGoal());
    }
    @Test public void comfortableSkipsOptionalHealingButStillHealsADownedWorkingPyromancer()
    {
        WintertodtSnapshot hurt = corner(scene(10, 25, 24), WorkingCorner.SOUTHWEST, BrazierState.LIT,
            CornerObservation.PyromancerState.ALIVE, 50, 10).toBuilder().brumaKindling(6).build();
        assertEquals(ObjectiveType.FEED, new WintertodtSoloEngine().decide(hurt, COMFORTABLE).getType());
        assertEquals("solo_points_heal", new WintertodtSoloEngine().decide(hurt, ACTIVE).getReason());
        WintertodtSnapshot down = corner(hurt, WorkingCorner.SOUTHWEST, BrazierState.UNLIT,
            CornerObservation.PyromancerState.INCAPACITATED, 0, 10);
        assertEquals(ObjectiveType.HEAL_PYROMANCER, new WintertodtSoloEngine().decide(down, COMFORTABLE).getType());
    }
    @Test public void comfortableFinishesAtOneStableFireAndActiveUsesOnlyAnAdjacentPair()
    {
        WintertodtSoloEngine engine = new WintertodtSoloEngine(); engine.configure(5000, true);
        assertEquals("solo_finish_drain", engine.decide(scene(10, 8, 24), COMFORTABLE).getReason());
        WintertodtObjective extra = engine.decide(scene(11, 8, 24), ACTIVE);
        assertEquals(ObjectiveType.LIGHT, extra.getType());
        WorkingCorner second = extra.getDestination(); assertNotEquals(WorkingCorner.SOUTHWEST, second);
        assertTrue(second == WorkingCorner.NORTHWEST || second == WorkingCorner.SOUTHEAST);
        WintertodtSnapshot paired = corner(scene(12, 8, 24), second, BrazierState.LIT,
            CornerObservation.PyromancerState.ALIVE, -1, -1);
        assertEquals("solo_finish_drain", engine.decide(paired, ACTIVE).getReason());
        assertEquals("solo_finish_drain", engine.decide(paired.toBuilder().workingCorner(WorkingCorner.NORTHEAST).build(), ACTIVE).getReason());
    }
    @Test public void unknownAndExhaustedRunNeverAddASecondFinishFire()
    {
        for (int run : new int[]{-1, 0, 10})
        {
            WintertodtSoloEngine e = new WintertodtSoloEngine(); e.configure(5000, true);
            assertEquals("solo_finish_drain", e.decide(scene(10, 8, 24).toBuilder().runEnergyPercent(run).build(), ACTIVE).getReason());
        }
    }
    @Test public void staleInjuryEvidenceCannotTriggerOptionalHealing()
    {
        WintertodtSnapshot stale = corner(scene(20, 25, 24), WorkingCorner.SOUTHWEST, BrazierState.LIT,
            CornerObservation.PyromancerState.ALIVE, 50, 10).toBuilder().brumaKindling(6).build();
        assertEquals(ObjectiveType.FEED, new WintertodtSoloEngine().decide(stale, ACTIVE).getType());
    }
    @Test public void effortChangePreservesTheBatchAndManualFinishStillPreempts()
    {
        WintertodtSoloEngine e = new WintertodtSoloEngine();
        assertEquals(6, e.decide(scene(10, 25, 24), COMFORTABLE).getQuantity());
        assertEquals(5, e.decide(scene(11, 25, 24).toBuilder().brumaRoots(1).freeSlots(5).build(), ACTIVE).getQuantity());
        e.configure(5000, true);
        assertEquals(ObjectiveType.FEED, e.decide(scene(12, 25, 24).toBuilder().brumaRoots(1).freeSlots(5).build(), ACTIVE).getType());
        assertTrue(e.isFinishRequested());
    }
    @Test public void sharedPolicyRejectsEveryDrainRestoringWorkDuringRecovery()
    {
        for (BrazierState fire : new BrazierState[]{BrazierState.LIT, BrazierState.UNLIT, BrazierState.BROKEN})
        {
            WintertodtSnapshot s = corner(scene(10, 11, 24), WorkingCorner.SOUTHWEST, fire,
                CornerObservation.PyromancerState.INCAPACITATED, 0, 10);
            for (ObjectiveType work : new ObjectiveType[]{ObjectiveType.LIGHT, ObjectiveType.REPAIR, ObjectiveType.HEAL_PYROMANCER})
            {
                WintertodtMaintenancePolicy.Verdict result = WintertodtMaintenancePolicy.evaluate(s, WorkingCorner.SOUTHWEST,
                    work, false, true, false, WorkingCorner.SOUTHWEST);
                assertEquals(fire != BrazierState.LIT && work == ObjectiveType.HEAL_PYROMANCER
                    ? WintertodtMaintenancePolicy.Verdict.ALLOW : WintertodtMaintenancePolicy.Verdict.RECOVERY, result);
            }
        }
    }
    @Test public void finishingDoesNotBypassUnknownCoverageAndSustainDoesNotAddRemoteDrain()
    {
        WintertodtSnapshot s = scene(10, 8, 24).toBuilder().clearCorners()
            .corner(scene(10, 8, 24).corner(WorkingCorner.SOUTHWEST)).build();
        assertEquals(WintertodtMaintenancePolicy.Verdict.COVERAGE, WintertodtMaintenancePolicy.evaluate(s,
            WorkingCorner.SOUTHWEST, ObjectiveType.LIGHT, true, true, true, WorkingCorner.SOUTHWEST));
        s = corner(scene(10, 25, 24), WorkingCorner.NORTHWEST, BrazierState.LIT, CornerObservation.PyromancerState.ALIVE, -1, -1);
        s = corner(s, WorkingCorner.SOUTHWEST, BrazierState.LIT, CornerObservation.PyromancerState.INCAPACITATED, 0, 10);
        assertEquals(WintertodtMaintenancePolicy.Verdict.REMOTE_FIRE, WintertodtMaintenancePolicy.evaluate(s,
            WorkingCorner.SOUTHWEST, ObjectiveType.HEAL_PYROMANCER, false, false, false, WorkingCorner.SOUTHWEST));
    }
    @Test public void warmthStillPreemptsBothEffortPreferencesAndFinishing()
    {
        for (PlannerSettings settings : new PlannerSettings[]{COMFORTABLE, ACTIVE})
        {
            WintertodtSoloEngine e = new WintertodtSoloEngine(); e.configure(5000, true);
            assertEquals(ObjectiveType.RESTORE_WARMTH, e.decide(scene(10, 8, 24).toBuilder().warmth(10).build(), settings).getType());
        }
    }

    @Test public void aPendingHealingRequestWaitsForItsResultAndExpiresWithoutPretendingSuccess()
    {
        WintertodtSnapshot down = corner(scene(10, 25, 24), WorkingCorner.SOUTHWEST, BrazierState.UNLIT,
            CornerObservation.PyromancerState.INCAPACITATED, 0, 10).toBuilder()
            .actionActivity(PlayerActivity.HEALING).actionCorner(WorkingCorner.SOUTHWEST)
            .actionState(WintertodtSnapshot.ActionState.REQUESTED).build();
        assertEquals("solo_healing_pending", new WintertodtSoloEngine().decide(down, COMFORTABLE).getReason());
        assertEquals(ObjectiveType.HEAL_PYROMANCER, new WintertodtSoloEngine().decide(down.toBuilder()
            .actionState(WintertodtSnapshot.ActionState.UNCONFIRMED).build(), COMFORTABLE).getType());
        WintertodtSnapshot recovering = down.toBuilder().wintertodtEnergy(9).build();
        assertNotEquals(ObjectiveType.HEAL_PYROMANCER, new WintertodtSoloEngine().decide(recovering, COMFORTABLE).getType());
    }
    @Test public void missingTargetEvidenceNeverPermitsMaintenanceOrOptionalHealing()
    {
        WintertodtSnapshot unknown = corner(scene(10, 25, 24), WorkingCorner.SOUTHWEST, BrazierState.UNKNOWN,
            CornerObservation.PyromancerState.ALIVE, 50, 10);
        for (ObjectiveType action : new ObjectiveType[]{ObjectiveType.LIGHT, ObjectiveType.REPAIR, ObjectiveType.HEAL_PYROMANCER})
        {
            assertEquals(WintertodtMaintenancePolicy.Verdict.UNOBSERVED, WintertodtMaintenancePolicy.evaluate(unknown,
                WorkingCorner.SOUTHWEST, action, false, false, false, WorkingCorner.SOUTHWEST));
        }
    }
}
