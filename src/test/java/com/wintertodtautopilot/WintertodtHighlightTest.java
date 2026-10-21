package com.wintertodtautopilot;

import org.junit.Test;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.*;

public class WintertodtHighlightTest
{
    @Test public void healingLeavesAllInventoryItemsQuietEvenWithPotionsAndFuelInTheObjective()
    {
        int[] potions = {ItemID.WINT_POTION1, ItemID.WINT_POTION2, ItemID.WINT_POTION3, ItemID.WINT_POTION4};
        for (int potion : potions)
        {
            WintertodtObjective heal = new WintertodtObjective(ObjectiveType.HEAL_PYROMANCER, "Heal", "local_pyromancer_down",
                TargetKind.PYROMANCER, 1, false, potion, ItemID.WINT_BRUMA_ROOT, ItemID.WINT_BRUMA_KINDLING, ItemID.WINT_VIAL);
            for (PlayerActivity activity : new PlayerActivity[]{PlayerActivity.IDLE, PlayerActivity.FEEDING, PlayerActivity.FLETCHING, PlayerActivity.HEALING})
            {
                WintertodtSnapshot s = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).roundState(RoundState.ACTIVE)
                    .brumaRoots(5).brumaKindling(5).currentActivity(activity).build();
                assertFalse(WintertodtHighlight.inventoryItem(s, heal, potion));
                assertFalse(WintertodtHighlight.inventoryItem(s, heal, ItemID.WINT_BRUMA_ROOT));
                assertFalse(WintertodtHighlight.inventoryItem(s, heal, ItemID.WINT_BRUMA_KINDLING));
                assertFalse(WintertodtHighlight.inventoryItem(s, heal, ItemID.WINT_VIAL));
            }
        }
    }

    @Test public void inventoryBorderWidthStaysFixedWhilePulsingAndCanBeAdjusted()
    {
        assertEquals(1, new WintertodtAutopilotConfig() { }.highlightBorderWidth());
        int thin = paintedBorderPixels(1, 1);
        assertEquals(thin, paintedBorderPixels(1, .25f));
        assertTrue(paintedBorderPixels(4, 1) > thin);
        assertEquals(thin, paintedBorderPixels(0, 1));
        assertEquals(paintedBorderPixels(4, 1), paintedBorderPixels(100, 1));
    }

    private int paintedBorderPixels(int width, float strength)
    {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(80, 80, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        WintertodtHighlight.inventory(graphics, new java.awt.Rectangle(20, 20, 40, 40), java.awt.Color.ORANGE, strength, width);
        graphics.dispose();
        int count = 0;
        // Count pixels outside the item bounds, excluding the interior fill.
        for (int y = 0; y < 20; y++)
        {
            if ((image.getRGB(40, y) >>> 24) != 0) { count++; }
        }
        return count;
    }

    @Test public void interruptionCuePersistsUntilResumingAndExcludesMovementAndWaiting()
    {
        WintertodtSnapshot s = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).roundState(RoundState.ACTIVE).interrupted(true).build();
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
        WintertodtSnapshot s = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).roundState(RoundState.ACTIVE).currentActivity(PlayerActivity.FEEDING).build();
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
        WintertodtSnapshot s = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).roundState(RoundState.ACTIVE).currentActivity(PlayerActivity.CHOPPING).build();
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
        WintertodtSnapshot s = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).roundState(RoundState.ACTIVE).build();
        WintertodtObjective blocked = new WintertodtObjective(ObjectiveType.WAIT, "Make space", "inventory_blocked", TargetKind.NONE, 0, false);
        assertTrue(WintertodtHighlight.needsAttention(s, blocked));
        assertFalse(WintertodtHighlight.needsAttention(s, new WintertodtObjective(ObjectiveType.WAIT, "Wait", "countdown", TargetKind.NONE, 0, false)));
    }


    @Test public void relightingHighlightsOnlyTheToolWhileTheBrazierPulses()
    {
        WintertodtSnapshot feeding = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).roundState(RoundState.ACTIVE)
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
        WintertodtSnapshot stopped = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).roundState(RoundState.ACTIVE)
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
        WintertodtSnapshot idle = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).roundState(RoundState.ACTIVE)
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

    @Test public void theSamePotionOnlyGetsAnInventoryCueForPersonalWarmth()
    {
        WintertodtSnapshot full = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).roundState(RoundState.ACTIVE)
            .warmth(100).currentActivity(PlayerActivity.IDLE).build();
        WintertodtObjective heal = new WintertodtObjective(ObjectiveType.HEAL_PYROMANCER, "Heal the pyromancer",
            "solo_heal", TargetKind.PYROMANCER, 1, false, ItemID.WINT_POTION4);
        WintertodtObjective drink = new WintertodtObjective(ObjectiveType.RESTORE_WARMTH, "Drink rejuvenation potion",
            "warmth_low", TargetKind.NONE, 1, true, ItemID.WINT_POTION4);
        assertFalse(WintertodtHighlight.inventoryItem(full, heal, ItemID.WINT_POTION4));
        assertTrue(WintertodtHighlight.needsAttention(full, heal)); // The NPC still pulses.
        assertEquals("Click the highlighted pyromancer to heal them.", WintertodtGuidance.reassurance(full, heal));
        WintertodtSnapshot cold = full.toBuilder().warmth(15).build();
        assertTrue(WintertodtHighlight.inventoryItem(cold, drink, ItemID.WINT_POTION4));
        assertFalse(WintertodtHighlight.inventoryItem(full, heal, ItemID.WINT_POTION4));
        assertFalse(WintertodtHighlight.needsAttention(full.toBuilder().currentActivity(PlayerActivity.HEALING).build(), heal));
    }

}
