package com.wintertodtautopilot;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.wintertodtautopilot.WintertodtEffortPolicyTest.*;

public class WintertodtConsumptionReserveTest
{
    private final WintertodtSoloSupplyPlan plan = new WintertodtSoloSupplyPlan();
    private WintertodtSnapshot sample(int tick, int personal, int healing)
    { return scene(tick, 25, 24).toBuilder().personalDosesUsed(personal).healingDosesUsed(healing).build(); }
    @Test public void baselineNeverShrinksFromLowConsumptionOrUnattributedLosses()
    {
        for (int t = 0; t <= 500; t++) { plan.observe(sample(t, 0, 0).toBuilder().rejuvenationDoses(t % 24).build(), COMFORTABLE); }
        plan.prepare(scene(501, 25, 24), COMFORTABLE, SoloPhase.SUSTAIN);
        assertEquals(24, plan.getTarget()); assertEquals(2, plan.personalMinimum());
        assertTrue(plan.detail().contains("0 personal / 0 healing"));
    }
    @Test public void confirmedSustainedConsumptionIncreasesReserveWithinABoundAndProtectsPersonalDoses()
    {
        for (int t = 0; t <= 310; t++) { plan.observe(sample(t, t % 10 == 0 ? 1 : 0, 0), ACTIVE); }
        plan.prepare(scene(311, 25, 24), ACTIVE, SoloPhase.SUSTAIN);
        assertEquals(40, plan.getTarget()); assertEquals(8, plan.personalMinimum());
        assertTrue(plan.detail().contains("31 personal"));
    }
    @Test public void sparseEvidenceAndDuplicatesCannotCalibrateAReserve()
    {
        for (int t = 0; t <= 600; t += 2)
        { plan.observe(sample(t, 1, 1), ACTIVE); plan.observe(sample(t, 1, 1), ACTIVE); }
        plan.prepare(scene(601, 25, 24), ACTIVE, SoloPhase.SUSTAIN);
        assertEquals(24, plan.getTarget()); assertTrue(plan.detail().contains("0 personal / 0 healing"));
    }
    @Test public void missingContainersLoadingAndEquipmentChangesBreakTheCalibrationWindow()
    {
        for (int t = 0; t <= 200; t++) { plan.observe(sample(t, t % 10 == 0 ? 1 : 0, 0), ACTIVE); }
        plan.suspend();
        for (int t = 201; t <= 400; t++) { plan.observe(sample(t, 1, 0), ACTIVE); }
        plan.observe(sample(401, 1, 0).toBuilder().inventoryKnown(false).build(), ACTIVE);
        for (int t = 402; t <= 500; t++) { plan.observe(sample(t, 1, 0).toBuilder().warmClothingCount(4).build(), ACTIVE); }
        plan.prepare(scene(501, 25, 24), ACTIVE, SoloPhase.SUSTAIN);
        assertEquals(24, plan.getTarget());
    }
    @Test public void optionalHealingAllowanceIsIndependentOfPersonalConsumptionAndEffort()
    {
        for (int t = 0; t <= 310; t++) { plan.observe(sample(t, 0, t % 10 == 0 ? 1 : 0), ACTIVE); }
        plan.prepare(scene(311, 25, 24), ACTIVE, SoloPhase.SUSTAIN);
        assertEquals(40, plan.getTarget()); assertEquals(2, plan.personalMinimum());
        plan.observe(sample(312, 0, 0), COMFORTABLE);
        plan.prepare(scene(312, 25, 24), COMFORTABLE, SoloPhase.SUSTAIN);
        assertEquals(24, plan.getTarget()); // Old effort's calibration does not leak into the new preference.
    }
    @Test public void preparationTargetIsLatchedAndFinishDoesNotDemandAFullWorkingReserve()
    {
        plan.prepare(scene(0, 25, 0), ACTIVE, SoloPhase.PREPARE);
        for (int t = 0; t <= 310; t++) { plan.observe(sample(t, 1, 0), ACTIVE); }
        plan.prepare(scene(311, 25, 4), ACTIVE, SoloPhase.PREPARE);
        assertEquals(24, plan.getTarget());
        plan.prepare(scene(312, 25, 24), ACTIVE, SoloPhase.SUSTAIN);
        plan.prepare(scene(313, 25, 24), ACTIVE, SoloPhase.SUSTAIN);
        assertEquals(40, plan.getTarget());
        assertNull(plan.prepare(scene(314, 25, 16), ACTIVE, SoloPhase.FINISH));
        assertTrue(plan.getTarget() <= 16);
    }
    @Test public void readinessDistinguishesUnknownDataMissingToolsAndReusablePartialReserve()
    {
        WintertodtSoloEngine e = new WintertodtSoloEngine();
        e.decide(scene(10, 25, 12).toBuilder().inventoryKnown(false).build(), COMFORTABLE);
        assertTrue(e.getReadiness().contains("Waiting"));
        e.decide(scene(11, 25, 12).toBuilder().hasAxe(false).build(), COMFORTABLE);
        assertEquals("Missing tool", e.getReadiness());
        e.decide(scene(12, 25, 12).toBuilder().warmClothingCount(2).build(), COMFORTABLE);
        assertTrue(e.getReadiness().contains("Supplies ready: 12 doses")); assertTrue(e.getReadiness().contains("2/4 warm items"));
    }
}
