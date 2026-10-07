package com.wintertodtautopilot;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

public class WintertodtInventoryOverlay extends WidgetItemOverlay
{
    private final WintertodtAutopilotPlugin plugin;
    private final WintertodtAutopilotConfig config;
    @Inject
    WintertodtInventoryOverlay(WintertodtAutopilotPlugin plugin, WintertodtAutopilotConfig config)
    {
        this.plugin = plugin; this.config = config; showOnInventory();
    }
    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem item)
    {
        WintertodtObjective o = plugin.getObjective();
        if (!config.highlightInventory() || o == null || !o.getItemIds().contains(itemId)) { return; }
        Rectangle bounds = item.getCanvasBounds();
        if (bounds == null) { return; }
        WintertodtHighlight.inventory(graphics, bounds, WintertodtOverlay.color(o),
            WintertodtHighlight.strength(plugin.getSnapshot(), o, config.pulseInterruptions()));
    }
}
