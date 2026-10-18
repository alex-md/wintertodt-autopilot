package com.wintertodtautopilot;

import org.junit.Test;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.*;

public class WintertodtHighlightTest
{
    @Test public void interruptionCuePersistsUntilResumingAndExcludesMovementAndWaiting()
    {
        WintertodtSnapshot s = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).interrupted(true).build();
        WintertodtObjective o = new WintertodtObjective(ObjectiveType.FLETCH, "Continue fletching", "batch_fletch", TargetKind.NONE, 12, false);
        assertTrue(WintertodtHighlight.needsRestart(s, o));
        assertFalse(WintertodtHighlight.needsRestart(s.toBuilder().currentActivity(PlayerActivity.FLETCHING).build(), o));
        assertFalse(WintertodtHighlight.needsRestart(s.toBuilder().currentActivity(PlayerActivity.MOVING).build(), o));
        assertFalse(WintertodtHighlight.needsRestart(s.toBuilder().roundState(RoundState.COMPLETE).build(), o));
        assertFalse(WintertodtHighlight.needsRestart(s,
            new WintertodtObjective(ObjectiveType.WAIT, "Wait", "countdown", TargetKind.NONE, 0, false)));
    }
    @Test public void extinguishedBrazierPulsesWithoutChatOrIdleDetection()
    {
        WintertodtSnapshot s = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).currentActivity(PlayerActivity.FEEDING).build();
        WintertodtObjective light = new WintertodtObjective(ObjectiveType.LIGHT, "Relight", "brazier_unlit", TargetKind.BRAZIER, 0, false);
        assertTrue(WintertodtHighlight.needsAttention(s, light));
        float bright = WintertodtHighlight.strengthAt(s, light, true, 300_000_000L);
        float dim = WintertodtHighlight.strengthAt(s, light, true, 900_000_000L);
        assertEquals(1f, bright, .001f); assertEquals(.25f, dim, .001f);
        assertEquals(1f, WintertodtHighlight.strengthAt(s, light, false, 900_000_000L), 0);
        assertFalse(WintertodtHighlight.needsAttention(s.toBuilder().currentActivity(PlayerActivity.LIGHTING).build(), light));
        assertFalse(WintertodtHighlight.needsAttention(s.toBuilder().currentActivity(PlayerActivity.MOVING).build(), light));
    }
    @Test public void warmthAndUrgentFeedDemandAttentionUntilTheirActionStarts()
    {
        WintertodtSnapshot s = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).currentActivity(PlayerActivity.CHOPPING).build();
        WintertodtObjective drink = new WintertodtObjective(ObjectiveType.RESTORE_WARMTH, "Drink", "warmth_low", TargetKind.NONE, 1, true);
        assertTrue(WintertodtHighlight.needsAttention(s, drink));
        assertTrue(WintertodtHighlight.needsAttention(s.toBuilder().currentActivity(PlayerActivity.MOVING).build(), drink));
        assertFalse(WintertodtHighlight.needsAttention(s.toBuilder().currentActivity(PlayerActivity.RESTORING_WARMTH).build(), drink));
        WintertodtObjective feed = new WintertodtObjective(ObjectiveType.FEED, "Feed now", "finish_feed", TargetKind.BRAZIER, 10, true);
        assertTrue(WintertodtHighlight.needsAttention(s, feed));
        assertFalse(WintertodtHighlight.needsAttention(s.toBuilder().currentActivity(PlayerActivity.FEEDING).build(), feed));
    }

    @Test public void blockedInventoryStillNeedsAttentionEvenWithoutSceneTarget()
    {
        WintertodtSnapshot s = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).build();
        WintertodtObjective blocked = new WintertodtObjective(ObjectiveType.WAIT, "Make space", "inventory_blocked", TargetKind.NONE, 0, false);
        assertTrue(WintertodtHighlight.needsAttention(s, blocked));
        assertFalse(WintertodtHighlight.needsAttention(s, new WintertodtObjective(ObjectiveType.WAIT, "Wait", "countdown", TargetKind.NONE, 0, false)));
    }


    @Test public void relightingHighlightsOnlyTheToolWhileTheBrazierPulses()
    {
        WintertodtSnapshot feeding = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE)
            .localBrazierState(BrazierState.LIT).brumaRoots(5).brumaKindling(4)
            .currentActivity(PlayerActivity.FEEDING).build();
        WintertodtObjective feed = new WintertodtObjective(ObjectiveType.FEED, "Feed", "batch_feed",
            TargetKind.BRAZIER, 9, false, ItemID.WINT_BRUMA_KINDLING);
        assertFalse(WintertodtHighlight.inventoryItem(feeding, feed, ItemID.WINT_BRUMA_KINDLING));
        WintertodtSnapshot stopped = feeding.toBuilder().localBrazierState(BrazierState.UNLIT).build();
        WintertodtObjective light = new WintertodtObjective(ObjectiveType.LIGHT, "Relight", "brazier_unlit",
            TargetKind.BRAZIER, 0, false, ItemID.TINDERBOX);
        // A changed objective must wake the cue even before a stale feeding animation clears.
        assertFalse(WintertodtHighlight.inventoryItem(stopped, light, ItemID.WINT_BRUMA_ROOT));
        assertFalse(WintertodtHighlight.inventoryItem(stopped, light, ItemID.WINT_BRUMA_KINDLING));
        assertTrue(WintertodtHighlight.inventoryItem(stopped, light, ItemID.TINDERBOX));
        assertFalse(WintertodtHighlight.inventoryItem(stopped, light, ItemID.KNIFE));
        assertEquals(.25f, WintertodtHighlight.strengthAt(stopped, light, true, 900_000_000L), .001f);
        assertEquals(1f, WintertodtHighlight.strengthAt(stopped, light, false, 900_000_000L), 0);
        assertFalse(WintertodtHighlight.inventoryItem(stopped.toBuilder().currentActivity(PlayerActivity.LIGHTING).build(),
            light, ItemID.WINT_BRUMA_ROOT));
        assertFalse(WintertodtHighlight.inventoryItem(feeding, feed, ItemID.WINT_BRUMA_KINDLING));
        assertTrue(WintertodtHighlight.inventoryItem(feeding.toBuilder().currentActivity(PlayerActivity.IDLE)
            .interrupted(true).build(), feed, ItemID.WINT_BRUMA_KINDLING));
    }

    @Test public void maintenanceAndWarmthHighlightOnlyTheirRelevantItems()
    {
        WintertodtSnapshot stopped = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE)
            .localBrazierState(BrazierState.BROKEN).brumaRoots(5).build();
        WintertodtObjective drink = new WintertodtObjective(ObjectiveType.RESTORE_WARMTH, "Drink", "warmth_low",
            TargetKind.NONE, 1, true, ItemID.WINT_POTION4);
        assertFalse(WintertodtHighlight.inventoryItem(stopped, drink, ItemID.WINT_BRUMA_ROOT));
        assertTrue(WintertodtHighlight.inventoryItem(stopped, drink, ItemID.WINT_POTION4));
        WintertodtObjective repair = new WintertodtObjective(ObjectiveType.REPAIR, "Repair", "brazier_broken",
            TargetKind.BRAZIER, 0, true, ItemID.HAMMER);
        assertFalse(WintertodtHighlight.inventoryItem(stopped, repair, ItemID.WINT_BRUMA_ROOT));
        assertTrue(WintertodtHighlight.inventoryItem(stopped, repair, ItemID.HAMMER));
        assertFalse(WintertodtHighlight.inventoryItem(stopped.toBuilder().brumaRoots(0).build(), repair, ItemID.WINT_BRUMA_ROOT));
    }

    @Test public void fletchingDistinguishesRootsFromEitherKnifeAndSettlesWhenStarted()
    {
        WintertodtSnapshot idle = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE)
            .brumaRoots(5).currentActivity(PlayerActivity.IDLE).build();
        WintertodtObjective fletch = new WintertodtObjective(ObjectiveType.FLETCH, "Fletch roots", "batch_fletch",
            TargetKind.NONE, 5, false, ItemID.KNIFE, ItemID.FLETCHING_KNIFE, ItemID.WINT_BRUMA_ROOT);
        assertNotEquals(WintertodtHighlight.inventoryColor(fletch, ItemID.KNIFE),
            WintertodtHighlight.inventoryColor(fletch, ItemID.WINT_BRUMA_ROOT));
        assertEquals(WintertodtOverlay.color(fletch), WintertodtHighlight.inventoryColor(fletch, ItemID.KNIFE));
        assertEquals(WintertodtHighlight.inventoryColor(fletch, ItemID.KNIFE),
            WintertodtHighlight.inventoryColor(fletch, ItemID.FLETCHING_KNIFE));
        for (int itemId : fletch.getItemIds())
        {
            assertTrue(WintertodtHighlight.inventoryItem(idle, fletch, itemId));
            assertFalse(WintertodtHighlight.inventoryItem(idle.toBuilder()
                .currentActivity(PlayerActivity.FLETCHING).build(), fletch, itemId));
        }
        WintertodtObjective feed = new WintertodtObjective(ObjectiveType.FEED, "Feed", "batch_feed",
            TargetKind.BRAZIER, 5, false, ItemID.WINT_BRUMA_ROOT);
        assertEquals(WintertodtOverlay.color(feed), WintertodtHighlight.inventoryColor(feed, ItemID.WINT_BRUMA_ROOT));
    }

}
