package com.wintertodtautopilot;

import net.runelite.api.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.itemstats.*;
import net.runelite.client.plugins.itemstats.stats.Stats;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WintertodtInventoryTrackerTest
{
    @Test public void countsStacksEmptySlotsAndEquippedToolsAndFoodAtFullHp() throws Exception
    {
        Client client = mock(Client.class); ItemStatChanges stats = mock(ItemStatChanges.class);
        WintertodtInventoryTracker tracker = new WintertodtInventoryTracker();
        TrackerTestSupport.inject(tracker, "client", client); TrackerTestSupport.inject(tracker, "itemStats", stats);
        ItemContainer bag = mock(ItemContainer.class), worn = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.INV)).thenReturn(bag);
        when(client.getItemContainer(InventoryID.WORN)).thenReturn(worn);
        when(bag.getItems()).thenReturn(new Item[]{new Item(ItemID.WINT_BRUMA_ROOT, 3), new Item(ItemID.WINT_BRUMA_KINDLING, 2),
            new Item(ItemID.WINT_POTION1, 1), new Item(ItemID.CAKE, 1), new Item(ItemID.KNIFE, 1), new Item(-1, 0)});
        when(worn.getItems()).thenReturn(new Item[]{new Item(ItemID.IMCANDO_HAMMER_OFFHAND, 1), new Item(ItemID.WINT_TORCH_OFFHAND, 1)});
        ItemComposition other = mock(ItemComposition.class);
        when(other.getNote()).thenReturn(-1); when(other.getName()).thenReturn("Other");
        when(client.getItemDefinition(anyInt())).thenReturn(other);
        ItemComposition cake = mock(ItemComposition.class);
        when(cake.getNote()).thenReturn(-1); when(cake.getName()).thenReturn("Cake"); when(cake.getInventoryActions()).thenReturn(new String[]{"Eat"});
        when(client.getItemDefinition(ItemID.CAKE)).thenReturn(cake);
        Effect effect = mock(Effect.class); when(stats.get(ItemID.CAKE)).thenReturn(effect);
        StatsChanges changes = new StatsChanges(1); StatChange change = new StatChange();
        change.setStat(Stats.HITPOINTS); change.setTheoretical(4); change.setRelative(0); changes.setStatChanges(new StatChange[]{change});
        when(effect.calculate(client)).thenReturn(changes);
        WintertodtSnapshot.WintertodtSnapshotBuilder b = WintertodtSnapshot.builder(); tracker.fill(b);
        WintertodtSnapshot s = b.build();
        assertEquals(3, s.getBrumaRoots()); assertEquals(2, s.getBrumaKindling()); assertEquals(23, s.getFreeSlots());
        assertTrue(s.isHasKnife()); assertTrue(s.isHasHammer()); assertTrue(s.isHasBrumaTorch());
        assertEquals(1, s.getFoodCount()); assertEquals(ItemID.CAKE, s.getWarmthItem()); assertEquals(1, s.getRejuvenationDoses());
        change.setTheoretical(3); tracker.fill(b); assertEquals(ItemID.WINT_POTION1, b.build().getWarmthItem());
        when(cake.getNote()).thenReturn(1); change.setTheoretical(20); tracker.fill(b); assertEquals(0, b.build().getFoodCount());
    }

    @Test public void warmthCountsOnlyEquippedItemsAndCapsAtFour() throws Exception
    {
        Client client = mock(Client.class); WintertodtInventoryTracker tracker = new WintertodtInventoryTracker();
        TrackerTestSupport.inject(tracker, "client", client);
        TrackerTestSupport.inject(tracker, "itemStats", mock(ItemStatChanges.class));
        ItemComposition warm = mock(ItemComposition.class);
        when(warm.getName()).thenReturn("Fire cape"); when(warm.getNote()).thenReturn(-1);
        when(client.getItemDefinition(anyInt())).thenReturn(warm);
        ItemContainer worn = mock(ItemContainer.class), bag = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.WORN)).thenReturn(worn);
        when(client.getItemContainer(InventoryID.INV)).thenReturn(bag);
        when(bag.getItems()).thenReturn(new Item[]{new Item(100000, 1)});
        when(worn.getItems()).thenReturn(new Item[]{new Item(100001, 1), new Item(100002, 1), new Item(100003, 1), new Item(100004, 1), new Item(100005, 1)});
        WintertodtSnapshot.WintertodtSnapshotBuilder b = WintertodtSnapshot.builder();
        tracker.fill(b); assertEquals(4, b.build().getWarmClothingCount());
        when(worn.getItems()).thenReturn(new Item[]{new Item(100001, 1), new Item(-1, 0)});
        tracker.fill(b); assertEquals(1, b.build().getWarmClothingCount());
        when(client.getItemContainer(InventoryID.WORN)).thenReturn(null);
        tracker.fill(b); assertEquals(-1, b.build().getWarmClothingCount());
    }
}
