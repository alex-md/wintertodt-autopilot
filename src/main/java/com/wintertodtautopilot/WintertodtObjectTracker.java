package com.wintertodtautopilot;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

/** Relevant scene references only; transformations are resolved once per game tick. */
@Singleton
public class WintertodtObjectTracker
{
    @Inject private Client client;
    private final Set<TileObject> objects = Collections.newSetFromMap(new IdentityHashMap<>());
    private WorkingCorner corner = WorkingCorner.AUTOMATIC;
    private int intentTicks;

    public void add(TileObject object)
    {
        if (object != null && kind(object.getId()) != TargetKind.NONE) { objects.add(object); }
    }
    public void remove(TileObject object) { objects.remove(object); }
    public void reset() { objects.clear(); resetCorner(); }
    public void resetCorner() { corner = WorkingCorner.AUTOMATIC; intentTicks = 0; }

    public void bootstrap()
    {
        if (client.getTopLevelWorldView() == null) { return; }
        for (Tile[][] plane : client.getTopLevelWorldView().getScene().getTiles())
        {
            for (Tile[] row : plane)
            {
                for (Tile tile : row)
                {
                    if (tile == null) { continue; }
                    for (GameObject object : tile.getGameObjects()) { add(object); }
                    add(tile.getWallObject()); add(tile.getDecorativeObject()); add(tile.getGroundObject());
                }
            }
        }
    }
    public WorkingCorner select(WorldPoint player, WorkingCorner configured, WorldPoint clicked)
    {
        if (configured != WorkingCorner.AUTOMATIC) { corner = configured; return corner; }
        if (clicked != null && clicked.getRegionID() == WintertodtConstants.REGION)
        {
            corner = WorkingCorner.nearest(clicked);
            intentTicks = 20;
        }
        if (intentTicks > 0)
        {
            --intentTicks;
            if (player.distanceTo(corner.location()) <= 4) { intentTicks = 0; }
            return corner;
        }
        WorkingCorner closest = WorkingCorner.nearest(player);
        if (corner == WorkingCorner.AUTOMATIC
            || (closest != corner && player.distanceTo(closest.location()) + 8 < player.distanceTo(corner.location())))
        {
            corner = closest;
        }
        return corner;
    }
    public TileObject target(TargetKind kind, WorldPoint player)
    {
        WorldPoint anchor = kind == TargetKind.BRAZIER || kind == TargetKind.ROOTS ? corner.location() : player;
        TileObject result = null;
        int distance = Integer.MAX_VALUE;
        for (TileObject object : objects)
        {
            if (kind(resolvedId(object)) != kind) { continue; }
            int d = object.getWorldLocation().distanceTo(anchor);
            if ((kind == TargetKind.BRAZIER && d > 3) || (kind == TargetKind.ROOTS && d > 15)) { continue; }
            if (d < distance) { result = object; distance = d; }
        }
        return result;
    }
    public int resolvedId(TileObject object)
    {
        ObjectComposition composition = client.getObjectDefinition(object.getId());
        if (composition.getImpostorIds() != null) { composition = composition.getImpostor(); }
        return composition == null ? -1 : composition.getId();
    }
    public BrazierState brazier(WorldPoint player)
    {
        TileObject object = target(TargetKind.BRAZIER, player);
        if (object == null) { return BrazierState.UNKNOWN; }
        switch (resolvedId(object))
        {
            case ObjectID.WINT_BRAZIER: return BrazierState.UNLIT;
            case ObjectID.WINT_BRAZIER_LIT: return BrazierState.LIT;
            case ObjectID.WINT_BRAZIER_BROKEN: return BrazierState.BROKEN;
            default: return BrazierState.UNAVAILABLE;
        }
    }
    public int litCount()
    {
        Set<WorldPoint> positions = new java.util.HashSet<>();
        for (TileObject object : objects)
        {
            if (resolvedId(object) == ObjectID.WINT_BRAZIER_LIT) { positions.add(object.getWorldLocation()); }
        }
        return positions.size();
    }
    public static TargetKind kind(int id)
    {
        switch (id)
        {
            case ObjectID.WINT_BRAZIER:
            case ObjectID.WINT_BRAZIER_LIT:
            case ObjectID.WINT_BRAZIER_BROKEN: return TargetKind.BRAZIER;
            case ObjectID.WINT_ROOTS: return TargetKind.ROOTS;
            case ObjectID.WINT_HERB_ROOTS: return TargetKind.HERBS;
            case ObjectID.WINT_CHEST_VIAL: return TargetKind.POTION_CRATE;
            case ObjectID.WINT_CHEST_HAMMER: return TargetKind.HAMMER_CRATE;
            case ObjectID.WINT_CHEST_KNIFE: return TargetKind.KNIFE_CRATE;
            case ObjectID.WINT_CHEST_AXE: return TargetKind.AXE_CRATE;
            case ObjectID.WINT_CHEST_TINDERBOX: return TargetKind.TINDERBOX_CRATE;
            case ObjectID.WINT_DOOR: return TargetKind.DOOR;
            default: return TargetKind.NONE;
        }
    }
}
