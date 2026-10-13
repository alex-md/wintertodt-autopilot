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
    static boolean needsAttention(WintertodtSnapshot s, WintertodtObjective o)
    {
        boolean blocked = o != null && ("inventory_blocked".equals(o.getReason()) || "supply_inventory_blocked".equals(o.getReason()));
        if (o == null || (!s.active() && o.getType() != ObjectiveType.RESTORE_WARMTH)
            || (!o.actionable() && !blocked)
            || (s.getCurrentActivity() == PlayerActivity.MOVING && o.getType() != ObjectiveType.RESTORE_WARMTH))
        { return false; }
        PlayerActivity activity;
        switch (o.getType())
        {
            case RESTORE_WARMTH: activity = PlayerActivity.RESTORING_WARMTH; break;
            case REPAIR: activity = PlayerActivity.REPAIRING; break;
            case LIGHT: activity = PlayerActivity.LIGHTING; break;
            case HEAL_PYROMANCER: activity = PlayerActivity.HEALING; break;
            case FEED: activity = PlayerActivity.FEEDING; break;
            case FLETCH: activity = PlayerActivity.FLETCHING; break;
            case CHOP: activity = PlayerActivity.CHOPPING; break;
            case MAKE_REJUVENATION: activity = PlayerActivity.MAKING_POTION; break;
            default: activity = PlayerActivity.MOVING; break;
        }
        if (s.getCurrentActivity() == activity) { return false; }
        return blocked || needsRestart(s, o) || s.isPlayerIdle() || o.isUrgent()
            || o.getType() == ObjectiveType.RESTORE_WARMTH || o.getType() == ObjectiveType.REPAIR
            || o.getType() == ObjectiveType.LIGHT || o.getType() == ObjectiveType.HEAL_PYROMANCER
            || o.getType() == ObjectiveType.FEED && (s.getCurrentActivity() == PlayerActivity.FLETCHING || s.getCurrentActivity() == PlayerActivity.CHOPPING)
            || o.getType() == ObjectiveType.FLETCH && s.getCurrentActivity() == PlayerActivity.CHOPPING;
    }
    static float strength(WintertodtSnapshot snapshot, WintertodtObjective objective, boolean enabled)
    {
        return strengthAt(snapshot, objective, enabled, System.nanoTime());
    }
    static float strengthAt(WintertodtSnapshot snapshot, WintertodtObjective objective, boolean enabled, long nanos)
    {
        return enabled && needsAttention(snapshot, objective)
            ? (float) (.625 + .375 * Math.sin(nanos / 1_000_000_000.0 * Math.PI * 2 / 1.2)) : 1;
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
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(alpha(color, (int) ((destination ? 28 : 10) * strength))); g.fillPolygon(tile);
            g.setStroke(new BasicStroke(destination ? 4 : 2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0, 0, 0, 160)); g.drawPolygon(tile);
            softEdge(g, tile, color, destination ? 1.5f + strength : 1, strength);
            g.setStroke(new BasicStroke(destination ? 1.5f + strength : 1));
            g.setColor(alpha(color, (int) (240 * strength))); g.drawPolygon(tile);
        }
        finally { g.dispose(); }
    }
    static void routeEdge(Graphics2D graphics, net.runelite.api.Point a, net.runelite.api.Point b, Color color, float strength)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0, 0, 0, 165)); g.drawLine(a.getX(), a.getY(), b.getX(), b.getY());
            softEdge(g, new java.awt.geom.Line2D.Float(a.getX(), a.getY(), b.getX(), b.getY()), color, 1.6f, strength);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(alpha(color, (int) (235 * strength))); g.drawLine(a.getX(), a.getY(), b.getX(), b.getY());
            g.setStroke(new BasicStroke(.5f)); g.setColor(new Color(255, 255, 255, (int) (130 * strength)));
            g.drawLine(a.getX(), a.getY(), b.getX(), b.getY());
        }
        finally { g.dispose(); }
    }
    static void inventory(Graphics2D graphics, Rectangle bounds, Color color, float strength)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(alpha(color, (int) (30 * strength))); g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 7, 7);
            g.setStroke(new BasicStroke(4)); g.setColor(new Color(0, 0, 0, 180));
            g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 7, 7);
            softEdge(g, new java.awt.geom.RoundRectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height, 7, 7),
                color, 1 + 3 * strength, strength);
            g.setStroke(new BasicStroke(1 + 3 * strength)); g.setColor(alpha(color, (int) (250 * strength)));
            g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 7, 7);
        }
        finally { g.dispose(); }
    }
    private static void softEdge(Graphics2D g, Shape shape, Color color, float width, float strength)
    {
        g.setStroke(new BasicStroke(width + 3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(alpha(color, (int) (18 * strength))); g.draw(shape);
        g.setStroke(new BasicStroke(width + 1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(alpha(color, (int) (42 * strength))); g.draw(shape);
    }

}
