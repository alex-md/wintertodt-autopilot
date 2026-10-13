package com.wintertodtautopilot;

import java.util.ArrayDeque;
import java.util.Deque;

/** Recent energy drain, with blended windows to avoid chasing individual HUD drops. */
public final class WintertodtRoundForecast
{
    private final Deque<int[]> samples = new ArrayDeque<>();
    private double smoothedRate;
    private int lastDropTick = -1;
    private int secondsLeft = -1;

    public int getSecondsLeft() { return secondsLeft; }
    public int getBudgetSeconds() { return secondsLeft < 0 ? -1 : Math.max(0, (int) (secondsLeft * .9) - 3); }
    public void reset()
    {
        samples.clear(); smoothedRate = 0; lastDropTick = -1; secondsLeft = -1;
    }
    public void observe(int energy, int tick, int lit)
    {
        if (energy <= 0 || energy > 100 || lit <= 0) { reset(); return; }
        if (!samples.isEmpty() && (energy > samples.getLast()[1] || tick <= samples.getLast()[0]))
        { reset(); }
        boolean dropped = !samples.isEmpty() && energy < samples.getLast()[1];
        if (samples.isEmpty() || dropped) { lastDropTick = tick; }
        samples.addLast(new int[]{tick, energy});
        while (!samples.isEmpty() && tick - samples.getFirst()[0] > 150) { samples.removeFirst(); }
        secondsLeft = -1;
        // A stopped drain must not leave a convincing but stale countdown.
        if (tick - lastDropTick > 20) { return; }
        double longRate = rate(tick, 150), recentRate = rate(tick, 60);
        if (longRate <= 0) { return; }
        double rate = recentRate > 0 ? longRate * .65 + recentRate * .35 : longRate;
        if (smoothedRate == 0) { smoothedRate = rate; }
        else if (dropped) { smoothedRate = smoothedRate * .6 + rate * .4; }
        secondsLeft = (int) Math.ceil(energy * .6 / smoothedRate);
    }
    private double rate(int tick, int window)
    {
        int count = 0, changes = 0, firstTick = -1, firstEnergy = -1, lastEnergy = -1;
        double sumT = 0, sumE = 0, sumTT = 0, sumTE = 0;
        for (int[] sample : samples)
        {
            if (tick - sample[0] > window) { continue; }
            if (firstTick < 0) { firstTick = sample[0]; firstEnergy = sample[1]; }
            double t = sample[0] - tick;
            double e = sample[1];
            ++count; sumT += t; sumE += e; sumTT += t * t; sumTE += t * e;
            if (lastEnergy >= 0 && sample[1] < lastEnergy) { ++changes; }
            lastEnergy = sample[1];
        }
        // Thirty ticks and three separate drops prevent a single burst becoming a forecast.
        if (count < 4 || changes < 3 || tick - firstTick < 30 || firstEnergy - lastEnergy < 3) { return 0; }
        double denominator = count * sumTT - sumT * sumT;
        return denominator <= 0 ? 0 : Math.max(0, -(count * sumTE - sumT * sumE) / denominator);
    }
}
