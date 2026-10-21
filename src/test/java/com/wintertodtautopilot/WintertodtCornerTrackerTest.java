package com.wintertodtautopilot;

import java.util.List;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.gameval.NpcID;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WintertodtCornerTrackerTest
{
    private final WintertodtObjectTracker objects = mock(WintertodtObjectTracker.class);
    private final WintertodtNpcTracker npcs = mock(WintertodtNpcTracker.class);
    private final WintertodtCornerTracker tracker = new WintertodtCornerTracker();

    private CornerObservation corner(List<CornerObservation> observations, WorkingCorner corner)
    {
        return observations.stream().filter(c -> c.getCorner() == corner).findFirst().get();
    }

    @Test public void partialCoverageNeverEstablishesGlobalUnlitState()
    {
        when(objects.brazierAt(WorkingCorner.SOUTHWEST)).thenReturn(BrazierState.UNLIT);
        List<CornerObservation> observations = tracker.observe(10, objects, npcs);
        assertEquals(4, observations.size());
        assertEquals(BrazierState.UNLIT, corner(observations, WorkingCorner.SOUTHWEST).getBrazierState());
        CornerObservation remote = corner(observations, WorkingCorner.NORTHEAST);
        assertEquals(BrazierState.UNKNOWN, remote.getBrazierState());
        assertEquals(-1, remote.getBrazierObservedTick());
        assertFalse(remote.brazierFresh(10));
        assertEquals(-1, WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).observationTick(10).corners(observations).build().observedGlobalLitBraziers());
    }

    @Test public void losingVisibilityImmediatelyClearsStateAndHistoryExpiresAfterEightTicks()
    {
        when(objects.brazierAt(WorkingCorner.SOUTHWEST)).thenReturn(BrazierState.LIT);
        List<CornerObservation> first = tracker.observe(10, objects, npcs);
        when(objects.brazierAt(WorkingCorner.SOUTHWEST)).thenReturn(BrazierState.UNKNOWN);
        CornerObservation lost = corner(tracker.observe(11, objects, npcs), WorkingCorner.SOUTHWEST);
        assertEquals(BrazierState.UNKNOWN, lost.getBrazierState());
        assertFalse(lost.isBrazierVisible());
        assertEquals(CornerObservation.Source.NONE, lost.getBrazierSource());
        assertEquals(10, lost.getBrazierObservedTick());
        assertTrue(lost.brazierFresh(18));
        assertFalse(lost.brazierFresh(19));
        assertFalse(lost.brazierFresh(9));
        assertEquals(BrazierState.LIT, corner(first, WorkingCorner.SOUTHWEST).getBrazierState());
        tracker.reset();
        assertEquals(-1, corner(tracker.observe(20, objects, npcs), WorkingCorner.SOUTHWEST).getBrazierObservedTick());
    }

    @Test public void globalCountRequiresFourFreshDistinctCorners()
    {
        for (WorkingCorner c : WorkingCorner.values())
        {
            if (c != WorkingCorner.AUTOMATIC) { when(objects.brazierAt(c)).thenReturn(BrazierState.UNLIT); }
        }
        when(objects.brazierAt(WorkingCorner.NORTHEAST)).thenReturn(BrazierState.LIT);
        WintertodtSnapshot snapshot = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).observationTick(10)
            .corners(tracker.observe(10, objects, npcs)).build();
        assertEquals(1, snapshot.observedGlobalLitBraziers());
        assertEquals(-1, snapshot.toBuilder().observationTick(19).build().observedGlobalLitBraziers());
        when(objects.brazierAt(WorkingCorner.NORTHEAST)).thenReturn(BrazierState.UNLIT);
        assertEquals(0, WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).observationTick(11)
            .corners(tracker.observe(11, objects, npcs)).build().observedGlobalLitBraziers());
    }

    @Test public void pyromancerHealthAndVisibilityAreIndependentOfBrazier()
    {
        NPC npc = mock(NPC.class); NPCComposition composition = mock(NPCComposition.class);
        when(npc.getTransformedComposition()).thenReturn(composition);
        when(composition.getId()).thenReturn(NpcID.WINT_WIZARD);
        when(npc.getHealthRatio()).thenReturn(3); when(npc.getHealthScale()).thenReturn(4);
        when(npcs.target(TargetKind.PYROMANCER, WorkingCorner.SOUTHWEST.location())).thenReturn(npc);
        CornerObservation alive = corner(tracker.observe(10, objects, npcs), WorkingCorner.SOUTHWEST);
        assertEquals(75, alive.getPyromancerHealthPercent());
        assertEquals(BrazierState.UNKNOWN, alive.getBrazierState());
        when(npc.getHealthScale()).thenReturn(-1);
        assertEquals(-1, corner(tracker.observe(11, objects, npcs), WorkingCorner.SOUTHWEST).getPyromancerHealthPercent());
        when(composition.getId()).thenReturn(NpcID.WINT_WIZARD_DOWN);
        CornerObservation down = corner(tracker.observe(12, objects, npcs), WorkingCorner.SOUTHWEST);
        assertEquals(CornerObservation.PyromancerState.INCAPACITATED, down.getPyromancerState());
        assertEquals(0, down.getPyromancerHealthPercent());
        when(npcs.target(TargetKind.PYROMANCER, WorkingCorner.SOUTHWEST.location())).thenReturn(null);
        CornerObservation lost = corner(tracker.observe(13, objects, npcs), WorkingCorner.SOUTHWEST);
        assertEquals(CornerObservation.PyromancerState.UNKNOWN, lost.getPyromancerState());
        assertEquals(-1, lost.getPyromancerHealthPercent());
        assertEquals(12, lost.getPyromancerObservedTick());
        assertFalse(lost.isPyromancerVisible());
        // Tick regression invalidates observations from a previous client timeline.
        assertEquals(-1, corner(tracker.observe(1, objects, npcs), WorkingCorner.SOUTHWEST).getPyromancerObservedTick());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void observationListIsImmutable() { tracker.observe(1, objects, npcs).clear(); }

    @Test public void traceKeepsEarnedAndCarriedSeparateAndLabelsPlannerHonestly()
    {
        WintertodtSnapshot snapshot = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).observationTick(10).points(400)
            .brumaRoots(3).brumaKindling(2).workingCorner(WorkingCorner.SOUTHWEST)
            .corners(tracker.observe(10, objects, npcs)).build();
        WintertodtObjective objective = new WintertodtObjective(ObjectiveType.LIGHT, "Relight", "brazier_unlit",
            TargetKind.BRAZIER, 0, false);
        String trace = WintertodtObservationTrace.format(snapshot, objective);
        assertTrue(trace.contains("tick=10 planner=MASS soloPhase=NOT_RUNNING"));
        assertTrue(trace.contains("destination=SOUTHWEST action=LIGHT reason=brazier_unlit"));
        assertTrue(trace.contains("earned=400 carried=80"));
        assertTrue(trace.contains("observedGlobalLit=-1"));
        assertTrue(trace.contains("NORTHEAST.brazier=UNKNOWN"));
    }

    @Test public void unchangedCachedHealthExpiresWhileStandingStateRemainsFresh()
    {
        NPC npc = mock(NPC.class); NPCComposition composition = mock(NPCComposition.class);
        when(npc.getTransformedComposition()).thenReturn(composition);
        when(composition.getId()).thenReturn(NpcID.WINT_WIZARD);
        when(npc.getHealthRatio()).thenReturn(3); when(npc.getHealthScale()).thenReturn(4);
        when(npcs.target(TargetKind.PYROMANCER, WorkingCorner.SOUTHWEST.location())).thenReturn(npc);
        assertTrue(corner(tracker.observe(10, objects, npcs), WorkingCorner.SOUTHWEST).healthFresh(10));
        for (int t = 11; t <= 19; t++) { tracker.observe(t, objects, npcs); }
        CornerObservation stale = corner(tracker.observe(20, objects, npcs), WorkingCorner.SOUTHWEST);
        assertEquals(-1, stale.getPyromancerHealthPercent()); assertFalse(stale.healthFresh(20));
        assertTrue(stale.pyromancerFresh(20));
        assertEquals(CornerObservation.PyromancerState.ALIVE, stale.getPyromancerState());
        when(npc.getHealthRatio()).thenReturn(2);
        CornerObservation updated = corner(tracker.observe(21, objects, npcs), WorkingCorner.SOUTHWEST);
        assertEquals(50, updated.getPyromancerHealthPercent()); assertTrue(updated.healthFresh(21));
    }
    @Test public void replacingTheNpcOrLosingItsHealthDoesNotReusePreviousInjuryEvidence()
    {
        NPC npc = mock(NPC.class), replacement = mock(NPC.class); NPCComposition composition = mock(NPCComposition.class);
        when(composition.getId()).thenReturn(NpcID.WINT_WIZARD);
        for (NPC n : new NPC[]{npc, replacement})
        {
            when(n.getTransformedComposition()).thenReturn(composition);
            when(n.getHealthRatio()).thenReturn(3); when(n.getHealthScale()).thenReturn(4);
        }
        when(npcs.target(TargetKind.PYROMANCER, WorkingCorner.SOUTHWEST.location())).thenReturn(npc);
        tracker.observe(10, objects, npcs);
        when(npcs.target(TargetKind.PYROMANCER, WorkingCorner.SOUTHWEST.location())).thenReturn(replacement);
        assertEquals(20, corner(tracker.observe(20, objects, npcs), WorkingCorner.SOUTHWEST).getHealthObservedTick());
        when(replacement.getHealthRatio()).thenReturn(-1);
        CornerObservation missing = corner(tracker.observe(21, objects, npcs), WorkingCorner.SOUTHWEST);
        assertEquals(-1, missing.getPyromancerHealthPercent()); assertFalse(missing.healthFresh(21));
    }

    @Test public void aSpentHealingDoseInvalidatesCachedInjuryUntilNewHealthEvidence()
    {
        NPC npc = mock(NPC.class); NPCComposition composition = mock(NPCComposition.class);
        when(npc.getTransformedComposition()).thenReturn(composition); when(composition.getId()).thenReturn(NpcID.WINT_WIZARD);
        when(npc.getHealthRatio()).thenReturn(2); when(npc.getHealthScale()).thenReturn(4);
        when(npcs.target(TargetKind.PYROMANCER, WorkingCorner.SOUTHWEST.location())).thenReturn(npc);
        tracker.observe(10, objects, npcs); tracker.invalidateHealth(WorkingCorner.SOUTHWEST);
        assertFalse(corner(tracker.observe(11, objects, npcs), WorkingCorner.SOUTHWEST).healthFresh(11));
        when(npc.getHealthRatio()).thenReturn(3);
        assertTrue(corner(tracker.observe(12, objects, npcs), WorkingCorner.SOUTHWEST).healthFresh(12));
    }
}
