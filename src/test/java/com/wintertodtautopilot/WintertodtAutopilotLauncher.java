package com.wintertodtautopilot;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class WintertodtAutopilotLauncher
{
    private WintertodtAutopilotLauncher() { }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(WintertodtAutopilotPlugin.class);
        RuneLite.main(args);
    }
}
