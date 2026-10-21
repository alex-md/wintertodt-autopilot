package com.wintertodtautopilot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Point;

/** Bounded scene search. Walking uses collision-checked eight-way steps; only explicit shortcut edges cross gaps. */
public final class WintertodtPathfinder
{
    private static final int UNLOADED = 0x1000000;
    private static final int BLOCKED = CollisionDataFlag.BLOCK_MOVEMENT_FULL | UNLOADED;
    private static final int[] DX = {1, 0, -1, 0, 1, -1, -1, 1};
    private static final int[] DY = {0, 1, 0, -1, 1, 1, -1, -1};
    private static final int HEADINGS = 9;
    private static final int NO_HEADING = 8;
    private WintertodtPathfinder() { }
    public static List<Point> find(int[][] flags, Point start, int minX, int minY, int maxX, int maxY, boolean adjacent)
    {
        return find(flags, start, minX, minY, maxX, maxY, adjacent, Collections.emptyList());
    }
    public static List<Point> find(int[][] flags, Point start, int minX, int minY, int maxX, int maxY,
        boolean adjacent, List<Point> shortcut)
    {
        if (flags == null || flags.length == 0 || flags[0] == null) { return Collections.emptyList(); }
        int width = flags.length, height = flags[0].length;
        if (height == 0 || !inside(start.getX(), start.getY(), width, height)) { return Collections.emptyList(); }
        for (int[] row : flags) { if (row == null || row.length != height) { return Collections.emptyList(); } }
        if (minX < 0 || minY < 0 || maxX >= width || maxY >= height) { return Collections.emptyList(); }
        // Keep arrival headings separate so ties can prefer fewer turns without losing shorter routes.
        int[] parents = new int[width * height * HEADINGS]; Arrays.fill(parents, -1);
        int origin = (start.getX() * height + start.getY()) * HEADINGS + NO_HEADING;
        parents[origin] = origin;
        int[] costs = new int[parents.length]; Arrays.fill(costs, Integer.MAX_VALUE); costs[origin] = 0;
        int[] turns = new int[parents.length]; Arrays.fill(turns, Integer.MAX_VALUE); turns[origin] = 0;
        PriorityQueue<int[]> queue = new PriorityQueue<>(Comparator.<int[]>comparingInt(entry -> entry[1])
            .thenComparingInt(entry -> entry[2]).thenComparingInt(entry -> entry[0]));
        queue.add(new int[]{origin, 0, 0});
        while (!queue.isEmpty())
        {
            int[] entry = queue.remove();
            int current = entry[0], tile = current / HEADINGS, x = tile / height, y = tile % height;
            if (entry[1] != costs[current] || entry[2] != turns[current]) { continue; }
            if (goal(x, y, minX, minY, maxX, maxY, adjacent))
            {
                List<Point> path = new ArrayList<>();
                for (int node = current; node != origin; node = parents[node])
                {
                    path.add(new Point(node / HEADINGS / height, node / HEADINGS % height));
                }
                path.add(start); Collections.reverse(path);
                return Collections.unmodifiableList(path);
            }
            for (int direction = 0; direction < DX.length; direction++)
            {
                int nx = x + DX[direction], ny = y + DY[direction];
                if (!canStep(flags, x, y, nx, ny)) { continue; }
                int next = (nx * height + ny) * HEADINGS + direction;
                int turn = current % HEADINGS == NO_HEADING || current % HEADINGS == direction ? 0 : 1;
                relax(queue, parents, costs, turns, current, next, 1, turn);
            }
            for (int i = 0; i < shortcut.size(); i++)
            {
                Point from = shortcut.get(i);
                if (from.getX() != x || from.getY() != y) { continue; }
                for (int j : new int[]{i - 1, i + 1})
                {
                    if (j < 0 || j >= shortcut.size()) { continue; }
                    Point to = shortcut.get(j);
                    int nx = to.getX(), ny = to.getY();
                    // A jump takes roughly two ticks: prefer walking when it is already shorter.
                    // Pillars may block normal walking. Only the two banks need walkable tiles;
                    // intermediate pillars are reached and left exclusively through these jumps.
                    if (!canJump(flags, shortcut, i, j)) { continue; }
                    int next = (nx * height + ny) * HEADINGS + NO_HEADING;
                    relax(queue, parents, costs, turns, current, next, 4, 0);
                }
            }
        }
        // Never fall back to a line through an unreachable obstacle.
        return Collections.emptyList();
    }
    private static void relax(PriorityQueue<int[]> queue, int[] parents, int[] costs, int[] turns,
        int current, int next, int cost, int turn)
    {
        int nextCost = costs[current] + cost, nextTurns = turns[current] + turn;
        if (nextCost > costs[next] || nextCost == costs[next] && nextTurns >= turns[next]) { return; }
        parents[next] = current; costs[next] = nextCost; turns[next] = nextTurns;
        queue.add(new int[]{next, nextCost, nextTurns});
    }
    /** One distance search for all candidate braziers. Rendering still uses the fewer-turn search above. */
    static java.util.Map<WorkingCorner, CostedPath> findBraziers(int[][] flags, Point start,
        java.util.Map<WorkingCorner, java.awt.Rectangle> targets, List<Point> shortcut)
    {
        java.util.Map<WorkingCorner, CostedPath> found = new java.util.EnumMap<>(WorkingCorner.class);
        if (flags == null || flags.length == 0 || flags[0] == null || flags[0].length == 0) { return found; }
        int width = flags.length, height = flags[0].length;
        for (int[] row : flags) { if (row == null || row.length != height) { return found; } }
        if (!inside(start.getX(), start.getY(), width, height)) { return found; }
        java.util.Map<WorkingCorner, java.awt.Rectangle> valid = new java.util.EnumMap<>(WorkingCorner.class);
        for (java.util.Map.Entry<WorkingCorner, java.awt.Rectangle> e : targets.entrySet())
        {
            java.awt.Rectangle r = e.getValue();
            if (r != null && r.width > 0 && r.height > 0 && inside(r.x, r.y, width, height)
                && inside(r.x + r.width - 1, r.y + r.height - 1, width, height)) { valid.put(e.getKey(), r); }
        }
        targets = valid;
        if (targets.isEmpty()) { return found; }
        int[] costs = new int[width * height], parents = new int[costs.length];
        Arrays.fill(costs, Integer.MAX_VALUE); Arrays.fill(parents, -1);
        int origin = start.getX() * height + start.getY(); costs[origin] = 0; parents[origin] = origin;
        PriorityQueue<int[]> queue = new PriorityQueue<>(Comparator.<int[]>comparingInt(a -> a[1]).thenComparingInt(a -> a[0]));
        queue.add(new int[]{origin, 0});
        while (!queue.isEmpty())
        {
            int[] entry = queue.remove(); int tile = entry[0], cost = entry[1], x = tile / height, y = tile % height;
            if (cost != costs[tile]) { continue; }
            for (java.util.Map.Entry<WorkingCorner, java.awt.Rectangle> target : targets.entrySet())
            {
                java.awt.Rectangle r = target.getValue();
                if (!found.containsKey(target.getKey()) && goal(x, y, r.x, r.y, r.x + r.width - 1, r.y + r.height - 1, true))
                {
                    List<Point> path = new ArrayList<>();
                    for (int node = tile; node != origin; node = parents[node]) { path.add(new Point(node / height, node % height)); }
                    path.add(start); Collections.reverse(path);
                    found.put(target.getKey(), new CostedPath(cost, Collections.unmodifiableList(path)));
                }
            }
            if (found.size() == targets.size()) { break; }
            for (int i = 0; i < DX.length; i++)
            {
                int nx = x + DX[i], ny = y + DY[i];
                if (canStep(flags, x, y, nx, ny)) { relaxTile(queue, parents, costs, tile, nx * height + ny, cost + 1); }
            }
            for (int i = 0; i < shortcut.size(); i++)
            {
                Point from = shortcut.get(i);
                if (from.getX() != x || from.getY() != y) { continue; }
                for (int j : new int[]{i - 1, i + 1})
                {
                    if (!canJump(flags, shortcut, i, j)) { continue; }
                    Point to = shortcut.get(j);
                    relaxTile(queue, parents, costs, tile, to.getX() * height + to.getY(), cost + 4);
                }
            }
        }
        for (WorkingCorner c : targets.keySet()) { found.putIfAbsent(c, new CostedPath(-1, Collections.emptyList())); }
        return found;
    }
    @lombok.Value static class CostedPath { int cost; List<Point> path; }
    /** At most one collision-checked walking step off the intended path to an interaction tile. */
    static boolean passesBrazier(int[][] flags, List<Point> path, java.awt.Rectangle r)
    {
        for (Point p : path)
        {
            int x = p.getX(), y = p.getY();
            if (goal(x, y, r.x, r.y, r.x + r.width - 1, r.y + r.height - 1, true)) { return true; }
            for (int i = 0; i < DX.length; i++)
            {
                int nx = x + DX[i], ny = y + DY[i];
                if (canStep(flags, x, y, nx, ny) && goal(nx, ny, r.x, r.y, r.x + r.width - 1, r.y + r.height - 1, true)) { return true; }
            }
        }
        return false;
    }
    private static void relaxTile(PriorityQueue<int[]> queue, int[] parents, int[] costs, int from, int to, int cost)
    {
        if (cost >= costs[to]) { return; }
        costs[to] = cost; parents[to] = from; queue.add(new int[]{to, cost});
    }
    private static boolean canJump(int[][] flags, List<Point> shortcut, int i, int j)
    {
        if (j < 0 || j >= shortcut.size()) { return false; }
        Point from = shortcut.get(i), to = shortcut.get(j);
        int x = from.getX(), y = from.getY(), nx = to.getX(), ny = to.getY();
        return inside(x, y, flags.length, flags[0].length) && inside(nx, ny, flags.length, flags[0].length)
            && (flags[nx][ny] & UNLOADED) == 0 && (flags[x][y] & UNLOADED) == 0
            && (j != 0 && j != shortcut.size() - 1 || (flags[nx][ny] & BLOCKED) == 0)
            && (i != 0 && i != shortcut.size() - 1 || (flags[x][y] & BLOCKED) == 0);
    }
    private static boolean goal(int x, int y, int minX, int minY, int maxX, int maxY, boolean adjacent)
    {
        if (!adjacent) { return x >= minX && x <= maxX && y >= minY && y <= maxY; }
        return (x == minX - 1 || x == maxX + 1) && y >= minY && y <= maxY
            || (y == minY - 1 || y == maxY + 1) && x >= minX && x <= maxX;
    }
    static boolean canStep(int[][] flags, int x, int y, int nx, int ny)
    {
        if (!inside(x, y, flags.length, flags[0].length) || !inside(nx, ny, flags.length, flags[0].length)
            || (flags[x][y] & BLOCKED) != 0 || (flags[nx][ny] & BLOCKED) != 0) { return false; }
        int dx = nx - x, dy = ny - y;
        if (Math.abs(dx) > 1 || Math.abs(dy) > 1 || dx == 0 && dy == 0) { return false; }
        if (dx != 0 && dy != 0)
        {
            int sourceCorner, destinationCorner;
            if (dx > 0 && dy > 0)
            { sourceCorner = CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST; destinationCorner = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST; }
            else if (dx > 0)
            { sourceCorner = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST; destinationCorner = CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST; }
            else if (dy > 0)
            { sourceCorner = CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST; destinationCorner = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST; }
            else
            { sourceCorner = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST; destinationCorner = CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST; }
            // Both side tiles and all four bordering edges must be clear: never cut an obstacle corner.
            return (flags[x][y] & sourceCorner) == 0 && (flags[nx][ny] & destinationCorner) == 0
                && canStep(flags, x, y, nx, y) && canStep(flags, x, y, x, ny)
                && canStep(flags, nx, y, nx, ny) && canStep(flags, x, ny, nx, ny);
        }
        int sourceWall, destinationWall;
        if (nx > x) { sourceWall = CollisionDataFlag.BLOCK_MOVEMENT_EAST; destinationWall = CollisionDataFlag.BLOCK_MOVEMENT_WEST; }
        else if (nx < x) { sourceWall = CollisionDataFlag.BLOCK_MOVEMENT_WEST; destinationWall = CollisionDataFlag.BLOCK_MOVEMENT_EAST; }
        else if (ny > y) { sourceWall = CollisionDataFlag.BLOCK_MOVEMENT_NORTH; destinationWall = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH; }
        else { sourceWall = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH; destinationWall = CollisionDataFlag.BLOCK_MOVEMENT_NORTH; }
        return (flags[x][y] & sourceWall) == 0 && (flags[nx][ny] & destinationWall) == 0;
    }
    private static boolean inside(int x, int y, int width, int height)
    {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
}
