package com.wintertodtautopilot;

/** Conservative planning costs, including walking rather than assuming run is enabled. */
public final class WintertodtFinishPlan
{
    public static final double FEED_SECONDS = 1.8;
    public static final double FLETCH_SECONDS = 1.8;
    /** A fletching knife, carried or wielded, saves one tick on every root after the first in a batch. */
    public static final double FLETCHING_KNIFE_SECONDS = 1.2;
    public static final double CHOP_SECONDS = 3.6;
    private WintertodtFinishPlan() { }

    public static double travelSeconds(WintertodtSnapshot s)
    {
        // An unknown/unloaded brazier needs a conservative allowance, not zero travel.
        return s.getBrazierDistance() < 0 ? 10 : s.getBrazierDistance() * .6 + 1.2;
    }
    public static double fletchSeconds(WintertodtSnapshot s)
    {
        return s.isHasFletchingKnife() ? FLETCHING_KNIFE_SECONDS : FLETCH_SECONDS;
    }
    /** The first root of a batch always takes the full time. */
    private static double batchStartSeconds(WintertodtSnapshot s) { return FLETCH_SECONDS - fletchSeconds(s); }
    public static int fletchCapacity(WintertodtSnapshot s)
    {
        if (s.getRoundBudgetSeconds() < 0) { return 0; }
        double spare = s.getRoundBudgetSeconds() - travelSeconds(s)
            - (s.getBrumaRoots() + s.getBrumaKindling()) * FEED_SECONDS;
        return Math.min(s.getBrumaRoots(), Math.max(0, (int) Math.floor((spare - batchStartSeconds(s)) / fletchSeconds(s))));
    }
    public static int additionalRoots(WintertodtSnapshot s, boolean fletch)
    {
        return additionalRoots(s, fletch, fletch ? s.getBrumaRoots() : 0);
    }
    public static int additionalRoots(WintertodtSnapshot s, boolean fletch, int existingFletches)
    {
        double committed = travelSeconds(s) + (s.getBrumaRoots() + s.getBrumaKindling()) * FEED_SECONDS
            + existingFletches * fletchSeconds(s) + (fletch || existingFletches > 0 ? batchStartSeconds(s) : 0);
        return Math.max(0, (int) Math.floor((s.getRoundBudgetSeconds() - committed)
            / (CHOP_SECONDS + FEED_SECONDS + (fletch ? fletchSeconds(s) : 0))));
    }
}
