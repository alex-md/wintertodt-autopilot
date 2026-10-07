package com.wintertodtautopilot;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WintertodtStateTrackerTest
{
    private Client client;
    private Player player;
    private WintertodtStateTracker tracker;
    private WintertodtInventoryTracker inventory;
    private WintertodtRoundTracker rounds;
    private int roots = 10, kindling, points;
    private RoundState round = RoundState.ACTIVE;
    @Before public void setup() throws Exception
    {
        client = mock(Client.class); player = mock(Player.class); tracker = new WintertodtStateTracker();
        inventory = mock(WintertodtInventoryTracker.class);
        WintertodtObjectTracker objects = mock(WintertodtObjectTracker.class);
        WintertodtNpcTracker npcs = mock(WintertodtNpcTracker.class);
        rounds = mock(WintertodtRoundTracker.class);
        TrackerTestSupport.inject(tracker, "client", client); TrackerTestSupport.inject(tracker, "inventory", inventory);
        TrackerTestSupport.inject(tracker, "objects", objects); TrackerTestSupport.inject(tracker, "npcs", npcs);
        TrackerTestSupport.inject(tracker, "rounds", rounds);
        when(client.getLocalPlayer()).thenReturn(player);
        when(client.getIntStack()).thenReturn(new int[]{2});
        when(player.getWorldLocation()).thenReturn(WorkingCorner.SOUTHEAST.location());
        when(player.getAnimation()).thenReturn(-1);
        when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(785);
        when(objects.select(any(), any(), any())).thenReturn(WorkingCorner.SOUTHEAST);
        when(objects.brazier(any())).thenReturn(BrazierState.LIT);
        doAnswer(invocation -> {
            WintertodtSnapshot.WintertodtSnapshotBuilder b = invocation.getArgument(0);
            b.brumaRoots(roots).brumaKindling(kindling).freeSlots(28-roots-kindling).hasKnife(true).hasAxe(true);
            return null;
        }).when(inventory).fill(any());
        doAnswer(invocation -> {
            WintertodtSnapshot.WintertodtSnapshotBuilder b = invocation.getArgument(0);
            b.roundState(round).wintertodtEnergy(80).points(points);
            return null;
        }).when(rounds).fill(any(), anyBoolean(), anyInt());
    }
    private WintertodtSnapshot tick(int n)
    {
        when(client.getTickCount()).thenReturn(n); return tracker.snapshot(WorkingCorner.AUTOMATIC);
    }
    private void chat(String message)
    {
        ChatMessage e = mock(ChatMessage.class);
        when(e.getType()).thenReturn(ChatMessageType.GAMEMESSAGE); when(e.getMessage()).thenReturn(message);
        tracker.chat(e);
    }
    @Test public void warmthUsesTenthsAndIdleHasFiveTickGrace()
    {
        assertEquals(78, tick(10).getWarmth());
        assertFalse(tick(14).isPlayerIdle()); assertTrue(tick(15).isPlayerIdle());
    }
    @Test public void preparationAreaInsidePrisonIsNotAnOutsideLobby()
    {
        when(player.getWorldLocation()).thenReturn(new WorldPoint(1630, 3975, 0));
        WintertodtSnapshot s = tick(10);
        verify(rounds).fill(any(), eq(true), anyInt());
        assertTrue(s.active());
        assertEquals(78, s.getWarmth());
    }
    @Test public void movementPreventsIdleAndResetsAttentionGrace()
    {
        tick(10);
        when(player.getWorldLocation()).thenReturn(new WorldPoint(1638, 3998, 0));
        assertEquals(PlayerActivity.MOVING, tick(20).getCurrentActivity());
        assertFalse(tick(21).isPlayerIdle()); assertTrue(tick(25).isPlayerIdle());
    }
    @Test public void animationGapsAreNotImmediateIdle()
    {
        when(player.getAnimation()).thenReturn(AnimationID.HUMAN_FLETCHING);
        assertEquals(PlayerActivity.FLETCHING, tick(10).getCurrentActivity());
        when(player.getAnimation()).thenReturn(-1);
        assertEquals(PlayerActivity.FLETCHING, tick(14).getCurrentActivity());
        assertEquals(PlayerActivity.IDLE, tick(15).getCurrentActivity());
    }
    @Test public void coldInterruptsFletchingButNotChopping()
    {
        when(player.getAnimation()).thenReturn(AnimationID.HUMAN_FLETCHING); tick(10);
        chat("The cold of the Wintertodt seeps into your bones."); when(player.getAnimation()).thenReturn(-1);
        assertTrue(tick(12).isInterrupted()); assertTrue(tick(12).isPlayerIdle());
        when(player.getAnimation()).thenReturn(AnimationID.HUMAN_WOODCUTTING_BRONZE_AXE); tick(20);
        chat("The cold of the Wintertodt seeps into your bones.");
        assertFalse(tick(21).isInterrupted()); assertEquals(PlayerActivity.CHOPPING, tick(21).getCurrentActivity());
    }
    @Test public void inventoryProgressDetectsFletchingAndEndsItWhenRootsRunOut()
    {
        tick(10); roots = 9; kindling = 1;
        assertEquals(PlayerActivity.FLETCHING, tick(20).getCurrentActivity());
        roots = 0; kindling = 10;
        assertEquals(PlayerActivity.IDLE, tick(21).getCurrentActivity());
    }
    @Test public void inventoryProgressDetectsFeeding()
    {
        tick(10); roots = 9; points = 10;
        assertEquals(PlayerActivity.FEEDING, tick(11).getCurrentActivity());
        roots = 0; points = 100;
        assertEquals(PlayerActivity.IDLE, tick(12).getCurrentActivity());
    }
    @Test public void completionAndRegionExitClearInterruptions()
    {
        when(player.getAnimation()).thenReturn(AnimationID.HUMAN_FLETCHING); tick(10);
        chat("The brazier has gone out."); when(player.getAnimation()).thenReturn(-1);
        round = RoundState.COMPLETE;
        assertFalse(tick(11).isInterrupted());
        when(player.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
        assertEquals(RoundState.OUTSIDE, tick(12).getRoundState());
        verify(inventory).reset();
    }

    @Test public void staleAnimationEventDoesNotClearColdRestartCue()
    {
        when(player.getAnimation()).thenReturn(AnimationID.HUMAN_FLETCHING); tick(10);
        chat("The cold of the Wintertodt seeps into your bones."); tracker.animation(player);
        assertTrue(tick(11).isInterrupted());
        when(player.getAnimation()).thenReturn(-1); assertTrue(tick(12).isInterrupted());
        when(player.getAnimation()).thenReturn(AnimationID.HUMAN_FLETCHING);
        assertFalse(tick(13).isInterrupted());
    }
}
