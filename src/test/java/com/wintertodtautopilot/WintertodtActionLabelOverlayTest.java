package com.wintertodtautopilot;

import java.awt.Rectangle;
import org.junit.Test;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayLayer;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WintertodtActionLabelOverlayTest
{
    @Test public void labelsRenderInForegroundAndSitBesideTheStandingTile()
    {
        WintertodtActionLabelOverlay overlay = new WintertodtActionLabelOverlay(mock(Client.class),
            mock(WintertodtAutopilotPlugin.class), new WintertodtAutopilotConfig() { });
        assertEquals(OverlayLayer.UNDER_WIDGETS, overlay.getLayer());
        Rectangle tile = new Rectangle(200, 200, 60, 40);
        Rectangle box = WintertodtActionLabelOverlay.placement(tile, new Rectangle(0, 0, 800, 600), 100, 24);
        assertTrue(box.x > tile.x + tile.width);
        assertFalse(box.intersects(tile));
    }
    @Test public void rightEdgeSwitchesSidesAndTopEdgeClampsInsideViewport()
    {
        Rectangle viewport = new Rectangle(25, 30, 500, 300);
        Rectangle tile = new Rectangle(450, 35, 50, 30);
        Rectangle box = WintertodtActionLabelOverlay.placement(tile, viewport, 110, 26);
        assertTrue(box.x + box.width < tile.x);
        assertTrue(viewport.contains(box));
        assertEquals(viewport.y + 4, box.y);
    }
    @Test public void offscreenAndUndersizedViewportsDoNotProduceFloatingLabels()
    {
        Rectangle tile = new Rectangle(300, 200, 60, 40);
        assertNull(WintertodtActionLabelOverlay.placement(tile, new Rectangle(0, 0, 100, 100), 80, 24));
        assertNull(WintertodtActionLabelOverlay.placement(tile, new Rectangle(300, 200, 60, 40), 100, 24));
    }
}
