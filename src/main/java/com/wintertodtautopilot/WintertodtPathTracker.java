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
    private boolean costsDirty = true, costsShortcut;
    private int costsTick = -100;
    private int[][] costsFlags;
    private WorldView costsView;
    private WorldPoint costsPlayer;
    private List<BrazierRoute> costs = Collections.emptyList();
    public void invalidate() { dirty = true; costsDirty = true; }
    public void reset()
    {
        clearPath(); costsDirty = true; costs = Collections.emptyList(); costsFlags = null; costsView = null; costsPlayer = null;
    }
    private void clearPath()
    {
        path = Collections.emptyList(); lastPlayer = null; lastTarget = null;
        lastObject = null; lastNpc = null; lastTick = -100; dirty = true;
    }
    public void update(WorldPoint target, TileObject object, NPC npc, boolean enabled)
    {
        Player player = client.getLocalPlayer(); WorldView view = client.getTopLevelWorldView();
        if (!enabled || target == null || player == null || view == null || target.getPlane() != view.getPlane())
        { clearPath(); return; }
        WorldPoint location = player.getWorldLocation(); int tick = client.getTickCount();
        boolean shortcut = shortcutAllowed(view);
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
        List<Point> jumps = jumps(view, shortcut);
        List<Point> tiles = WintertodtPathfinder.find(maps[view.getPlane()].getFlags(),
            new Point(start.getSceneX(), start.getSceneY()), minX, minY, maxX, maxY, adjacent, jumps);
        List<WorldPoint> route = new ArrayList<>();
        for (Point tile : tiles) { route.add(WorldPoint.fromScene(view, tile.getX(), tile.getY(), view.getPlane())); }
        path = Collections.unmodifiableList(route);
    }

    /** Navigation evidence is independent of whether the optional walking line is displayed. */
    public List<BrazierRoute> observeBrazierRoutes(WintertodtSnapshot s, WintertodtObjectTracker objects)
    {
        WorldView view = client.getTopLevelWorldView(); Player player = client.getLocalPlayer();
        if (view == null || player == null || player.getLocalLocation() == null || s.getPlayerLocation() == null) { return Collections.emptyList(); }
        CollisionData[] maps = view.getCollisionMaps();
        if (maps == null || view.getPlane() < 0 || view.getPlane() >= maps.length || maps[view.getPlane()] == null) { return Collections.emptyList(); }
        int[][] flags = maps[view.getPlane()].getFlags(); boolean shortcut = shortcutAllowed(view);
        int tick = client.getTickCount();
        if (!costsDirty && view == costsView && flags == costsFlags && s.getPlayerLocation().equals(costsPlayer)
            && shortcut == costsShortcut && tick >= costsTick && tick - costsTick < 5) { return costs; }
        java.util.Map<WorkingCorner, java.awt.Rectangle> targets = new java.util.EnumMap<>(WorkingCorner.class);
        for (WorkingCorner c : WorkingCorner.values())
        {
            if (c == WorkingCorner.AUTOMATIC) { continue; }
            LocalPoint local = LocalPoint.fromWorld(view, c.location());
            if (local == null || c.location().getPlane() != view.getPlane()) { continue; }
            java.awt.Rectangle footprint = new java.awt.Rectangle(local.getSceneX(), local.getSceneY(), 1, 1);
            TileObject object = objects.target(TargetKind.BRAZIER, s.getPlayerLocation(), c);
            if (object instanceof GameObject)
            {
                Point min = ((GameObject) object).getSceneMinLocation(), max = ((GameObject) object).getSceneMaxLocation();
                if (min != null && max != null) { footprint = new java.awt.Rectangle(min.getX(), min.getY(), max.getX() - min.getX() + 1, max.getY() - min.getY() + 1); }
            }
            targets.put(c, footprint);
        }
        LocalPoint start = player.getLocalLocation();
        java.util.Map<WorkingCorner, WintertodtPathfinder.CostedPath> paths = WintertodtPathfinder.findBraziers(flags,
            new Point(start.getSceneX(), start.getSceneY()), targets, jumps(view, shortcut));
        List<BrazierRoute> result = new ArrayList<>();
        for (java.util.Map.Entry<WorkingCorner, WintertodtPathfinder.CostedPath> e : paths.entrySet())
        {
            List<WorldPoint> steps = new ArrayList<>();
            for (Point p : e.getValue().getPath()) { steps.add(WorldPoint.fromScene(view, p.getX(), p.getY(), view.getPlane())); }
            List<WorkingCorner> onWay = new ArrayList<>();
            for (java.util.Map.Entry<WorkingCorner, java.awt.Rectangle> target : targets.entrySet())
            {
                if (target.getKey() != e.getKey() && WintertodtPathfinder.passesBrazier(flags, e.getValue().getPath(), target.getValue()))
                { onWay.add(target.getKey()); }
            }
            result.add(new BrazierRoute(e.getKey(), e.getValue().getCost(), steps, onWay));
        }
        costsDirty = false; costsView = view; costsFlags = flags; costsPlayer = s.getPlayerLocation();
        costsShortcut = shortcut; costsTick = tick; costs = Collections.unmodifiableList(result);
        return costs;
    }
    private boolean shortcutAllowed(WorldView view)
    {
        return config.mode() == WintertodtMode.SOLO && view.getPlane() == 0
            && client.getRealSkillLevel(Skill.AGILITY) >= 60 && client.getBoostedSkillLevel(Skill.AGILITY) >= 60;
    }
    private static List<Point> jumps(WorldView view, boolean allowed)
    {
        List<Point> jumps = new ArrayList<>();
        if (!allowed) { return jumps; }
        for (int x : GAP_X)
        {
            LocalPoint pillar = LocalPoint.fromWorld(view, new WorldPoint(x, GAP_Y, 0));
            if (pillar == null) { jumps.clear(); break; }
            jumps.add(new Point(pillar.getSceneX(), pillar.getSceneY()));
        }
        return jumps;
    }
}
