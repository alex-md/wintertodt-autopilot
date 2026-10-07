package com.wintertodtautopilot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

public class WintertodtSceneOverlay extends Overlay
{
    private final Client client;
    private final WintertodtAutopilotPlugin plugin;
    private final WintertodtAutopilotConfig config;
    private final ModelOutlineRenderer outlines;
    @Inject
    WintertodtSceneOverlay(Client client, WintertodtAutopilotPlugin plugin,
        WintertodtAutopilotConfig config, ModelOutlineRenderer outlines)
    {
        this.client = client; this.plugin = plugin; this.config = config; this.outlines = outlines;
        setPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ABOVE_SCENE);
    }
    @Override
    public Dimension render(Graphics2D graphics)
    {
        WintertodtObjective o = plugin.getObjective();
        WintertodtSnapshot s = plugin.getSnapshot();
        if (o == null || s.getRoundState() == RoundState.OUTSIDE) { return null; }
        Color color = WintertodtOverlay.color(o);
        float strength = WintertodtHighlight.strength(s, o, config.pulseInterruptions());
        if (config.highlightTarget())
        {
            NPC npc = plugin.getTargetNpc(); TileObject object = plugin.getTargetObject();
            int width = Math.round(3 + 2 * strength);
            Color outline = WintertodtHighlight.alpha(color, (int) (255 * strength));
            if (npc != null) { outlines.drawOutline(npc, width, outline, 2); }
            else if (object != null) { outlines.drawOutline(object, width, outline, 2); }
        }
        List<WorldPoint> path = plugin.getPath();
        if (config.showPath())
        {
            Point previous = null;
            // Only join validated tile edges; do not bridge missing projections.
            for (WorldPoint step : path)
            {
                LocalPoint local = LocalPoint.fromWorld(client, step);
                Point next = local == null ? null : Perspective.localToCanvas(client, local, step.getPlane());
                if (previous != null && next != null) { WintertodtHighlight.routeEdge(graphics, previous, next, color, strength); }
                previous = next;
            }
        }
        if (config.highlightStandingTiles())
        {
            // Breadcrumbs provide floor context without painting every tile in the scene.
            for (int i = 3; i < path.size() - 1; i += 4)
            {
                Polygon tile = tile(path.get(i));
                if (tile != null) { WintertodtHighlight.tile(graphics, tile, color, strength, false); }
            }
            WorldPoint destination = path.isEmpty() ? null : path.get(path.size() - 1);
            if (o.getTarget() == TargetKind.NONE && s.getCurrentActivity() != PlayerActivity.MOVING
                && (o.getType() == ObjectiveType.FLETCH || o.getType() == ObjectiveType.MAKE_REJUVENATION))
            { destination = s.getPlayerLocation(); }
            Polygon tile = destination == null ? null : tile(destination);
            if (tile != null)
            {
                WintertodtHighlight.tile(graphics, tile, color, strength, true);
                label(graphics, tile, standingLabel(o), color);
            }
        }
        return null;
    }
    private Polygon tile(WorldPoint point)
    {
        LocalPoint local = LocalPoint.fromWorld(client, point);
        return local == null ? null : Perspective.getCanvasTilePoly(client, local);
    }
    static String standingLabel(WintertodtObjective objective)
    {
        switch (objective.getType())
        {
            case CHOP: return "CHOP HERE";
            case FEED: return "FEED HERE";
            case FLETCH: return "FLETCH HERE";
            case REPAIR: return "REPAIR HERE";
            case LIGHT: return "LIGHT HERE";
            case HEAL_PYROMANCER: return "HEAL HERE";
            case GET_TOOL: return "TAKE HERE";
            case MAKE_REJUVENATION: return objective.getTarget() == TargetKind.NONE ? "MIX HERE" : "SUPPLIES HERE";
            case ENTER: return "ENTER HERE";
            case RESTORE_WARMTH: return "SAFE AREA";
            default: return "STAND HERE";
        }
    }
    private void label(Graphics2D graphics, Polygon tile, String text, Color color)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, g.getFont().getSize2D() + 1));
            java.awt.Rectangle bounds = tile.getBounds();
            TextComponent label = new TextComponent(); label.setText(text); label.setColor(color);
            label.setPosition(new java.awt.Point((int) bounds.getCenterX() - g.getFontMetrics().stringWidth(text) / 2,
                (int) bounds.getCenterY() - 7));
            label.render(g);
        }
        finally { g.dispose(); }
    }
}
