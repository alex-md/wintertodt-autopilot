package com.wintertodtautopilot;

import java.util.ArrayDeque;
import java.util.Deque;

/** Observed trend, independent of the mass countdown. Never predicts a recovery deadline. */
final class WintertodtSoloEnergyModel
{
    // Wiki advises avoiding relights around 8%; keep a two-point margin without waiting to 24%.
    static final int RECOVERY_TRIGGER = 10;
    static final int RECOVERY_EXIT = 12;
    enum Trend { UNKNOWN, DRAINING, RECOVERING, STABLE }
    private final Deque<int[]> samples = new ArrayDeque<>();
    private int lastTick = -1;
    private int recoverySince = -1;
    private int consecutiveRecoveryExit;
    private boolean recovering;
    private Trend trend = Trend.UNKNOWN;
    private boolean rapidDrop;

    void reset()
    {
        samples.clear(); lastTick = -1; recoverySince = -1; consecutiveRecoveryExit = 0;
        recovering = false; trend = Trend.UNKNOWN; rapidDrop = false;
    }
    /** Invalidate cadence without granting permission to leave an unfinished recovery. */
    void suspend()
    {
        invalidateTrend(); lastTick = -1; recoverySince = -1;
    }
    void observe(int tick, int energy)
    {
        if (tick < 0 || energy < 0 || energy > 100) { invalidateTrend(); return; }
        if (tick <= lastTick)
        {
            if (tick == lastTick) { return; }
            suspend();
        }
        if (lastTick >= 0 && tick - lastTick > 2) { invalidateTrend(); consecutiveRecoveryExit = 0; }
        lastTick = tick;
        samples.addLast(new int[]{tick, energy});
        while (!samples.isEmpty() && tick - samples.peekFirst()[0] > 17) { samples.removeFirst(); }
        trend = Trend.UNKNOWN; rapidDrop = false;
        if (!samples.isEmpty() && tick - samples.peekFirst()[0] >= 10)
        {
            int delta = energy - samples.peekFirst()[1];
            trend = delta < 0 ? Trend.DRAINING : delta > 0 ? Trend.RECOVERING : Trend.STABLE;
        }
        int recentPeak = energy;
        int previousEnergy = -1;
        for (int[] sample : samples)
        {
            if (tick - sample[0] > 6) { continue; }
            // A recovery increase ends the preceding drain episode.
            recentPeak = previousEnergy >= 0 && sample[1] > previousEnergy
                ? sample[1] : Math.max(recentPeak, sample[1]);
            previousEnergy = sample[1];
        }
        rapidDrop = recentPeak - energy > 2;
        if (energy <= RECOVERY_TRIGGER && !recovering)
        { recovering = true; recoverySince = tick; consecutiveRecoveryExit = 0; }
        if (recovering)
        {
            if (recoverySince < 0) { recoverySince = tick; }
            consecutiveRecoveryExit = energy >= RECOVERY_EXIT ? consecutiveRecoveryExit + 1 : 0;
            if (consecutiveRecoveryExit >= 3 && tick - recoverySince >= 3)
            { recovering = false; recoverySince = -1; consecutiveRecoveryExit = 0; }
        }
    }
    private void invalidateTrend() { samples.clear(); trend = Trend.UNKNOWN; rapidDrop = false; consecutiveRecoveryExit = 0; }
    boolean isRecovering() { return recovering; }
    boolean isRapidDrop() { return rapidDrop; }
    Trend getTrend() { return trend; }
}
