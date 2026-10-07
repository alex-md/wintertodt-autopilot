package com.wintertodtautopilot;

import java.util.Arrays;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.itemstats.Effect;
import net.runelite.client.plugins.itemstats.ItemStatChanges;
import net.runelite.client.plugins.itemstats.StatChange;
import net.runelite.client.plugins.itemstats.stats.Stats;

@Singleton
public class WintertodtInventoryTracker
{
    @Inject private Client client;
    @Inject private ItemStatChanges itemStats;
    private Item[] previous = new Item[0];

    /** Inventory has only 28 slots; equipment and food effects are sampled once per tick. */
    public void fill(WintertodtSnapshot.WintertodtSnapshotBuilder b)
    {
        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
        Item[] items = inventory == null ? new Item[0] : inventory.getItems();
        int roots = 0, kindling = 0, food = 0, doses = 0, herbs = 0, unfinished = 0, occupied = 0;
        int bestFood = -1, potion = -1, smallestDose = 5;
        boolean knife = false, hammer = false, tinder = false, torch = false, axe = false;
        for (Item item : items)
        {
            int id = item.getId();
            if (id < 0 || item.getQuantity() <= 0) { continue; }
            occupied++;
            int q = item.getQuantity();
            if (id == ItemID.WINT_BRUMA_ROOT) { roots += q; }
            if (id == ItemID.WINT_BRUMA_KINDLING) { kindling += q; }
            if (id == ItemID.WINT_HERB) { herbs += q; }
            if (id == ItemID.WINT_VIAL) { unfinished += q; }
            int d = WintertodtConstants.doses(id);
            doses += d * q;
            if (d > 0 && d < smallestDose) { potion = id; smallestDose = d; }
            if (validFood(id)) { food += q; if (bestFood < 0) { bestFood = id; } }
            knife |= id == ItemID.KNIFE;
            hammer |= hammer(id);
            tinder |= id == ItemID.TINDERBOX;
            torch |= torch(id);
            axe |= axe(id);
        }
        int warmItems = equipment == null ? -1 : 0;
        if (equipment != null)
        {
            for (Item item : equipment.getItems())
            {
                if (item.getId() < 0) { continue; }
                hammer |= hammer(item.getId());
                torch |= torch(item.getId());
                axe |= axe(item.getId());
                if (WintertodtWarmClothing.isWarm(client.getItemDefinition(item.getId()).getName())) { warmItems++; }
            }
        }
        b.brumaRoots(roots).brumaKindling(kindling).foodCount(food).rejuvenationDoses(doses)
            .unfinishedPotionCount(unfinished).brumaHerbCount(herbs).freeSlots(28 - occupied)
            .warmthItem(bestFood >= 0 ? bestFood : potion).potionItem(potion)
            .hasKnife(knife).hasHammer(hammer).hasTinderbox(tinder).hasBrumaTorch(torch).hasAxe(axe)
            .warmClothingCount(warmItems < 0 ? -1 : Math.min(4, warmItems));
    }

    private boolean validFood(int id)
    {
        if (id == ItemID.TRIANGLE_SANDWICH || id == ItemID.RAG_BOTTLE_WINE) { return false; }
        ItemComposition item = client.getItemDefinition(id);
        if (item.getNote() != -1 || item.getInventoryActions() == null
            || !Arrays.asList(item.getInventoryActions()).contains("Eat")) { return false; }
        Effect effect = itemStats.get(id);
        if (effect == null) { return false; }
        for (StatChange change : effect.calculate(client).getStatChanges())
        {
            // Theoretical healing is important: food still warms at full Hitpoints.
            if (change != null && change.getStat() == Stats.HITPOINTS && change.getTheoretical() >= 4)
            {
                return true;
            }
        }
        return false;
    }
    private boolean axe(int id)
    {
        ItemComposition item = client.getItemDefinition(id);
        if (item.getNote() != -1) { return false; }
        String name = item.getName().toLowerCase(Locale.ROOT);
        // Names cover cosmetic variants; exclude pickaxes, battleaxes and thrownaxes.
        return name.contains(" axe") && !name.contains("battleaxe") && !name.contains("pickaxe")
            && !name.contains("thrownaxe") && !name.contains("broken") && !name.contains("head");
    }
    private static boolean hammer(int id)
    {
        return id == ItemID.HAMMER || id == ItemID.IMCANDO_HAMMER || id == ItemID.IMCANDO_HAMMER_OFFHAND;
    }
    private static boolean torch(int id) { return id == ItemID.WINT_TORCH || id == ItemID.WINT_TORCH_OFFHAND; }

    public boolean changed()
    {
        ItemContainer container = client.getItemContainer(InventoryID.INV);
        Item[] current = container == null ? new Item[0] : container.getItems();
        boolean changed = !Arrays.equals(previous, current);
        previous = current.clone();
        return changed;
    }
    public void reset() { previous = new Item[0]; }
}
