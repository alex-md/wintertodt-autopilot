package com.wintertodtautopilot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Text is composited after actors/overheads and before interface widgets. */
public class WintertodtActionLabelOverlay extends Overlay
{
    private final Client client;
    private final WintertodtAutopilotPlugin plugin;
    private final WintertodtAutopilotConfig config;

    @Inject
    WintertodtActionLabelOverlay(Client client, WintertodtAutopilotPlugin plugin, WintertodtAutopilotConfig config)
    {
        this.client = client; this.plugin = plugin; this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.UNDER_WIDGETS);
    }
    @Override
    public Dimension render(Graphics2D graphics)
    {
        WintertodtObjective o = plugin.getObjective();
        WintertodtSnapshot s = plugin.getSnapshot();
        if (plugin.isPaused() || !config.highlightStandingTiles() || o == null || s.getRoundState() == RoundState.OUTSIDE) { return null; }
        WorldPoint destination = WintertodtSceneOverlay.standingPoint(s, o, plugin.getPath());
        LocalPoint local = destination == null ? null : LocalPoint.fromWorld(client, destination);
        Polygon tile = local == null ? null : Perspective.getCanvasTilePoly(client, local);
        if (tile == null) { return null; }
        Rectangle viewport = new Rectangle(client.getViewportXOffset(), client.getViewportYOffset(),
            client.getViewportWidth(), client.getViewportHeight());
        draw(graphics, tile.getBounds(), viewport, WintertodtSceneOverlay.standingLabel(o), WintertodtOverlay.color(o));
        return null;
    }

    static void draw(Graphics2D graphics, Rectangle tile, Rectangle viewport, String text, Color color)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            Rectangle box = placement(tile, viewport, g.getFontMetrics().stringWidth(text) + 16,
                g.getFontMetrics().getHeight() + 10);
            if (box == null) { return; }
            g.clip(viewport);
            int anchorX = (int) tile.getCenterX(), anchorY = (int) tile.getCenterY();
            int endX = Math.max(box.x, Math.min(anchorX, box.x + box.width));
            int endY = Math.max(box.y, Math.min(anchorY, box.y + box.height));
            g.setStroke(new BasicStroke(3)); g.setColor(new Color(0, 0, 0, 180));
            g.drawLine(anchorX, anchorY, endX, endY);
            g.setStroke(new BasicStroke(1.2f)); g.setColor(color);
            g.drawLine(anchorX, anchorY, endX, endY);
            g.setColor(new Color(18, 23, 30, 240));
            g.fillRoundRect(box.x, box.y, box.width, box.height, 7, 7);
            g.setStroke(new BasicStroke(1)); g.setColor(color);
            g.drawRoundRect(box.x, box.y, box.width, box.height, 7, 7);
            g.setColor(Color.WHITE);
            g.drawString(text, box.x + 8, box.y + 5 + g.getFontMetrics().getAscent());
        }
        finally { g.dispose(); }
    }

    /** Offset from the actor's feet, switch sides near the edge, and stay inside the game viewport. */
    static Rectangle placement(Rectangle tile, Rectangle viewport, int width, int height)
    {
        if (!viewport.contains(tile.getCenterX(), tile.getCenterY()) || width > viewport.width - 8 || height > viewport.height - 8)
        { return null; }
        int x = tile.x + tile.width + 10;
        if (x + width > viewport.x + viewport.width - 4) { x = tile.x - width - 10; }
        x = Math.max(viewport.x + 4, Math.min(x, viewport.x + viewport.width - width - 4));
        int y = (int) tile.getCenterY() - height - 12;
        y = Math.max(viewport.y + 4, Math.min(y, viewport.y + viewport.height - height - 4));
        return new Rectangle(x, y, width, height);
    }
}
