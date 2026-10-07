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
            assertEquals(1, Math.abs(a.getX()-b.getX()) + Math.abs(a.getY()-b.getY()));
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
}
