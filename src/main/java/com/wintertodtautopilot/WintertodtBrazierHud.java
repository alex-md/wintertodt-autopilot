package com.wintertodtautopilot;

import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/** Resolve compass positions from the current HUD layout, never legacy child offsets. */
final class WintertodtBrazierHud
{
    private static final int[] WIDGETS = {InterfaceID.WintStatus.BRAZIER1, InterfaceID.WintStatus.BRAZIER2,
        InterfaceID.WintStatus.BRAZIER3, InterfaceID.WintStatus.BRAZIER4};
    private WintertodtBrazierHud() { }

    static Map<WorkingCorner, BrazierState> read(Client client)
    {
        Map<WorkingCorner, BrazierState> result = new EnumMap<>(WorkingCorner.class);
        if (client == null) { return result; }
        Widget[] widgets = new Widget[4];
        Rectangle[] bounds = new Rectangle[4];
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = minX, maxY = maxX;
        for (int i = 0; i < 4; i++)
        {
            widgets[i] = client.getWidget(WIDGETS[i]);
            if (widgets[i] == null || widgets[i].isHidden()) { return result; }
            bounds[i] = widgets[i].getBounds();
            if (bounds[i] == null || bounds[i].width <= 0 || bounds[i].height <= 0) { return result; }
            int x = (int) bounds[i].getCenterX(), y = (int) bounds[i].getCenterY();
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
        }
        if (maxX - minX < 10 || maxY - minY < 10) { return result; }
        for (int i = 0; i < 4; i++)
        {
            boolean west = bounds[i].getCenterX() < (minX + maxX) / 2.0;
            boolean north = bounds[i].getCenterY() < (minY + maxY) / 2.0;
            WorkingCorner corner = north ? (west ? WorkingCorner.NORTHWEST : WorkingCorner.NORTHEAST)
                : (west ? WorkingCorner.SOUTHWEST : WorkingCorner.SOUTHEAST);
            if (result.containsKey(corner)) { result.clear(); return result; }
            result.put(corner, decode(widgets[i].getSpriteId()));
        }
        return result;
    }
    static BrazierState decode(int sprite)
    {
        // Sprite semantics checked against the pinned Solo Helper source; layout is read at runtime.
        switch (sprite)
        {
            case 1397: return BrazierState.BROKEN;
            case 1398: return BrazierState.UNLIT;
            case 1399: return BrazierState.LIT;
            default: return BrazierState.UNKNOWN;
        }
    }
}
