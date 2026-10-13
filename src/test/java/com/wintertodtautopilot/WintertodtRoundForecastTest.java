package com.wintertodtautopilot;

import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtRoundForecastTest
{
    @Test public void learnsSustainedPaceAndKeepsAModerateSafetyMargin()
    {
        WintertodtRoundForecast forecast = new WintertodtRoundForecast();
        for (int tick = 0; tick < 30; tick++) { forecast.observe(30 - tick / 10, tick, 4); }
        assertEquals(-1, forecast.getSecondsLeft());
        forecast.observe(27, 30, 4);
        assertTrue(forecast.getSecondsLeft() >= 150 && forecast.getSecondsLeft() <= 190);
        assertEquals((int) (forecast.getSecondsLeft() * .9) - 3, forecast.getBudgetSeconds());
    }
    @Test public void respondsToSustainedAccelerationWithoutChasingOneDrop()
    {
        WintertodtRoundForecast forecast = new WintertodtRoundForecast();
        for (int tick = 0; tick <= 70; tick++) { forecast.observe(60 - tick / 10, tick, 4); }
        int before = forecast.getSecondsLeft();
        for (int tick = 71; tick <= 130; tick++) { forecast.observe(53 - (tick - 70) / 2, tick, 4); }
        assertTrue(forecast.getSecondsLeft() < before / 2);
        assertTrue(forecast.getSecondsLeft() < 70);
    }
    @Test public void partialBrazierChangesDoNotDiscardLearnedPace()
    {
        WintertodtRoundForecast forecast = new WintertodtRoundForecast();
        for (int tick = 0; tick <= 50; tick++) { forecast.observe(30 - tick / 10, tick, 4); }
        int before = forecast.getSecondsLeft();
        forecast.observe(25, 51, 3);
        assertTrue(forecast.getSecondsLeft() > 0);
        assertTrue(Math.abs(before - forecast.getSecondsLeft()) <= 2);
        forecast.observe(25, 52, 4);
        assertTrue(Math.abs(before - forecast.getSecondsLeft()) <= 2);
    }
    @Test public void unavailableAndStalledSignalsInvalidatePrediction()
    {
        WintertodtRoundForecast forecast = new WintertodtRoundForecast();
        for (int tick = 0; tick <= 50; tick++) { forecast.observe(30 - tick / 10, tick, 4); }
        forecast.observe(25, 71, 4);
        assertEquals(-1, forecast.getSecondsLeft());
        forecast.observe(25, 72, 0);
        assertEquals(-1, forecast.getSecondsLeft());
        forecast.observe(-1, 73, 4);
        assertEquals(-1, forecast.getBudgetSeconds());
    }
    @Test public void newRoundAndSingleHudBurstDoNotProduceFalseCountdown()
    {
        WintertodtRoundForecast forecast = new WintertodtRoundForecast();
        forecast.observe(30, 0, 4); forecast.observe(20, 30, 4);
        assertEquals(-1, forecast.getSecondsLeft());
        forecast.reset();
        for (int tick = 0; tick <= 50; tick++) { forecast.observe(30 - tick / 10, tick, 4); }
        assertTrue(forecast.getSecondsLeft() > 0);
        forecast.observe(100, 51, 4);
        assertEquals(-1, forecast.getSecondsLeft());
        forecast.observe(99, 71, 4);
        assertEquals(-1, forecast.getSecondsLeft());
    }
}
