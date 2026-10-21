package com.wintertodtautopilot;

/** Bounded diagnostics for the observed portion of a run, never a claim of full-session coverage. */
final class WintertodtSoloRunSummary
{
    private int firstTick = -1, lastTick = -1, observations, minWarmth = 101, earned = -1, switches, recoveries;
    private SoloPhase lastPhase;
    private ObjectiveType lastAction;
    private WorkingCorner lastDestination;

    String observe(WintertodtSnapshot s, WintertodtObjective o, SoloPhase phase)
    {
        if (!s.active())
        {
            if (s.getRoundState() == RoundState.COMPLETE || s.getSecondsToStart() > 0)
            { return end("verified_round_boundary"); }
            return null;
        }
        if (s.getObservationTick() < 0 || s.getObservationTick() == lastTick) { return null; }
        if (firstTick < 0) { firstTick = s.getObservationTick(); }
        lastTick = s.getObservationTick(); observations++;
        if (s.getWarmth() >= 0) { minWarmth = Math.min(minWarmth, s.getWarmth()); }
        earned = Math.max(earned, s.getPoints());
        if (lastAction != null && (lastAction != o.getType() || lastDestination != o.getDestination())) { switches++; }
        if (phase == SoloPhase.RECOVER_ENERGY && lastPhase != SoloPhase.RECOVER_ENERGY) { recoveries++; }
        lastPhase = phase; lastAction = o.getType(); lastDestination = o.getDestination();
        return null;
    }

    String end(String reason)
    {
        String line = observations == 0 ? null : "solo_run_v1 coverage=observed_only end=" + reason
            + " firstTick=" + firstTick + " lastTick=" + lastTick + " observations=" + observations
            + " minWarmth=" + (minWarmth > 100 ? -1 : minWarmth) + " maxEarned=" + earned
            + " actionSwitches=" + switches + " recoveryEntries=" + recoveries;
        reset(); return line;
    }
    void reset()
    {
        firstTick = -1; lastTick = -1; observations = 0; minWarmth = 101; earned = -1; switches = 0; recoveries = 0;
        lastPhase = null; lastAction = null; lastDestination = null;
    }
}
