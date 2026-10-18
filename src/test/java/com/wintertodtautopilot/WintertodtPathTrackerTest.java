package com.wintertodtautopilot;

import java.util.List;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WintertodtPathTrackerTest
{
    @Test public void resolvesFootprintCachesAndInvalidatesCollisionChanges() throws Exception
    {
        Client client = mock(Client.class); Player player = mock(Player.class);
        WorldView view = mock(WorldView.class); CollisionData collision = mock(CollisionData.class);
        when(client.getLocalPlayer()).thenReturn(player); when(client.getTopLevelWorldView()).thenReturn(view);
        when(view.getBaseX()).thenReturn(1600); when(view.getBaseY()).thenReturn(3968);
        when(view.getSizeX()).thenReturn(8); when(view.getSizeY()).thenReturn(8);
        when(view.getCollisionMaps()).thenReturn(new CollisionData[]{collision});
        when(player.getWorldLocation()).thenReturn(new WorldPoint(1601, 3969, 0));
        LocalPoint start = LocalPoint.fromScene(1, 1, view);
        when(player.getLocalLocation()).thenReturn(start);
        int[][] flags = new int[8][8];
        for (int x = 4; x <= 5; x++) { for (int y = 4; y <= 5; y++) { flags[x][y] = CollisionDataFlag.BLOCK_MOVEMENT_FULL; } }
        when(collision.getFlags()).thenReturn(flags);
        GameObject object = mock(GameObject.class);
        when(object.getSceneMinLocation()).thenReturn(new Point(4, 4));
        when(object.getSceneMaxLocation()).thenReturn(new Point(5, 5));
        WintertodtPathTracker tracker = new WintertodtPathTracker(); TrackerTestSupport.inject(tracker, "client", client);
        TrackerTestSupport.inject(tracker, "config", new WintertodtAutopilotConfig() {});
        WorldPoint target = new WorldPoint(1605, 3973, 0);
        tracker.update(target, object, null, true);
        List<WorldPoint> route = tracker.getPath(); assertFalse(route.isEmpty());
        WorldPoint end = route.get(route.size()-1);
        assertTrue(end.getX() == 1603 || end.getY() == 3971);
        tracker.update(target, object, null, true); assertSame(route, tracker.getPath());
        flags[2][1] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
        tracker.invalidate(); tracker.update(target, object, null, true);
        assertFalse(tracker.getPath().contains(new WorldPoint(1602, 3969, 0)));
        tracker.update(target, object, null, false); assertTrue(tracker.getPath().isEmpty());
        tracker.update(target, object, null, true); tracker.reset(); assertTrue(tracker.getPath().isEmpty());
    }
    @Test public void soloGapRoutesBothWaysAndRechecksLevelAndModeImmediately() throws Exception
    {
        Client client = mock(Client.class); Player player = mock(Player.class);
        WorldView view = mock(WorldView.class); CollisionData collision = mock(CollisionData.class);
        WintertodtAutopilotConfig config = mock(WintertodtAutopilotConfig.class);
        when(client.getLocalPlayer()).thenReturn(player); when(client.getTopLevelWorldView()).thenReturn(view);
        when(view.getBaseX()).thenReturn(1620); when(view.getBaseY()).thenReturn(4000);
        when(view.getSizeX()).thenReturn(24); when(view.getSizeY()).thenReturn(32);
        when(view.getCollisionMaps()).thenReturn(new CollisionData[]{collision});
        int[][] flags = new int[24][32];
        // Northern chasm: the southern walking route stays open, with three separated pillars.
        for (int x = 8; x <= 12; x++)
        {
            for (int y = 5; y < 32; y++) { flags[x][y] = CollisionDataFlag.BLOCK_MOVEMENT_FULL; }
        }
        flags[9][23] = 0; flags[11][23] = 0;
        when(collision.getFlags()).thenReturn(flags);
        WorldPoint west = new WorldPoint(1627, 4023, 0), east = new WorldPoint(1633, 4023, 0);
        when(player.getWorldLocation()).thenReturn(west);
        LocalPoint westLocal = LocalPoint.fromScene(7, 23, view), eastLocal = LocalPoint.fromScene(13, 23, view);
        when(player.getLocalLocation()).thenReturn(westLocal);
        when(config.mode()).thenReturn(WintertodtMode.SOLO);
        when(client.getRealSkillLevel(Skill.AGILITY)).thenReturn(60);
        when(client.getBoostedSkillLevel(Skill.AGILITY)).thenReturn(60);
        WintertodtPathTracker tracker = new WintertodtPathTracker();
        TrackerTestSupport.inject(tracker, "client", client); TrackerTestSupport.inject(tracker, "config", config);
        tracker.update(east, null, null, true);
        assertEquals(java.util.Arrays.asList(west, new WorldPoint(1629, 4023, 0),
            new WorldPoint(1631, 4023, 0), east), tracker.getPath());
        when(client.getBoostedSkillLevel(Skill.AGILITY)).thenReturn(59);
        tracker.update(east, null, null, true);
        assertTrue(tracker.getPath().contains(new WorldPoint(1628, 4004, 0)));
        when(client.getBoostedSkillLevel(Skill.AGILITY)).thenReturn(60);
        when(client.getRealSkillLevel(Skill.AGILITY)).thenReturn(59);
        tracker.update(east, null, null, true);
        assertTrue(tracker.getPath().size() > 4);
        when(client.getRealSkillLevel(Skill.AGILITY)).thenReturn(60);
        when(config.mode()).thenReturn(WintertodtMode.MASS);
        tracker.update(east, null, null, true); assertTrue(tracker.getPath().size() > 4);
        when(config.mode()).thenReturn(WintertodtMode.SOLO);
        when(player.getWorldLocation()).thenReturn(east);
        when(player.getLocalLocation()).thenReturn(eastLocal);
        tracker.update(west, null, null, true);
        assertEquals(east, tracker.getPath().get(0)); assertEquals(4, tracker.getPath().size());
        assertEquals(west, tracker.getPath().get(3));
        flags[9][23] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
        flags[11][23] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
        tracker.invalidate(); tracker.update(west, null, null, true);
        assertEquals(4, tracker.getPath().size());
        flags[9][23] = 0x1000000; tracker.invalidate();
        tracker.update(west, null, null, true); assertTrue(tracker.getPath().size() > 4);
        tracker.update(west, null, null, false); assertTrue(tracker.getPath().isEmpty());
    }
}
