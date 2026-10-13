package com.wintertodtautopilot;

/** Conservative planning costs, including walking rather than assuming run is enabled. */
public final class WintertodtFinishPlan
{
    public static final double FEED_SECONDS = 1.8;
    public static final double FLETCH_SECONDS = 1.8;
    public static final double CHOP_SECONDS = 3.6;
    private WintertodtFinishPlan() { }

    public static double travelSeconds(WintertodtSnapshot s)
    {
        // An unknown/unloaded brazier needs a conservative allowance, not zero travel.
        return s.getBrazierDistance() < 0 ? 10 : s.getBrazierDistance() * .6 + 1.2;
    }
    public static int fletchCapacity(WintertodtSnapshot s)
    {
        if (s.getRoundBudgetSeconds() < 0) { return 0; }
        double spare = s.getRoundBudgetSeconds() - travelSeconds(s)
            - (s.getBrumaRoots() + s.getBrumaKindling()) * FEED_SECONDS;
        return Math.min(s.getBrumaRoots(), Math.max(0, (int) Math.floor(spare / FLETCH_SECONDS)));
    }
    public static int additionalRoots(WintertodtSnapshot s, boolean fletch)
    {
        return additionalRoots(s, fletch, fletch ? s.getBrumaRoots() : 0);
    }
    public static int additionalRoots(WintertodtSnapshot s, boolean fletch, int existingFletches)
    {
        double committed = travelSeconds(s) + (s.getBrumaRoots() + s.getBrumaKindling()) * FEED_SECONDS
            + existingFletches * FLETCH_SECONDS;
        return Math.max(0, (int) Math.floor((s.getRoundBudgetSeconds() - committed)
            / (CHOP_SECONDS + FEED_SECONDS + (fletch ? FLETCH_SECONDS : 0))));
    }
}
