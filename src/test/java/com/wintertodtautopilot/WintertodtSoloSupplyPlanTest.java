package com.wintertodtautopilot;

import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtSoloSupplyPlanTest
{
    private final WintertodtSoloSupplyPlan plan = new WintertodtSoloSupplyPlan();
    private final PlannerSettings points = new PlannerSettings(WintertodtStrategy.MASS_POINTS,
        PointGoal.MINIMUM_500, FletchMode.AUTOMATIC, 50, 20);
    private final PlannerSettings xp = new PlannerSettings(WintertodtStrategy.MASS_XP,
        PointGoal.MINIMUM_500, FletchMode.AUTOMATIC, 50, 20);
    private WintertodtSnapshot supplies(int doses)
    {
        return WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).hasAxe(true).hasHammer(true).hasTinderbox(true).hasKnife(true)
            .canMixPotion(true).freeSlots(20).rejuvenationDoses(doses).build();
    }
    @Test public void openingPreparesSixBottlesAndLeavesRoomForHerbs()
    {
        WintertodtObjective o = plan.prepare(supplies(0), points, SoloPhase.PREPARE);
        assertEquals(TargetKind.POTION_CRATE, o.getTarget()); assertEquals(6, o.getQuantity());
        assertEquals(24, plan.getTarget());
        o = plan.prepare(supplies(0).toBuilder().freeSlots(2).build(), points, SoloPhase.PREPARE);
        assertEquals(1, o.getQuantity());
        assertNull(plan.prepare(supplies(24), points, SoloPhase.PREPARE));
    }
    @Test public void pointsRestocksBeforeHealingExhaustsItsSpareBottles()
    {
        assertNull(plan.prepare(supplies(24), points, SoloPhase.SUSTAIN));
        assertNull(plan.prepare(supplies(8), points, SoloPhase.SUSTAIN));
        assertNotNull(plan.prepare(supplies(7), points, SoloPhase.SUSTAIN));
        assertEquals(24, plan.getTarget());
        assertNotNull(plan.prepare(supplies(12), points, SoloPhase.SUSTAIN)); // Complete the ongoing trip.
        assertNull(plan.prepare(supplies(24), points, SoloPhase.SUSTAIN));
    }
    @Test public void xpConservesDosesAndFinishingKeepsTwoBottles()
    {
        assertNull(plan.prepare(supplies(24), xp, SoloPhase.SUSTAIN));
        assertNull(plan.prepare(supplies(7), xp, SoloPhase.SUSTAIN));
        assertNotNull(plan.prepare(supplies(3), xp, SoloPhase.SUSTAIN));
        assertEquals(24, plan.getTarget());
        assertNotNull(plan.prepare(supplies(3), xp, SoloPhase.FINISH));
        assertEquals(8, plan.getTarget());
        assertNull(plan.prepare(supplies(8), xp, SoloPhase.FINISH));
    }
    @Test public void routineRestockWaitsUntilCarriedFuelIsFed()
    {
        assertNull(plan.prepare(supplies(24), points, SoloPhase.SUSTAIN));
        WintertodtSnapshot working = supplies(7).toBuilder().roundState(RoundState.ACTIVE)
            .brumaRoots(5).build();
        assertNull(plan.prepare(working, points, SoloPhase.SUSTAIN));
        assertNull(plan.prepare(working.toBuilder().brumaRoots(0).brumaKindling(5).build(), points, SoloPhase.SUSTAIN));
        assertNull(plan.prepare(working.toBuilder().brumaRoots(1).build(), points, SoloPhase.SUSTAIN));
        assertNotNull(plan.prepare(working.toBuilder().brumaRoots(0).build(), points, SoloPhase.SUSTAIN));
        assertNotNull(plan.prepare(supplies(12), points, SoloPhase.SUSTAIN));
        assertNull(plan.prepare(supplies(24), points, SoloPhase.SUSTAIN));
    }

    @Test public void lastDoseCannotDeferRestockingBehindAFuelBatch()
    {
        assertNull(plan.prepare(supplies(24), points, SoloPhase.SUSTAIN));
        assertNotNull(plan.prepare(supplies(1).toBuilder().roundState(RoundState.ACTIVE)
            .brumaRoots(5).build(), points, SoloPhase.SUSTAIN));
    }

    @Test public void joiningMidRoundAcceptsAUsablePartialReserve()
    {
        assertNull(plan.prepare(supplies(12).toBuilder().roundState(RoundState.ACTIVE).build(), points, SoloPhase.SUSTAIN));
        assertNull(plan.prepare(supplies(11).toBuilder().roundState(RoundState.ACTIVE).build(), points, SoloPhase.SUSTAIN));
        assertNotNull(plan.prepare(supplies(7).toBuilder().roundState(RoundState.ACTIVE).build(), points, SoloPhase.SUSTAIN));
    }

    @Test public void sixBottlePreparationCollectsAllVialsAndHerbsBeforeMixing()
    {
        for (int vials = 0; vials < 6; vials++)
        {
            WintertodtObjective o = plan.prepare(supplies(0).toBuilder().unfinishedPotionCount(vials).freeSlots(20 - vials).build(), points, SoloPhase.PREPARE);
            assertEquals(TargetKind.POTION_CRATE, o.getTarget()); assertEquals(6 - vials, o.getQuantity());
        }
        for (int herbs = 0; herbs < 6; herbs++)
        {
            WintertodtObjective o = plan.prepare(supplies(0).toBuilder().unfinishedPotionCount(6).brumaHerbCount(herbs).freeSlots(14 - herbs).build(), points, SoloPhase.PREPARE);
            assertEquals(TargetKind.HERBS, o.getTarget()); assertEquals(6 - herbs, o.getQuantity());
        }
        for (int mixed = 0; mixed < 6; mixed++)
        {
            WintertodtObjective o = plan.prepare(supplies(mixed * 4).toBuilder().unfinishedPotionCount(6 - mixed)
                .brumaHerbCount(6 - mixed).freeSlots(8 + mixed).build(), points, SoloPhase.PREPARE);
            assertEquals("mix_rejuvenation", o.getReason()); assertEquals(6 - mixed, o.getQuantity());
        }
        assertNull(plan.prepare(supplies(24), points, SoloPhase.PREPARE));
    }

    @Test public void existingDosesAndIngredientsReduceOnlyTheirOwnDeficits()
    {
        WintertodtSnapshot s = supplies(7).toBuilder().unfinishedPotionCount(2).brumaHerbCount(1).freeSlots(12).build();
        WintertodtObjective o = plan.prepare(s, points, SoloPhase.PREPARE);
        assertEquals(TargetKind.POTION_CRATE, o.getTarget()); assertEquals(3, o.getQuantity());
        o = plan.prepare(s.toBuilder().unfinishedPotionCount(5).freeSlots(9).build(), points, SoloPhase.PREPARE);
        assertEquals(TargetKind.HERBS, o.getTarget()); assertEquals(4, o.getQuantity());
        o = plan.prepare(s.toBuilder().unfinishedPotionCount(5).brumaHerbCount(5).freeSlots(5).build(), points, SoloPhase.PREPARE);
        assertEquals("mix_rejuvenation", o.getReason()); assertEquals(5, o.getQuantity());
        assertNull(plan.prepare(supplies(27), points, SoloPhase.PREPARE)); // Rounded up to full four-dose bottles.
    }

    @Test public void constrainedInventoryUsesFeasibleSubBatchesAndNeverRequestsAnUnpairableVial()
    {
        WintertodtObjective o = plan.prepare(supplies(0).toBuilder().freeSlots(1).build(), points, SoloPhase.PREPARE);
        assertEquals("supply_inventory_blocked", o.getReason());
        o = plan.prepare(supplies(0).toBuilder().freeSlots(1).unfinishedPotionCount(6).build(), points, SoloPhase.PREPARE);
        assertEquals(TargetKind.HERBS, o.getTarget()); assertEquals(1, o.getQuantity());
        o = plan.prepare(supplies(0).toBuilder().freeSlots(0).unfinishedPotionCount(6).brumaHerbCount(1).build(), points, SoloPhase.PREPARE);
        assertEquals("mix_rejuvenation", o.getReason()); assertEquals(1, o.getQuantity());
        o = plan.prepare(supplies(4).toBuilder().freeSlots(1).unfinishedPotionCount(5).build(), points, SoloPhase.PREPARE);
        assertEquals(TargetKind.HERBS, o.getTarget()); assertEquals(1, o.getQuantity());
    }

    @Test public void newRoundPreservesAUsableReserveAndRestocksOnlyAfterItFallsBelowThreshold()
    {
        assertNull(plan.prepare(supplies(12).toBuilder().roundState(RoundState.ACTIVE).build(), points, SoloPhase.SUSTAIN));
        for (int round = 0; round < 3; round++)
        {
            plan.nextRound();
            assertNull(plan.prepare(supplies(12).toBuilder().roundState(RoundState.COUNTDOWN).build(), points, SoloPhase.PREPARE));
        }
        assertNull(plan.prepare(supplies(8), points, SoloPhase.PREPARE));
        WintertodtObjective deficit = plan.prepare(supplies(7), points, SoloPhase.PREPARE);
        assertEquals(TargetKind.POTION_CRATE, deficit.getTarget()); assertEquals(5, deficit.getQuantity());
    }

    @Test public void unusedFullReserveAndSurplusIngredientsNeverCauseExtraPreparation()
    {
        WintertodtSnapshot full = supplies(24).toBuilder().unfinishedPotionCount(3).brumaHerbCount(3).build();
        assertNull(plan.prepare(full, points, SoloPhase.SUSTAIN));
        plan.nextRound();
        assertNull(plan.prepare(full, points, SoloPhase.PREPARE));
    }

    @Test public void interruptedMixingAndFinishReserveSurviveTheEncounterBoundary()
    {
        WintertodtSnapshot ingredients = supplies(0).toBuilder().unfinishedPotionCount(6).brumaHerbCount(6).freeSlots(0).build();
        assertEquals(6, plan.prepare(ingredients, points, SoloPhase.PREPARE).getQuantity());
        plan.nextRound();
        WintertodtObjective remainder = plan.prepare(supplies(4).toBuilder().unfinishedPotionCount(5).brumaHerbCount(5).freeSlots(1).build(), points, SoloPhase.PREPARE);
        assertEquals("mix_rejuvenation", remainder.getReason()); assertEquals(5, remainder.getQuantity());
        assertNull(plan.prepare(supplies(24), points, SoloPhase.SUSTAIN));
        assertNull(plan.prepare(supplies(8), points, SoloPhase.FINISH));
        plan.nextRound();
        assertNull(plan.prepare(supplies(8), points, SoloPhase.PREPARE));
        assertEquals(24, plan.getTarget());
    }

    @Test public void simulatedPreparationCompletesAcrossCapacityLimitsAndRoundInterruptions()
    {
        for (int capacity = 7; capacity <= 20; capacity++)
        {
            plan.reset(); int vials = 0, herbs = 0, doses = 0, free = capacity;
            for (int action = 0; action < 60 && doses < 24; action++)
            {
                if (action % 5 == 4) { plan.nextRound(); }
                WintertodtObjective o = plan.prepare(supplies(doses).toBuilder().unfinishedPotionCount(vials)
                    .brumaHerbCount(herbs).freeSlots(free).build(), points, SoloPhase.PREPARE);
                assertNotNull(o); assertTrue(o.getQuantity() > 0);
                // Apply one actual player action, not the entire requested batch.
                if (o.getTarget() == TargetKind.POTION_CRATE) { assertTrue(free > 0); vials++; free--; }
                else if (o.getTarget() == TargetKind.HERBS) { assertTrue(free > 0); herbs++; free--; }
                else { assertEquals("mix_rejuvenation", o.getReason()); assertTrue(vials > 0 && herbs > 0); vials--; herbs--; doses += 4; free++; }
            }
            assertEquals("Capacity " + capacity, 24, doses);
            assertNull(plan.prepare(supplies(doses).toBuilder().unfinishedPotionCount(vials).brumaHerbCount(herbs).freeSlots(free).build(), points, SoloPhase.PREPARE));
        }
    }

    @Test public void brewmaAlsoWaitsForTheRequestedHerbBatch()
    {
        WintertodtSnapshot s = supplies(0).toBuilder().canMixPotion(false).unfinishedPotionCount(6)
            .brumaHerbCount(1).freeSlots(13).build();
        WintertodtObjective gather = plan.prepare(s, points, SoloPhase.PREPARE);
        assertEquals(TargetKind.HERBS, gather.getTarget()); assertEquals(5, gather.getQuantity());
        WintertodtObjective brew = plan.prepare(s.toBuilder().brumaHerbCount(6).freeSlots(8).build(), points, SoloPhase.PREPARE);
        assertEquals(TargetKind.BREWMA, brew.getTarget()); assertEquals(6, brew.getQuantity());
    }

}
