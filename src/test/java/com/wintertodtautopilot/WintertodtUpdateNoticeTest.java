package com.wintertodtautopilot;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class WintertodtUpdateNoticeTest
{
    @Test public void waitsForLoginAndPersistsAcrossTicksAndPluginRestarts() throws Exception
    {
        Client client = mock(Client.class);
        ConfigManager manager = mock(ConfigManager.class);
        Map<String, String> saved = new HashMap<>();
        when(manager.getConfiguration("wintertodtautopilot", WintertodtAutopilotPlugin.UPDATE_NOTICE_KEY))
            .thenAnswer(call -> saved.get("notice"));
        doAnswer(call -> { saved.put("notice", call.getArgument(2)); return null; }).when(manager)
            .setConfiguration(eq("wintertodtautopilot"), eq(WintertodtAutopilotPlugin.UPDATE_NOTICE_KEY), anyString());
        WintertodtAutopilotPlugin plugin = plugin(client, manager);
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        plugin.onGameTick(new GameTick());
        verifyNoInteractions(manager);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        plugin.onGameTick(new GameTick());
        plugin.onGameTick(new GameTick());
        when(client.getGameState()).thenReturn(GameState.HOPPING);
        plugin.onGameTick(new GameTick());
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        plugin(client, manager).onGameTick(new GameTick());
        verify(client, times(1)).addChatMessage(ChatMessageType.GAMEMESSAGE, "", WintertodtAutopilotPlugin.UPDATE_NOTICE, null);
        verify(manager, times(1)).setConfiguration("wintertodtautopilot", WintertodtAutopilotPlugin.UPDATE_NOTICE_KEY,
            WintertodtAutopilotPlugin.UPDATE_NOTICE_VERSION);
        // A separate RuneLite profile has its own saved configuration.
        saved.clear();
        plugin.onGameTick(new GameTick());
        verify(client, times(2)).addChatMessage(ChatMessageType.GAMEMESSAGE, "", WintertodtAutopilotPlugin.UPDATE_NOTICE, null);
    }

    @Test public void olderNoticeMarkerStillReceivesThisUpdate() throws Exception
    {
        Client client = mock(Client.class);
        ConfigManager manager = mock(ConfigManager.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(manager.getConfiguration("wintertodtautopilot", WintertodtAutopilotPlugin.UPDATE_NOTICE_KEY)).thenReturn("0.2.1");
        plugin(client, manager).onGameTick(new GameTick());
        verify(client).addChatMessage(ChatMessageType.GAMEMESSAGE, "", WintertodtAutopilotPlugin.UPDATE_NOTICE, null);
    }

    @Test public void savedCurrentReleaseMarkerDoesNotRepeatTheNotice() throws Exception
    {
        Client client = mock(Client.class);
        ConfigManager manager = mock(ConfigManager.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(manager.getConfiguration("wintertodtautopilot", WintertodtAutopilotPlugin.UPDATE_NOTICE_KEY))
            .thenReturn("0.3.0");
        plugin(client, manager).onGameTick(new GameTick());
        verify(client, never()).addChatMessage(any(), anyString(), anyString(), any());
        verify(manager, never()).setConfiguration(anyString(), anyString(), any());
    }

    private WintertodtAutopilotPlugin plugin(Client client, ConfigManager manager) throws Exception
    {
        WintertodtAutopilotPlugin plugin = new WintertodtAutopilotPlugin();
        TrackerTestSupport.inject(plugin, "client", client);
        TrackerTestSupport.inject(plugin, "configManager", manager);
        // No player, region, or gameplay-guidance dependencies: the notice works anywhere, even paused.
        return plugin;
    }
}
