package com.wintertodtautopilot;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WintertodtBrazierRouteTest
{
    @Test public void oneSearchAgreesWithExistingWalkingSearchAroundWallsAndUnreachableTargets()
    {
        int[][] flags = new int[60][60];
        for (int y = 0; y < 45; y++) { flags[30][y] = CollisionDataFlag.BLOCK_MOVEMENT_FULL; }
        Map<WorkingCorner, Rectangle> goals = goals();
        Point start = new Point(40, 18);
        Map<WorkingCorner, WintertodtPathfinder.CostedPath> routes = WintertodtPathfinder.findBraziers(flags, start, goals, Collections.emptyList());
        for (Map.Entry<WorkingCorner, Rectangle> e : goals.entrySet())
        {
            Rectangle r = e.getValue();
            List<Point> reference = WintertodtPathfinder.find(flags, start, r.x, r.y, r.x, r.y, true);
            assertEquals(reference.size() - 1, routes.get(e.getKey()).getCost());
            List<Point> path = routes.get(e.getKey()).getPath();
            for (int i = 1; i < path.size(); i++)
            { assertTrue(WintertodtPathfinder.canStep(flags, path.get(i-1).getX(), path.get(i-1).getY(), path.get(i).getX(), path.get(i).getY())); }
        }
        assertTrue(routes.get(WorkingCorner.NORTHWEST).getCost() > routes.get(WorkingCorner.NORTHEAST).getCost());
        for (int y = 0; y < 60; y++) { flags[30][y] = CollisionDataFlag.BLOCK_MOVEMENT_FULL; }
        routes = WintertodtPathfinder.findBraziers(flags, start, goals, Collections.emptyList());
        assertEquals(-1, routes.get(WorkingCorner.NORTHWEST).getCost());
        assertEquals(-1, routes.get(WorkingCorner.SOUTHWEST).getCost());
    }

    @Test public void weightedNorthernJumpsUseTheSameEligibilityEdgesAsDisplayedPaths()
    {
        int[][] flags = new int[24][32];
        for (int x = 8; x <= 12; x++) { for (int y = 5; y < 32; y++) { flags[x][y] = CollisionDataFlag.BLOCK_MOVEMENT_FULL; } }
        List<Point> jumps = Arrays.asList(new Point(7, 23), new Point(9, 23), new Point(11, 23), new Point(13, 23));
        Map<WorkingCorner, Rectangle> goals = new EnumMap<>(WorkingCorner.class);
        goals.put(WorkingCorner.NORTHEAST, new Rectangle(14, 23, 1, 1));
        WintertodtPathfinder.CostedPath shortcut = WintertodtPathfinder.findBraziers(flags, new Point(7, 23), goals, jumps).get(WorkingCorner.NORTHEAST);
        assertEquals(12, shortcut.getCost()); assertEquals(jumps, shortcut.getPath());
        WintertodtPathfinder.CostedPath walk = WintertodtPathfinder.findBraziers(flags, new Point(7, 23), goals, Collections.emptyList()).get(WorkingCorner.NORTHEAST);
        assertTrue(walk.getCost() > shortcut.getCost());
        flags[9][23] = 0x1000000;
        assertEquals(walk.getCost(), WintertodtPathfinder.findBraziers(flags, new Point(7, 23), goals, jumps).get(WorkingCorner.NORTHEAST).getCost());
    }

    @Test public void nearbyButWallSeparatedBrazierIsNotAnOnTheWayInteraction()
    {
        int[][] flags = new int[8][10]; flags[3][4] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
        List<Point> path = Arrays.asList(new Point(1, 3), new Point(1, 4), new Point(1, 5));
        Rectangle brazier = new Rectangle(3, 4, 1, 1);
        assertTrue(WintertodtPathfinder.passesBrazier(flags, path, brazier));
        for (int y = 0; y < 10; y++) { flags[2][y] = CollisionDataFlag.BLOCK_MOVEMENT_FULL; }
        assertFalse(WintertodtPathfinder.passesBrazier(flags, path, brazier));
    }

    @Test public void missingOrInvalidSceneGeometryRemainsUnknownRatherThanUnreachable()
    {
        Map<WorkingCorner, Rectangle> targets = new EnumMap<>(WorkingCorner.class);
        targets.put(WorkingCorner.NORTHWEST, new Rectangle(20, 20, 1, 1));
        assertTrue(WintertodtPathfinder.findBraziers(new int[8][8], new Point(1, 1), targets, Collections.emptyList()).isEmpty());
        assertTrue(WintertodtPathfinder.findBraziers(null, new Point(1, 1), targets, Collections.emptyList()).isEmpty());
        assertTrue(WintertodtPathfinder.findBraziers(new int[][]{new int[2], null}, new Point(1, 1), targets, Collections.emptyList()).isEmpty());
    }

    @Test public void navigationWorksWithWalkingLineDisabledAndCacheRespondsToCollisionInvalidation() throws Exception
    {
        Client client = mock(Client.class); WorldView view = mock(WorldView.class);
        Player player = mock(Player.class); CollisionData collision = mock(CollisionData.class);
        WintertodtObjectTracker objects = mock(WintertodtObjectTracker.class);
        when(client.getLocalPlayer()).thenReturn(player); when(client.getTopLevelWorldView()).thenReturn(view);
        when(view.getBaseX()).thenReturn(1600); when(view.getBaseY()).thenReturn(3980);
        when(view.getSizeX()).thenReturn(60); when(view.getSizeY()).thenReturn(60);
        when(view.getCollisionMaps()).thenReturn(new CollisionData[]{collision});
        int[][] flags = new int[60][60]; when(collision.getFlags()).thenReturn(flags);
        when(player.getWorldLocation()).thenReturn(new WorldPoint(1640, 3998, 0));
        LocalPoint location = LocalPoint.fromScene(40, 18, view);
        when(player.getLocalLocation()).thenReturn(location);
        WintertodtPathTracker tracker = new WintertodtPathTracker();
        TrackerTestSupport.inject(tracker, "client", client);
        TrackerTestSupport.inject(tracker, "config", new WintertodtAutopilotConfig() {
            @Override public WintertodtMode mode() { return WintertodtMode.SOLO; }
        });
        WintertodtSnapshot s = WintertodtSoloReliabilityTest.observed(1, 100, 100, 0, BrazierState.LIT).toBuilder()
            .playerLocation(player.getWorldLocation()).build();
        List<BrazierRoute> first = tracker.observeBrazierRoutes(s, objects);
        assertEquals(4, first.size());
        tracker.update(null, null, null, false); // Optional rendering must not throw away navigation evidence.
        assertSame(first, tracker.observeBrazierRoutes(s, objects));
        for (int y = 0; y < 60; y++) { flags[30][y] = CollisionDataFlag.BLOCK_MOVEMENT_FULL; }
        tracker.invalidate();
        List<BrazierRoute> second = tracker.observeBrazierRoutes(s, objects);
        assertNotSame(first, second);
        assertEquals(-1, second.stream().filter(r -> r.getDestination() == WorkingCorner.NORTHWEST).findFirst().get().getWalkingCost());
        java.util.List<CornerObservation> observed = new java.util.ArrayList<>();
        for (CornerObservation c : s.getCorners())
        {
            observed.add(c.getCorner() != WorkingCorner.SOUTHEAST ? c : new CornerObservation(c.getCorner(), BrazierState.LIT,
                c.getPyromancerState(), c.getPyromancerHealthPercent(), c.getBrazierSource(), c.getPyromancerSource(),
                c.getBrazierObservedTick(), c.getPyromancerObservedTick(), c.isBrazierVisible(), c.isPyromancerVisible()));
        }
        WintertodtSnapshot routed = s.toBuilder().clearCorners().corners(observed).brazierRoutes(second).build();
        WintertodtObjective next = new WintertodtSoloEngine().decide(routed, WintertodtSoloReliabilityTest.SETTINGS);
        assertEquals(ObjectiveType.LIGHT, next.getType());
        assertEquals(WorkingCorner.NORTHEAST, next.getDestination()); // Actual collision evidence reaches the planner.
        tracker.reset();
        assertNotSame(second, tracker.observeBrazierRoutes(s, objects));
    }

    private Map<WorkingCorner, Rectangle> goals()
    {
        Map<WorkingCorner, Rectangle> result = new EnumMap<>(WorkingCorner.class);
        result.put(WorkingCorner.NORTHWEST, new Rectangle(21, 36, 1, 1));
        result.put(WorkingCorner.NORTHEAST, new Rectangle(39, 36, 1, 1));
        result.put(WorkingCorner.SOUTHWEST, new Rectangle(21, 18, 1, 1));
        result.put(WorkingCorner.SOUTHEAST, new Rectangle(39, 18, 1, 1));
        return result;
    }
}
