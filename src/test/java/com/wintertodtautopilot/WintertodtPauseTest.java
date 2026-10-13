package com.wintertodtautopilot;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WintertodtPauseTest
{
    @Test public void pausedTicksKeepObservationsButClearGuidanceAndMuteNotifications() throws Exception
    {
        WintertodtAutopilotPlugin plugin = new WintertodtAutopilotPlugin();
        Client client = mock(Client.class); Player player = mock(Player.class);
        ClientThread thread = mock(ClientThread.class);
        WintertodtStateTracker tracker = mock(WintertodtStateTracker.class);
        WintertodtHintArrow arrow = mock(WintertodtHintArrow.class);
        WintertodtPathTracker paths = mock(WintertodtPathTracker.class);
        Notifier notifier = mock(Notifier.class);
        TrackerTestSupport.inject(plugin, "client", client);
        TrackerTestSupport.inject(plugin, "clientThread", thread);
        TrackerTestSupport.inject(plugin, "tracker", tracker);
        TrackerTestSupport.inject(plugin, "arrow", arrow);
        TrackerTestSupport.inject(plugin, "paths", paths);
        TrackerTestSupport.inject(plugin, "notifier", notifier);
        TrackerTestSupport.inject(plugin, "config", new WintertodtAutopilotConfig() { });
        TrackerTestSupport.inject(plugin, "bootstrapNeeded", false);
        doAnswer(call -> { call.getArgument(0, Runnable.class).run(); return null; }).when(thread).invoke(any(Runnable.class));
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getWorldLocation()).thenReturn(new WorldPoint(1639, 3997, 0));
        WintertodtSnapshot s = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).warmth(15)
            .warmthItem(1).points(100).workingCorner(WorkingCorner.SOUTHEAST).build();
        when(tracker.snapshot(any())).thenReturn(s);
        plugin.togglePause(); assertTrue(plugin.isPaused());
        plugin.onGameTick(new GameTick());
        assertEquals(s, plugin.getSnapshot());
        assertNotNull(plugin.getObjective()); assertNull(plugin.getTargetPoint());
        verify(arrow, atLeastOnce()).clear(); verify(paths, atLeastOnce()).reset();
        verifyNoInteractions(notifier);
        verify(arrow, never()).update(any(), any(), anyBoolean());
        plugin.togglePause(); assertFalse(plugin.isPaused());
        plugin.onGameTick(new GameTick());
        verify(notifier).notify("Wintertodt: low warmth - restore now");
        verify(arrow).update(null, null, true);
    }
}
