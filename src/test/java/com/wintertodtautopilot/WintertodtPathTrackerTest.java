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
}
