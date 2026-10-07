package com.wintertodtautopilot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Point;

/** Bounded scene BFS. Cardinal steps avoid diagonal corner cutting entirely. */
public final class WintertodtPathfinder
{
    private static final int UNLOADED = 0x1000000;
    private static final int BLOCKED = CollisionDataFlag.BLOCK_MOVEMENT_FULL | UNLOADED;
    private WintertodtPathfinder() { }
    public static List<Point> find(int[][] flags, Point start, int minX, int minY, int maxX, int maxY, boolean adjacent)
    {
        if (flags == null || flags.length == 0 || flags[0] == null) { return Collections.emptyList(); }
        int width = flags.length, height = flags[0].length;
        if (height == 0 || !inside(start.getX(), start.getY(), width, height)) { return Collections.emptyList(); }
        for (int[] row : flags) { if (row == null || row.length != height) { return Collections.emptyList(); } }
        if (minX < 0 || minY < 0 || maxX >= width || maxY >= height) { return Collections.emptyList(); }
        int[] parents = new int[width * height]; Arrays.fill(parents, -1);
        int origin = start.getX() * height + start.getY(); parents[origin] = origin;
        ArrayDeque<Integer> queue = new ArrayDeque<>(); queue.add(origin);
        int[] dx = {1, 0, -1, 0}, dy = {0, 1, 0, -1};
        while (!queue.isEmpty())
        {
            int current = queue.removeFirst(), x = current / height, y = current % height;
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
                if (!inside(nx, ny, width, height) || parents[nx * height + ny] >= 0
                    || !canStep(flags, x, y, nx, ny)) { continue; }
                parents[nx * height + ny] = current;
                queue.addLast(nx * height + ny);
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
