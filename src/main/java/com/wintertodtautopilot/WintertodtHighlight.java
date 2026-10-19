package com.wintertodtautopilot;

import java.awt.*;
import net.runelite.api.gameval.ItemID;

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
        if (WintertodtGuidance.performing(s, o)) { return false; }
        return blocked || needsRestart(s, o) || s.isPlayerIdle() || o.isUrgent()
            || o.getType() == ObjectiveType.RESTORE_WARMTH || o.getType() == ObjectiveType.REPAIR
            || o.getType() == ObjectiveType.LIGHT || o.getType() == ObjectiveType.HEAL_PYROMANCER
            || o.getType() == ObjectiveType.FEED && (s.getCurrentActivity() == PlayerActivity.FLETCHING || s.getCurrentActivity() == PlayerActivity.CHOPPING)
            || o.getType() == ObjectiveType.FLETCH && s.getCurrentActivity() == PlayerActivity.CHOPPING;
    }
    static boolean inventoryItem(WintertodtSnapshot s, WintertodtObjective o, int itemId)
    {
        if (o == null || s.getRoundState() == RoundState.OUTSIDE || WintertodtGuidance.performing(s, o)) { return false; }
        // Heal is a click on the pyromancer. A potion border suggests drinking,
        // so keep attention on the NPC even though healing consumes a dose.
        if (o.getType() == ObjectiveType.HEAL_PYROMANCER) { return false; }
        return o.getItemIds().contains(itemId);
    }

    static Color inventoryColor(WintertodtObjective objective, int itemId)
    {
        // Keep the tool amber and give its fletching target a softer gold tone.
        return objective.getType() == ObjectiveType.FLETCH && itemId == ItemID.WINT_BRUMA_ROOT
            ? new Color(255, 215, 130) : WintertodtOverlay.color(objective);
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
        inventory(graphics, bounds, color, strength, 1);
    }
    static void inventory(Graphics2D graphics, Rectangle bounds, Color color, float strength, int borderWidth)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(alpha(color, (int) (30 * strength))); g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 7, 7);
            float width = Math.max(1, Math.min(4, borderWidth));
            g.setStroke(new BasicStroke(width + 1)); g.setColor(new Color(0, 0, 0, (int) (100 * strength)));
            g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 7, 7);
            g.setStroke(new BasicStroke(width)); g.setColor(alpha(color, (int) (250 * strength)));
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
