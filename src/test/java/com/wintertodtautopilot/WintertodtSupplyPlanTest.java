package com.wintertodtautopilot;

import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtSupplyPlanTest
{
    private WintertodtSnapshot.WintertodtSnapshotBuilder start()
    {
        return WintertodtSnapshot.builder().roundState(RoundState.COUNTDOWN).warmth(100);
    }
    @Test public void higherFiremakingAndWarmClothingReduceReserve()
    {
        assertEquals(3, WintertodtSupplyPlan.recommendedPotions(start().firemakingLevel(50).warmClothingCount(0).build(), 35));
        assertEquals(2, WintertodtSupplyPlan.recommendedPotions(start().firemakingLevel(50).warmClothingCount(4).build(), 35));
        assertEquals(1, WintertodtSupplyPlan.recommendedPotions(start().firemakingLevel(99).warmClothingCount(4).build(), 35));
    }
    @Test public void remainingEnergyAndFoodReduceNeedButKeepSpare()
    {
        WintertodtSnapshot s = start().roundState(RoundState.ACTIVE).wintertodtEnergy(20).firemakingLevel(50).warmClothingCount(0).build();
        assertEquals(1, WintertodtSupplyPlan.recommendedPotions(s, 35));
        assertEquals(1, WintertodtSupplyPlan.recommendedPotions(start().foodCount(20).build(), 35));
    }
    @Test public void reserveAlwaysStaysWithinThreeBottles()
    {
        for (int fm = 50; fm <= 99; fm++)
        {
            for (int clothes = 0; clothes <= 4; clothes++)
            {
                int reserve = WintertodtSupplyPlan.recommendedPotions(start().firemakingLevel(fm).warmClothingCount(clothes).build(), 35);
                assertTrue(reserve >= 1 && reserve <= 3);
            }
        }
    }
    @Test public void registryIncludesWarmItemsAndExcludesKnownNonWarmItems()
    {
        assertTrue(WintertodtWarmClothing.isWarm("Pyromancer garb"));
        assertTrue(WintertodtWarmClothing.isWarm("Clue hunter cloak"));
        assertTrue(WintertodtWarmClothing.isWarm("Fire cape"));
        assertFalse(WintertodtWarmClothing.isWarm("Ice gloves"));
        assertFalse(WintertodtWarmClothing.isWarm("Cake"));
    }
}
