package com.wintertodtautopilot;

import java.util.List;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Point;
import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtPathfinderTest
{
    @Test public void routesAroundWallThroughOnlyOpening()
    {
        int[][] flags = new int[7][7];
        for (int y = 0; y < 6; y++) { flags[3][y] = CollisionDataFlag.BLOCK_MOVEMENT_FULL; }
        List<Point> path = WintertodtPathfinder.find(flags, new Point(1, 1), 5, 1, 5, 1, false);
        assertFalse(path.isEmpty()); assertTrue(path.contains(new Point(3, 6)));
        for (int i = 1; i < path.size(); i++)
        {
            Point a = path.get(i-1), b = path.get(i);
            assertEquals(1, Math.max(Math.abs(a.getX()-b.getX()), Math.abs(a.getY()-b.getY())));
            assertTrue(WintertodtPathfinder.canStep(flags, a.getX(), a.getY(), b.getX(), b.getY()));
        }
    }
    @Test public void checksWallsOnBothSidesOfTileEdge()
    {
        int[][] flags = new int[2][1];
        flags[0][0] = CollisionDataFlag.BLOCK_MOVEMENT_EAST;
        assertTrue(WintertodtPathfinder.find(flags, new Point(0, 0), 1, 0, 1, 0, false).isEmpty());
        flags[0][0] = 0; flags[1][0] = CollisionDataFlag.BLOCK_MOVEMENT_WEST;
        assertTrue(WintertodtPathfinder.find(flags, new Point(0, 0), 1, 0, 1, 0, false).isEmpty());
    }
    @Test public void stopsBesideBlockedObjectFootprint()
    {
        int[][] flags = new int[8][8];
        for (int x = 4; x <= 6; x++) { for (int y = 4; y <= 6; y++) { flags[x][y] = CollisionDataFlag.BLOCK_MOVEMENT_FULL; } }
        List<Point> path = WintertodtPathfinder.find(flags, new Point(0, 0), 4, 4, 6, 6, true);
        assertFalse(path.isEmpty());
        Point end = path.get(path.size()-1);
        assertTrue(end.getX() == 3 && end.getY() >= 4 && end.getY() <= 6
            || end.getY() == 3 && end.getX() >= 4 && end.getX() <= 6);
        assertEquals(0, flags[end.getX()][end.getY()]);
    }
    @Test public void unreachableOrUnloadedTilesHaveNoStraightLineFallback()
    {
        int[][] flags = new int[3][3];
        flags[1][0] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
        flags[0][1] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
        assertTrue(WintertodtPathfinder.find(flags, new Point(0, 0), 2, 2, 2, 2, false).isEmpty());
        assertTrue(WintertodtPathfinder.find(flags, new Point(0, 0), 5, 5, 5, 5, false).isEmpty());
        int[][] unloaded = new int[2][1]; unloaded[1][0] = 0x1000000;
        assertTrue(WintertodtPathfinder.find(unloaded, new Point(0, 0), 1, 0, 1, 0, false).isEmpty());
    }
    @Test public void prefersShortWalkOverSlowJumps()
    {
        java.util.List<Point> jumps = java.util.Arrays.asList(new Point(1, 1), new Point(3, 1));
        List<Point> path = WintertodtPathfinder.find(new int[5][3], new Point(0, 1), 4, 1, 4, 1, false, jumps);
        assertEquals(5, path.size());
        assertTrue(path.contains(new Point(2, 1)));
    }
    @Test public void crossesOpenGroundDiagonallyWithTheFewestTurns()
    {
        int[][] flags = new int[12][12];
        List<Point> diagonal = WintertodtPathfinder.find(flags, new Point(1, 1), 8, 8, 8, 8, false);
        assertEquals(8, diagonal.size());
        for (int i = 0; i < diagonal.size(); i++) { assertEquals(new Point(i + 1, i + 1), diagonal.get(i)); }
        List<Point> route = WintertodtPathfinder.find(flags, new Point(1, 1), 10, 6, 10, 6, false);
        assertEquals(10, route.size());
        int turns = 0, dx = 0, dy = 0;
        for (int i = 1; i < route.size(); i++)
        {
            int nextDx = route.get(i).getX() - route.get(i - 1).getX();
            int nextDy = route.get(i).getY() - route.get(i - 1).getY();
            if (i > 1 && (nextDx != dx || nextDy != dy)) { turns++; }
            dx = nextDx; dy = nextDy;
        }
        assertEquals(1, turns);
        assertEquals(route, WintertodtPathfinder.find(flags, new Point(1, 1), 10, 6, 10, 6, false));
    }

    @Test public void diagonalsRespectEveryCornerWallAndBlockedSideInAllDirections()
    {
        int[] dx = {1, -1, -1, 1}, dy = {1, 1, -1, -1};
        int[] corners = {CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST, CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST,
            CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST, CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST};
        for (int i = 0; i < 4; i++)
        {
            int nx = 1 + dx[i], ny = 1 + dy[i];
            int[][] flags = new int[3][3];
            assertTrue(WintertodtPathfinder.canStep(flags, 1, 1, nx, ny));
            flags[1][1] = corners[i];
            assertFalse(WintertodtPathfinder.canStep(flags, 1, 1, nx, ny));
            flags[1][1] = 0; flags[nx][ny] = corners[(i + 2) % 4];
            assertFalse(WintertodtPathfinder.canStep(flags, 1, 1, nx, ny));
            flags[nx][ny] = 0; flags[nx][1] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
            assertFalse(WintertodtPathfinder.canStep(flags, 1, 1, nx, ny));
            flags[nx][1] = 0; flags[1][ny] = 0x1000000;
            assertFalse(WintertodtPathfinder.canStep(flags, 1, 1, nx, ny));
            flags[1][ny] = 0;
            flags[nx][1] = dy[i] > 0 ? CollisionDataFlag.BLOCK_MOVEMENT_NORTH : CollisionDataFlag.BLOCK_MOVEMENT_SOUTH;
            assertFalse(WintertodtPathfinder.canStep(flags, 1, 1, nx, ny));
            flags[nx][1] = 0;
            flags[1][ny] = dx[i] > 0 ? CollisionDataFlag.BLOCK_MOVEMENT_EAST : CollisionDataFlag.BLOCK_MOVEMENT_WEST;
            assertFalse(WintertodtPathfinder.canStep(flags, 1, 1, nx, ny));
        }
    }

}
