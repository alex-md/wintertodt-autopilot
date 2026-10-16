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
    @Test public void firstRootStillNeedsThreeTicksWithTheFletchingKnife()
    {
        // 5 seconds minus 1.8 walking and 1.8 feeding leaves only 1.4 for the first fletch.
        WintertodtSnapshot s = timed(5).brazierDistance(1).brumaRoots(1).hasFletchingKnife(true).build();
        assertEquals(0, WintertodtFinishPlan.fletchCapacity(s));
        assertEquals(1, WintertodtFinishPlan.fletchCapacity(s.toBuilder().roundBudgetSeconds(6).build()));
    }

    @Test public void capacityNeverExceedsBudgetOrLeavesRoomForAnotherFletch()
    {
        for (boolean knife : new boolean[]{false, true})
        {
            for (int budget = 0; budget <= 120; budget++)
            {
                for (int roots = 1; roots <= 28; roots++)
                {
                    WintertodtSnapshot s = timed(budget).brumaRoots(roots).hasFletchingKnife(knife).build();
                    int count = WintertodtFinishPlan.fletchCapacity(s);
                    double spare = budget - WintertodtFinishPlan.travelSeconds(s) - roots * WintertodtFinishPlan.FEED_SECONDS;
                    double first = knife ? .6 : 0;
                    double each = WintertodtFinishPlan.fletchSeconds(s);
                    if (count > 0) { assertTrue(count * each + first <= spare + 1e-9); }
                    if (count < roots) { assertTrue("Missed a viable fletch: budget=" + budget + ", roots=" + roots,
                        (count + 1) * each + first > spare - 1e-9); }
                }
            }
        }
    }
}
