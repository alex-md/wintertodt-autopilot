package com.wintertodtautopilot;

import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtSoloEnergyModelTest
{
    @Test public void trendAllowsRecoveryAndNeverProducesMassDeadline()
    {
        WintertodtSoloEnergyModel model = new WintertodtSoloEnergyModel();
        for (int tick = 1; tick <= 18; tick++) { model.observe(tick, 30 - tick / 10); }
        assertEquals(WintertodtSoloEnergyModel.Trend.DRAINING, model.getTrend());
        for (int tick = 19; tick <= 40; tick++) { model.observe(tick, 29 + (tick - 19) / 10); }
        assertEquals(WintertodtSoloEnergyModel.Trend.RECOVERING, model.getTrend());
        model.observe(41, -1); assertEquals(WintertodtSoloEnergyModel.Trend.UNKNOWN, model.getTrend());
        model.observe(42, 9); assertTrue(model.isRecovering());
    }
    @Test public void duplicateTicksCannotSatisfyRecoveryExitAndGapsResetConfirmation()
    {
        WintertodtSoloEnergyModel model = new WintertodtSoloEnergyModel();
        model.observe(1, 9); model.observe(2, 12);
        for (int i = 0; i < 10; i++) { model.observe(2, 12); }
        assertTrue(model.isRecovering());
        model.observe(3, 12); assertTrue(model.isRecovering());
        model.observe(8, 12); assertTrue(model.isRecovering());
        model.observe(9, 12); model.observe(10, 12); assertFalse(model.isRecovering());
    }
    @Test public void recoveryIncreaseClearsAnEarlierRapidDrainEpisode()
    {
        WintertodtSoloEnergyModel model = new WintertodtSoloEnergyModel();
        model.observe(1, 28); model.observe(2, 14); assertTrue(model.isRapidDrop());
        model.observe(3, 25); assertFalse(model.isRapidDrop());
        model.observe(4, 25); assertFalse(model.isRapidDrop());
    }

}
