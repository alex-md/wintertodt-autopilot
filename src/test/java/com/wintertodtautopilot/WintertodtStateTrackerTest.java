package com.wintertodtautopilot;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.MenuOptionClicked;
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
    private int roots = 10, kindling, points, doses = 4;
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
            b.inventoryKnown(true).equipmentKnown(true).brumaRoots(roots).brumaKindling(kindling).freeSlots(28-roots-kindling).hasKnife(true).hasAxe(true)
                .rejuvenationDoses(doses).warmthItem(net.runelite.api.gameval.ItemID.WINT_POTION4);
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
        WintertodtSnapshot first = tick(10);
        assertEquals(78, first.getWarmth());
        assertEquals(10, first.getObservationTick());
        assertEquals(4, first.getCorners().size());
        assertEquals(-1, first.observedGlobalLitBraziers());
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
        WintertodtSnapshot outside = tick(12);
        assertEquals(RoundState.OUTSIDE, outside.getRoundState());
        assertTrue(outside.getCorners().isEmpty());
        assertEquals(-1, outside.getObservationTick());
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
    @Test public void warmthReadingSurvivesMissingRoundHudAndInvalidSignalIsUnknown()
    {
        round = RoundState.COUNTDOWN;
        when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(350);
        WintertodtSnapshot s = tick(10);
        assertEquals(35, s.getWarmth()); assertEquals(350, s.getRawWarmth());
        when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(10000);
        assertEquals(-1, tick(11).getWarmth());
        when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(0);
        assertEquals(-1, tick(12).getWarmth());
        round = RoundState.ACTIVE;
        assertEquals(0, tick(13).getWarmth());
    }

    @Test public void observedWarmthAndPotionDriveARealRestoreDecision()
    {
        when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(450);
        doAnswer(invocation -> {
            WintertodtSnapshot.WintertodtSnapshotBuilder b = invocation.getArgument(0);
            b.inventoryKnown(true).equipmentKnown(true).brumaRoots(10).hasAxe(true).rejuvenationDoses(4)
                .warmthItem(net.runelite.api.gameval.ItemID.WINT_POTION4).potionItem(net.runelite.api.gameval.ItemID.WINT_POTION4);
            return null;
        }).when(inventory).fill(any());
        when(player.getAnimation()).thenReturn(AnimationID.HUMAN_WOODCUTTING_BRONZE_AXE);
        WintertodtSnapshot s = tick(10);
        assertEquals(45, s.getWarmth());
        WintertodtObjective o = new WintertodtDecisionEngine().decide(s, PlannerSettings.from(new WintertodtAutopilotConfig() { }));
        assertEquals(ObjectiveType.RESTORE_WARMTH, o.getType());
        assertEquals("Drink rejuvenation potion", o.getInstruction());
        assertTrue(WintertodtHighlight.needsAttention(s, o));
    }

    @Test public void runEnergyUsesHundredthsAndInvalidSignalIsUnknown()
    {
        when(client.getEnergy()).thenReturn(1734);
        assertEquals(17, tick(10).getRunEnergyPercent());
        when(client.getEnergy()).thenReturn(10001);
        assertEquals(-1, tick(11).getRunEnergyPercent());
    }

    private void click(String option, String target, int itemId)
    {
        MenuOptionClicked e = mock(MenuOptionClicked.class);
        when(e.getMenuOption()).thenReturn(option); when(e.getMenuTarget()).thenReturn(target);
        when(e.getItemId()).thenReturn(itemId);
        tracker.clicked(e);
    }

    @Test public void failedPotionClickNeverReleasesWarmthPriorityAndPendingIntentExpires()
    {
        when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(180);
        when(player.getAnimation()).thenReturn(AnimationID.HUMAN_WOODCUTTING_BRONZE_AXE);
        tick(10);
        click("Drink", "Rejuvenation potion (4)", net.runelite.api.gameval.ItemID.WINT_POTION4);
        WintertodtSnapshot requested = tick(11);
        assertEquals(WintertodtSnapshot.WarmthAction.REQUESTED, requested.getWarmthAction());
        assertNotEquals(PlayerActivity.RESTORING_WARMTH, requested.getCurrentActivity());
        PlannerSettings settings = PlannerSettings.from(new WintertodtAutopilotConfig() { });
        assertEquals(ObjectiveType.RESTORE_WARMTH, new WintertodtDecisionEngine().decide(requested, settings).getType());
        assertEquals("warmth_low", new WintertodtDecisionEngine().decide(requested, settings).getReason());
        WintertodtSnapshot unconfirmed = tick(12);
        assertEquals(WintertodtSnapshot.WarmthAction.UNCONFIRMED, unconfirmed.getWarmthAction());
        assertEquals(ObjectiveType.RESTORE_WARMTH, new WintertodtSoloEngine().decide(unconfirmed, settings).getType());
    }

    @Test public void potionConsumptionAndWarmthRiseConfirmRestorationWithoutSuppressingRemainingLowWarmth()
    {
        final int[] doses = {4};
        doAnswer(i -> {
            WintertodtSnapshot.WintertodtSnapshotBuilder b = i.getArgument(0);
            b.inventoryKnown(true).equipmentKnown(true).rejuvenationDoses(doses[0])
                .warmthItem(net.runelite.api.gameval.ItemID.WINT_POTION4);
            return null;
        }).when(inventory).fill(any());
        when(inventory.changed()).thenReturn(true);
        when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(100);
        tick(10);
        click("Drink", "Rejuvenation potion (4)", net.runelite.api.gameval.ItemID.WINT_POTION4);
        doses[0] = 3;
        when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(400);
        WintertodtSnapshot confirmed = tick(11);
        assertEquals(WintertodtSnapshot.WarmthAction.CONFIRMED, confirmed.getWarmthAction());
        assertEquals(PlayerActivity.RESTORING_WARMTH, confirmed.getCurrentActivity());
        assertEquals(ObjectiveType.RESTORE_WARMTH, WintertodtSafety.objective(confirmed,
            PlannerSettings.from(new WintertodtAutopilotConfig() { })).getType());
        assertNotEquals(PlayerActivity.RESTORING_WARMTH, tick(12).getCurrentActivity());
    }

    @Test public void naturalRegenerationAndOtherInventoryChangesAreNotPersonalPotionConfirmation()
    {
        when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(400);
        when(inventory.changed()).thenReturn(true);
        tick(10); roots++;
        when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(520);
        WintertodtSnapshot regenerated = tick(11);
        assertNotEquals(PlayerActivity.RESTORING_WARMTH, regenerated.getCurrentActivity());
        assertEquals(WintertodtSnapshot.WarmthAction.NONE, regenerated.getWarmthAction());
    }

    @Test public void partialFoodTransformationCanConfirmRestoration()
    {
        when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(180);
        tick(10);
        click("Eat", "Cake", net.runelite.api.gameval.ItemID.CAKE);
        when(inventory.changed()).thenReturn(true);
        when(inventory.quantityDecreased(net.runelite.api.gameval.ItemID.CAKE)).thenReturn(true);
        when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(530);
        assertEquals(WintertodtSnapshot.WarmthAction.CONFIRMED, tick(11).getWarmthAction());
    }

    @Test public void healingAPyromancerAndNaturalRegenerationCannotConfirmAnEarlierDrinkRequest()
    {
        when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(180);
        when(inventory.changed()).thenReturn(true);
        tick(10);
        click("Drink", "Rejuvenation potion (4)", net.runelite.api.gameval.ItemID.WINT_POTION4);
        click("Help", "Pyromancer", -1);
        doses = 3;
        when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(300);
        assertEquals(WintertodtSnapshot.WarmthAction.NONE, tick(11).getWarmthAction());
    }


    @Test public void aHealingMenuClickIsOnlyARequestAndCannotSuppressTheTarget()
    {
        tick(10); click("Help", "Pyromancer", -1);
        WintertodtSnapshot requested = tick(11);
        assertEquals(WintertodtSnapshot.ActionState.REQUESTED, requested.getActionState());
        assertNotEquals(PlayerActivity.HEALING, requested.getCurrentActivity());
        assertEquals(0, requested.getHealingDosesUsed());
        tick(12); tick(13);
        assertEquals(WintertodtSnapshot.ActionState.UNCONFIRMED, tick(14).getActionState());
    }
}
