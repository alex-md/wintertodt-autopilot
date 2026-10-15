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
        return WintertodtSnapshot.builder().hasAxe(true).hasHammer(true).hasTinderbox(true).hasKnife(true)
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
}
