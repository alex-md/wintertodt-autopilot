package com.wintertodtautopilot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/** Calculates on game ticks, never during rendering; invalidated by scene events. */
@Singleton
public class WintertodtPathTracker
{
    @Inject private Client client;
    @Inject private WintertodtAutopilotConfig config;
    private boolean lastShortcut;
    // Three manual jumps across the northern pillars, in either direction.
    private static final int[] GAP_X = {1627, 1629, 1631, 1633};
    private static final int GAP_Y = 4023;
    @Getter private List<WorldPoint> path = Collections.emptyList();
    private WorldPoint lastPlayer, lastTarget;
    private TileObject lastObject;
    private NPC lastNpc;
    private int lastTick = -100;
    private boolean dirty = true;
    public void invalidate() { dirty = true; }
    public void reset()
    {
        path = Collections.emptyList(); lastPlayer = null; lastTarget = null;
        lastObject = null; lastNpc = null; lastTick = -100; dirty = true;
    }
    public void update(WorldPoint target, TileObject object, NPC npc, boolean enabled)
    {
        Player player = client.getLocalPlayer(); WorldView view = client.getTopLevelWorldView();
        if (!enabled || target == null || player == null || view == null || target.getPlane() != view.getPlane())
        { reset(); return; }
        WorldPoint location = player.getWorldLocation(); int tick = client.getTickCount();
        boolean shortcut = config.mode() == WintertodtMode.SOLO && view.getPlane() == 0
            && client.getRealSkillLevel(Skill.AGILITY) >= 60 && client.getBoostedSkillLevel(Skill.AGILITY) >= 60;
        if (!dirty && location.equals(lastPlayer) && target.equals(lastTarget)
            && shortcut == lastShortcut && object == lastObject && npc == lastNpc && tick - lastTick < 5) { return; }
        path = Collections.emptyList();
        lastShortcut = shortcut;
        dirty = false; lastPlayer = location; lastTarget = target; lastObject = object; lastNpc = npc; lastTick = tick;
        CollisionData[] maps = view.getCollisionMaps();
        if (maps == null || view.getPlane() >= maps.length || maps[view.getPlane()] == null) { return; }
        LocalPoint local = LocalPoint.fromWorld(view, target);
        LocalPoint start = player.getLocalLocation();
        if (local == null || start == null) { return; }
        int minX = local.getSceneX(), minY = local.getSceneY(), maxX = minX, maxY = minY;
        boolean adjacent = object != null || npc != null;
        if (object instanceof GameObject)
        {
            GameObject gameObject = (GameObject) object;
            Point min = gameObject.getSceneMinLocation(), max = gameObject.getSceneMaxLocation();
            if (min == null || max == null) { return; }
            minX = min.getX(); minY = min.getY(); maxX = max.getX(); maxY = max.getY();
        }
        else if (npc != null && npc.getTransformedComposition() != null)
        {
            int size = Math.max(1, npc.getTransformedComposition().getSize());
            maxX += size - 1; maxY += size - 1;
        }
        List<Point> jumps = new ArrayList<>();
        if (shortcut)
        {
            for (int x : GAP_X)
            {
                LocalPoint pillar = LocalPoint.fromWorld(view, new WorldPoint(x, GAP_Y, 0));
                if (pillar == null) { jumps.clear(); break; }
                jumps.add(new Point(pillar.getSceneX(), pillar.getSceneY()));
            }
        }
        List<Point> tiles = WintertodtPathfinder.find(maps[view.getPlane()].getFlags(),
            new Point(start.getSceneX(), start.getSceneY()), minX, minY, maxX, maxY, adjacent, jumps);
        List<WorldPoint> route = new ArrayList<>();
        for (Point tile : tiles) { route.add(WorldPoint.fromScene(view, tile.getX(), tile.getY(), view.getPlane())); }
        path = Collections.unmodifiableList(route);
    }
}
