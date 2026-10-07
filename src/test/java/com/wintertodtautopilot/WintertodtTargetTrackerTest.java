package com.wintertodtautopilot;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WintertodtTargetTrackerTest
{
    @Test public void automaticCornerHasHysteresisAndFollowsIntentionalMovement()
    {
        WintertodtObjectTracker tracker = new WintertodtObjectTracker();
        assertEquals(WorkingCorner.SOUTHEAST, tracker.select(new WorldPoint(1638, 3999, 0), WorkingCorner.AUTOMATIC, null));
        assertEquals(WorkingCorner.SOUTHEAST, tracker.select(new WorldPoint(1629, 4007, 0), WorkingCorner.AUTOMATIC, null));
        assertEquals(WorkingCorner.NORTHWEST, tracker.select(new WorldPoint(1620, 4016, 0), WorkingCorner.AUTOMATIC, null));
        assertEquals(WorkingCorner.SOUTHWEST, tracker.select(new WorldPoint(1620, 4016, 0), WorkingCorner.SOUTHWEST, null));
        tracker.reset();
        assertEquals(WorkingCorner.SOUTHEAST, tracker.select(new WorldPoint(1638, 3999, 0), WorkingCorner.AUTOMATIC, null));
    }
    @Test public void distantClickRetainsIntentWhileWalking()
    {
        WintertodtObjectTracker tracker = new WintertodtObjectTracker();
        tracker.select(WorkingCorner.SOUTHWEST.location(), WorkingCorner.AUTOMATIC, WorkingCorner.NORTHEAST.location());
        assertEquals(WorkingCorner.NORTHEAST, tracker.select(WorkingCorner.SOUTHWEST.location(), WorkingCorner.AUTOMATIC, null));
        for (int i = 0; i < 20; i++) { tracker.select(WorkingCorner.SOUTHWEST.location(), WorkingCorner.AUTOMATIC, null); }
        assertEquals(WorkingCorner.SOUTHWEST, tracker.select(WorkingCorner.SOUTHWEST.location(), WorkingCorner.AUTOMATIC, null));
    }
    @Test public void currentClickEstablishesCorner()
    {
        WintertodtObjectTracker tracker = new WintertodtObjectTracker();
        assertEquals(WorkingCorner.NORTHEAST, tracker.select(new WorldPoint(1631, 4005, 0), WorkingCorner.AUTOMATIC,
            WorkingCorner.NORTHEAST.location()));
    }
    @Test public void brazierUsesCurrentImpostorAndDespawnRemovesReference() throws Exception
    {
        Client client = mock(Client.class);
        WintertodtObjectTracker tracker = new WintertodtObjectTracker();
        TrackerTestSupport.inject(tracker, "client", client);
        GameObject brazier = mock(GameObject.class);
        when(brazier.getId()).thenReturn(ObjectID.WINT_BRAZIER);
        when(brazier.getWorldLocation()).thenReturn(WorkingCorner.SOUTHEAST.location());
        ObjectComposition base = mock(ObjectComposition.class), transformed = mock(ObjectComposition.class);
        when(client.getObjectDefinition(ObjectID.WINT_BRAZIER)).thenReturn(base);
        when(base.getImpostorIds()).thenReturn(new int[]{ObjectID.WINT_BRAZIER_LIT});
        when(base.getImpostor()).thenReturn(transformed);
        when(transformed.getId()).thenReturn(ObjectID.WINT_BRAZIER_LIT);
        tracker.select(WorkingCorner.SOUTHEAST.location(), WorkingCorner.AUTOMATIC, null); tracker.add(brazier);
        assertEquals(BrazierState.LIT, tracker.brazier(WorkingCorner.SOUTHEAST.location()));
        when(transformed.getId()).thenReturn(ObjectID.WINT_BRAZIER_BROKEN);
        assertEquals(BrazierState.BROKEN, tracker.brazier(WorkingCorner.SOUTHEAST.location()));
        tracker.remove(brazier);
        assertEquals(BrazierState.UNKNOWN, tracker.brazier(WorkingCorner.SOUTHEAST.location()));
    }
    @Test public void pyromancerUsesTransformedCompositionAndStaysLocal()
    {
        WintertodtNpcTracker tracker = new WintertodtNpcTracker();
        NPC npc = mock(NPC.class); NPCComposition transformed = mock(NPCComposition.class);
        when(npc.getId()).thenReturn(NpcID.WINT_WIZARD);
        when(npc.getTransformedComposition()).thenReturn(transformed);
        when(transformed.getId()).thenReturn(NpcID.WINT_WIZARD_DOWN);
        when(npc.getWorldLocation()).thenReturn(WorkingCorner.SOUTHWEST.location()); tracker.add(npc);
        assertEquals(NpcID.WINT_WIZARD_DOWN, WintertodtNpcTracker.id(npc));
        assertSame(npc, tracker.target(TargetKind.PYROMANCER, WorkingCorner.SOUTHWEST.location()));
        assertNull(tracker.target(TargetKind.PYROMANCER, WorkingCorner.NORTHEAST.location()));
        tracker.reset(); assertNull(tracker.target(TargetKind.PYROMANCER, WorkingCorner.SOUTHWEST.location()));
    }
}
