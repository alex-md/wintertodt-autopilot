package com.wintertodtautopilot;

import java.awt.*;

/** Shared scene/inventory animation, with no mutation of planner state. */
final class WintertodtHighlight
{
    private WintertodtHighlight() { }
    static boolean needsRestart(WintertodtSnapshot snapshot, WintertodtObjective objective)
    {
        return snapshot.active() && snapshot.isInterrupted() && objective != null && objective.actionable()
            && snapshot.getCurrentActivity() == PlayerActivity.IDLE;
    }
    static float strength(WintertodtSnapshot snapshot, WintertodtObjective objective, boolean enabled)
    {
        return enabled && needsRestart(snapshot, objective)
            ? (float) (0.85 + 0.15 * Math.sin(System.nanoTime() / 1_000_000_000.0 * Math.PI * 2 / 1.4)) : 1;
    }
    static Color alpha(Color color, int alpha)
    {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
    static void tile(Graphics2D graphics, Polygon tile, Color color, float strength, boolean destination)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setColor(alpha(color, (int) ((destination ? 65 : 18) * strength))); g.fillPolygon(tile);
            g.setStroke(new BasicStroke(destination ? 7 : 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0, 0, 0, 160)); g.drawPolygon(tile);
            g.setStroke(new BasicStroke(destination ? 3 + strength : 2));
            g.setColor(alpha(color, (int) (240 * strength))); g.drawPolygon(tile);
        }
        finally { g.dispose(); }
    }
    static void routeEdge(Graphics2D graphics, net.runelite.api.Point a, net.runelite.api.Point b, Color color, float strength)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0, 0, 0, 165)); g.drawLine(a.getX(), a.getY(), b.getX(), b.getY());
            g.setStroke(new BasicStroke(4 + strength, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(alpha(color, (int) (235 * strength))); g.drawLine(a.getX(), a.getY(), b.getX(), b.getY());
            g.setStroke(new BasicStroke(1.2f)); g.setColor(new Color(255, 255, 255, (int) (130 * strength)));
            g.drawLine(a.getX(), a.getY(), b.getX(), b.getY());
        }
        finally { g.dispose(); }
    }
    static void inventory(Graphics2D graphics, Rectangle bounds, Color color, float strength)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setColor(alpha(color, (int) (30 * strength))); g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 7, 7);
            g.setStroke(new BasicStroke(6)); g.setColor(new Color(0, 0, 0, 180));
            g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 7, 7);
            g.setStroke(new BasicStroke(2 + 2 * strength)); g.setColor(alpha(color, (int) (250 * strength)));
            g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 7, 7);
        }
        finally { g.dispose(); }
    }
}
