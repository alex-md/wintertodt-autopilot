package com.wintertodtautopilot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Point;

/** Bounded scene search. Walking uses cardinal steps; only explicit shortcut edges cross gaps. */
public final class WintertodtPathfinder
{
    private static final int UNLOADED = 0x1000000;
    private static final int BLOCKED = CollisionDataFlag.BLOCK_MOVEMENT_FULL | UNLOADED;
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
        int[] parents = new int[width * height]; Arrays.fill(parents, -1);
        int origin = start.getX() * height + start.getY(); parents[origin] = origin;
        int[] costs = new int[parents.length]; Arrays.fill(costs, Integer.MAX_VALUE); costs[origin] = 0;
        PriorityQueue<int[]> queue = new PriorityQueue<>(Comparator.comparingInt(entry -> entry[1]));
        queue.add(new int[]{origin, 0});
        int[] dx = {1, 0, -1, 0}, dy = {0, 1, 0, -1};
        while (!queue.isEmpty())
        {
            int[] entry = queue.remove();
            int current = entry[0], x = current / height, y = current % height;
            if (entry[1] != costs[current]) { continue; }
            if (goal(x, y, minX, minY, maxX, maxY, adjacent))
            {
                List<Point> path = new ArrayList<>();
                for (int node = current; node != origin; node = parents[node])
                {
                    path.add(new Point(node / height, node % height));
                }
                path.add(start); Collections.reverse(path);
                return Collections.unmodifiableList(path);
            }
            for (int direction = 0; direction < 4; direction++)
            {
                int nx = x + dx[direction], ny = y + dy[direction];
                if ((flags[x][y] & BLOCKED) != 0 || !inside(nx, ny, width, height) || costs[nx * height + ny] <= costs[current] + 1
                    || !canStep(flags, x, y, nx, ny)) { continue; }
                parents[nx * height + ny] = current;
                costs[nx * height + ny] = costs[current] + 1;
                queue.add(new int[]{nx * height + ny, costs[nx * height + ny]});
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
                    if (!inside(nx, ny, width, height) || (flags[nx][ny] & UNLOADED) != 0
                        || (flags[x][y] & UNLOADED) != 0
                        || (j == 0 || j == shortcut.size() - 1) && (flags[nx][ny] & BLOCKED) != 0
                        || (i == 0 || i == shortcut.size() - 1) && (flags[x][y] & BLOCKED) != 0
                        || costs[nx * height + ny] <= costs[current] + 4) { continue; }
                    int next = nx * height + ny;
                    parents[next] = current; costs[next] = costs[current] + 4;
                    queue.add(new int[]{next, costs[next]});
                }
            }
        }
        // Never fall back to a line through an unreachable obstacle.
        return Collections.emptyList();
    }
    private static boolean goal(int x, int y, int minX, int minY, int maxX, int maxY, boolean adjacent)
    {
        if (!adjacent) { return x >= minX && x <= maxX && y >= minY && y <= maxY; }
        return (x == minX - 1 || x == maxX + 1) && y >= minY && y <= maxY
            || (y == minY - 1 || y == maxY + 1) && x >= minX && x <= maxX;
    }
    static boolean canStep(int[][] flags, int x, int y, int nx, int ny)
    {
        if ((flags[nx][ny] & BLOCKED) != 0) { return false; }
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
