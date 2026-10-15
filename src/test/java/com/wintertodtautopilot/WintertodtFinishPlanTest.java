package com.wintertodtautopilot;

import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtFinishPlanTest
{
    private static WintertodtSnapshot.WintertodtSnapshotBuilder timed(int budget)
    {
        return WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).brazierDistance(0).roundBudgetSeconds(budget);
    }

    @Test public void fletchingKnifeFitsMoreFletchesBeforeTheRoundEnds()
    {
        // 56s budget - 1.2s travel - 20 feeds * 1.8s leaves 18.8s to fletch.
        WintertodtSnapshot.WintertodtSnapshotBuilder s = timed(56).brumaRoots(20);
        assertEquals(10, WintertodtFinishPlan.fletchCapacity(s.build()));
        // The first root still takes 1.8s; each later root takes 1.2s.
        assertEquals(15, WintertodtFinishPlan.fletchCapacity(s.hasFletchingKnife(true).build()));
    }

    @Test public void fletchingKnifeFitsMoreRootsOnlyWhenFletching()
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder s = timed(100);
        assertEquals(13, WintertodtFinishPlan.additionalRoots(s.build(), true));
        assertEquals(14, WintertodtFinishPlan.additionalRoots(s.hasFletchingKnife(true).build(), true));
        assertEquals(WintertodtFinishPlan.additionalRoots(s.hasFletchingKnife(false).build(), false),
            WintertodtFinishPlan.additionalRoots(s.hasFletchingKnife(true).build(), false));
    }
}
