package com.wintertodtautopilot;

import org.junit.Test;
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

}
