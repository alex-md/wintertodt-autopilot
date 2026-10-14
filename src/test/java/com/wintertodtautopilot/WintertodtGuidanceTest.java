package com.wintertodtautopilot;

import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtGuidanceTest
{
    private final WintertodtSnapshot active = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).build();
    private final WintertodtObjective fletch = new WintertodtObjective(ObjectiveType.FLETCH,
        "Fletch 12 roots", "batch_fletch", TargetKind.NONE, 12, false);

    @Test public void acknowledgesOnlyTheRequestedActivity()
    {
        assertEquals("NEXT ACTION", WintertodtGuidance.status(active, fletch));
        assertEquals("IN PROGRESS", WintertodtGuidance.status(active.toBuilder()
            .currentActivity(PlayerActivity.FLETCHING).interrupted(true).playerIdle(true).build(), fletch));
        assertEquals("RESUME", WintertodtGuidance.status(active.toBuilder().interrupted(true).build(), fletch));
        assertEquals("NEXT ACTION", WintertodtGuidance.status(active.toBuilder()
            .currentActivity(PlayerActivity.CHOPPING).build(), fletch));
    }

    @Test public void safetyOverridesRoutineActivityAndBlockedInventoryIsNotStandby()
    {
        WintertodtObjective drink = new WintertodtObjective(ObjectiveType.RESTORE_WARMTH,
            "Drink", "warmth_low", TargetKind.NONE, 1, true);
        assertEquals("ACT NOW", WintertodtGuidance.status(active.toBuilder()
            .currentActivity(PlayerActivity.FLETCHING).build(), drink));
        WintertodtObjective blocked = new WintertodtObjective(ObjectiveType.WAIT,
            "Make space", "inventory_blocked", TargetKind.NONE, 0, false);
        assertEquals("ACTION NEEDED", WintertodtGuidance.status(active, blocked));
        assertEquals("Free an inventory slot to continue.", WintertodtGuidance.reassurance(active, blocked));
    }

    @Test public void gatheringIngredientsDoesNotClaimMixingIsInProgress()
    {
        WintertodtSnapshot mixing = active.toBuilder().currentActivity(PlayerActivity.MAKING_POTION).build();
        WintertodtObjective gather = new WintertodtObjective(ObjectiveType.MAKE_REJUVENATION,
            "Pick herbs", "prepare_supply", TargetKind.HERBS, 1, false);
        assertFalse(WintertodtGuidance.performing(mixing, gather));
        assertTrue(WintertodtGuidance.performing(mixing, new WintertodtObjective(ObjectiveType.MAKE_REJUVENATION,
            "Combine ingredients", "mix_rejuvenation", TargetKind.NONE, 1, false)));
    }
}
