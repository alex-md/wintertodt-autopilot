package com.wintertodtautopilot;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.wintertodtautopilot.WintertodtSnapshot.ActionState.*;
import static com.wintertodtautopilot.WintertodtEffortPolicyTest.*;

public class WintertodtActionTrackerTest
{
    private final WintertodtActionTracker tracker = new WintertodtActionTracker();
    @Test public void failedClickExpiresWithoutSuppressingGuidanceOrProvingSuccess()
    {
        WintertodtSnapshot s = scene(10, 25, 24);
        tracker.request(PlayerActivity.HEALING, WorkingCorner.SOUTHWEST, s, 10);
        WintertodtSnapshot pending = tracker.observe(s, scene(11, 25, 24), PlayerActivity.IDLE);
        assertEquals(REQUESTED, pending.getActionState());
        WintertodtObjective heal = new WintertodtObjective(ObjectiveType.HEAL_PYROMANCER, "Heal", "solo_heal", TargetKind.PYROMANCER, 1, false);
        assertFalse(WintertodtGuidance.performing(pending, heal));
        for (int t = 12; t <= 14; t++)
        {
            WintertodtSnapshot next = tracker.observe(pending, scene(t, 25, 24), PlayerActivity.IDLE);
            pending = next;
        }
        assertEquals(UNCONFIRMED, pending.getActionState());
        assertEquals(0, pending.getHealingDosesUsed());
        assertEquals(NONE, tracker.observe(pending, scene(15, 25, 24), PlayerActivity.IDLE).getActionState());
    }
    @Test public void observedAnimationIsProgressAndInventoryTransformationConfirmsFletching()
    {
        WintertodtSnapshot a = scene(10, 25, 24).toBuilder().brumaRoots(6).build();
        tracker.request(PlayerActivity.FLETCHING, WorkingCorner.AUTOMATIC, a, 10);
        WintertodtSnapshot b = tracker.observe(a, a.toBuilder().observationTick(11).build(), PlayerActivity.FLETCHING);
        assertEquals(IN_PROGRESS, b.getActionState());
        WintertodtSnapshot c = tracker.observe(b, b.toBuilder().observationTick(12).brumaRoots(5).brumaKindling(1)
            .actionState(NONE).build(), PlayerActivity.IDLE);
        assertEquals(CONFIRMED, c.getActionState()); assertEquals(PlayerActivity.FLETCHING, c.getActionActivity());
    }
    @Test public void confirmedFeedingRequiresEarnedScoreAndFuelLossTogether()
    {
        WintertodtSnapshot a = scene(10, 25, 24).toBuilder().brumaRoots(6).build();
        WintertodtSnapshot dropped = tracker.observe(a, scene(11, 25, 24).toBuilder().brumaRoots(5).build(), PlayerActivity.IDLE);
        assertEquals(NONE, dropped.getActionState());
        WintertodtSnapshot fed = tracker.observe(dropped, scene(12, 25, 24).toBuilder().brumaRoots(4).points(110).build(), PlayerActivity.IDLE);
        assertEquals(CONFIRMED, fed.getActionState()); assertEquals(PlayerActivity.FEEDING, fed.getActionActivity());
    }
    @Test public void mixingNeedsBothIngredientsAndAnIncreaseInFinishedDoses()
    {
        WintertodtSnapshot a = scene(10, 25, 0).toBuilder().unfinishedPotionCount(6).brumaHerbCount(6).build();
        WintertodtSnapshot mixed = tracker.observe(a, scene(11, 25, 4).toBuilder().unfinishedPotionCount(5).brumaHerbCount(5).build(), PlayerActivity.IDLE);
        assertEquals(PlayerActivity.MAKING_POTION, mixed.getActionActivity()); assertEquals(CONFIRMED, mixed.getActionState());
        assertEquals(0, mixed.getPersonalDosesUsed());
    }
    @Test public void repairsAndLightingNeedTheRequestedCornerToChange()
    {
        WintertodtSnapshot a = corner(scene(10, 25, 24), WorkingCorner.SOUTHWEST, BrazierState.BROKEN,
            CornerObservation.PyromancerState.ALIVE, -1, -1);
        tracker.request(PlayerActivity.REPAIRING, WorkingCorner.SOUTHWEST, a, 10);
        WintertodtSnapshot b = tracker.observe(a, corner(scene(11, 25, 24), WorkingCorner.SOUTHWEST, BrazierState.UNLIT,
            CornerObservation.PyromancerState.ALIVE, -1, -1), PlayerActivity.IDLE);
        assertEquals(CONFIRMED, b.getActionState()); assertEquals(PlayerActivity.REPAIRING, b.getActionActivity());
        tracker.request(PlayerActivity.LIGHTING, WorkingCorner.SOUTHWEST, b, 11);
        WintertodtSnapshot other = corner(b.toBuilder().observationTick(12).build(), WorkingCorner.NORTHWEST,
            BrazierState.LIT, CornerObservation.PyromancerState.ALIVE, -1, -1);
        WintertodtSnapshot waiting = tracker.observe(b, other, PlayerActivity.IDLE);
        assertNotEquals(CONFIRMED, waiting.getActionState());
        WintertodtSnapshot lit = tracker.observe(waiting, scene(13, 25, 24), PlayerActivity.IDLE);
        assertEquals(CONFIRMED, lit.getActionState()); assertEquals(PlayerActivity.LIGHTING, lit.getActionActivity());
    }
    @Test public void healingCanMatchDelayedStateButNeedsOneDoseAndFreshTargetEvidence()
    {
        WintertodtSnapshot down = corner(scene(10, 25, 24), WorkingCorner.SOUTHWEST, BrazierState.UNLIT,
            CornerObservation.PyromancerState.INCAPACITATED, 0, 10);
        tracker.request(PlayerActivity.HEALING, WorkingCorner.SOUTHWEST, down, 10);
        WintertodtSnapshot spent = tracker.observe(down, corner(scene(11, 25, 23), WorkingCorner.SOUTHWEST, BrazierState.UNLIT,
            CornerObservation.PyromancerState.INCAPACITATED, 0, 11), PlayerActivity.IDLE);
        assertEquals(0, spent.getHealingDosesUsed());
        WintertodtSnapshot healed = tracker.observe(spent, scene(12, 25, 23), PlayerActivity.IDLE);
        assertEquals(CONFIRMED, healed.getActionState()); assertEquals(1, healed.getHealingDosesUsed());
        assertEquals(0, healed.getPersonalDosesUsed());
        assertEquals(0, tracker.observe(healed, scene(13, 25, 23), PlayerActivity.IDLE).getHealingDosesUsed());
    }
    @Test public void unrelatedDoseLossOrOtherPlayersHealingNeverCountsAsPersonalUse()
    {
        WintertodtSnapshot a = scene(10, 25, 24);
        assertEquals(0, tracker.observe(a, scene(11, 25, 23), PlayerActivity.IDLE).getPersonalDosesUsed());
        tracker.request(PlayerActivity.HEALING, WorkingCorner.SOUTHWEST, a, 10);
        assertNotEquals(CONFIRMED, tracker.observe(a, scene(11, 25, 24), PlayerActivity.IDLE).getActionState());
    }
    @Test public void confirmedPersonalPotionUseIsAttributedOnceAndFoodIsNotADose()
    {
        WintertodtSnapshot a = scene(10, 25, 24);
        tracker.request(PlayerActivity.RESTORING_WARMTH, WorkingCorner.AUTOMATIC, a, 10);
        WintertodtSnapshot sip = tracker.observe(a, scene(11, 25, 23).toBuilder()
            .warmthAction(WintertodtSnapshot.WarmthAction.CONFIRMED).build(), PlayerActivity.IDLE);
        assertEquals(1, sip.getPersonalDosesUsed()); assertEquals(0, sip.getHealingDosesUsed());
        assertEquals(0, tracker.observe(sip, scene(12, 25, 23), PlayerActivity.IDLE).getPersonalDosesUsed());
        tracker.request(PlayerActivity.RESTORING_WARMTH, WorkingCorner.AUTOMATIC, sip, 11);
        assertEquals(0, tracker.observe(sip, scene(12, 25, 23).toBuilder()
            .warmthAction(WintertodtSnapshot.WarmthAction.CONFIRMED).build(), PlayerActivity.IDLE).getPersonalDosesUsed());
    }
    @Test public void gapsDuplicateTicksAndInterruptedRequestsCannotConfirmOldActions()
    {
        WintertodtSnapshot a = scene(10, 25, 24);
        tracker.request(PlayerActivity.HEALING, WorkingCorner.SOUTHWEST, a, 10);
        assertEquals(NONE, tracker.observe(a, a, PlayerActivity.IDLE).getActionState());
        assertEquals(NONE, tracker.observe(a, scene(20, 25, 23), PlayerActivity.IDLE).getActionState());
        tracker.request(PlayerActivity.FLETCHING, WorkingCorner.AUTOMATIC, scene(20, 25, 23), 20);
        assertEquals(INTERRUPTED, tracker.observe(scene(20, 25, 23), scene(21, 25, 23).toBuilder().interrupted(true).build(), PlayerActivity.IDLE).getActionState());
        assertEquals(NONE, tracker.observe(scene(21, 25, 23), scene(22, 25, 23), PlayerActivity.IDLE).getActionState());
    }

    @Test public void unknownInventoryCannotCarryAConsumedDoseIntoALaterHealingConfirmation()
    {
        WintertodtSnapshot down = corner(scene(10, 25, 24), WorkingCorner.SOUTHWEST, BrazierState.UNLIT,
            CornerObservation.PyromancerState.INCAPACITATED, 0, 10);
        tracker.request(PlayerActivity.HEALING, WorkingCorner.SOUTHWEST, down, 10);
        WintertodtSnapshot missing = tracker.observe(down, scene(11, 25, 0).toBuilder().inventoryKnown(false).build(), PlayerActivity.IDLE);
        WintertodtSnapshot alive = tracker.observe(missing, scene(12, 25, 23), PlayerActivity.IDLE);
        assertEquals(NONE, alive.getActionState()); assertEquals(0, alive.getHealingDosesUsed());
    }

    @Test public void aHealingScoreIncreaseAndDroppedFuelDoNotLookLikeConfirmedFeeding()
    {
        WintertodtSnapshot a = scene(10, 25, 24).toBuilder().brumaRoots(6).build();
        WintertodtSnapshot b = scene(11, 25, 23).toBuilder().brumaRoots(5).points(175).build();
        assertNotEquals(CONFIRMED, tracker.observe(a, b, PlayerActivity.IDLE).getActionState());
    }
}
