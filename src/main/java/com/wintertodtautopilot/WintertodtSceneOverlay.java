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
        if (plugin.isPaused() || o == null || s.getRoundState() == RoundState.OUTSIDE
            || WintertodtGuidance.performing(s, o)) { return null; }
        Color color = WintertodtOverlay.color(o);
        float strength = WintertodtHighlight.strength(s, o, config.pulseInterruptions());
        if (config.highlightTarget())
        {
            NPC npc = plugin.getTargetNpc(); TileObject object = plugin.getTargetObject();
            int width = Math.max(1, Math.min(4, config.highlightBorderWidth()));
            Color outline = WintertodtHighlight.alpha(color, (int) (255 * strength));
            if (npc != null) { outlines.drawOutline(npc, width, outline, 1); }
            else if (object != null) { outlines.drawOutline(object, width, outline, 1); }
        }
        if (!config.showPath() && !config.highlightStandingTiles()) { return null; }
        // Isolate the clip: paths and floor markers must never paint over the UI.
        Graphics2D floor = (Graphics2D) graphics.create();
        try
        {
            floor.clip(new java.awt.Rectangle(client.getViewportXOffset(), client.getViewportYOffset(),
                client.getViewportWidth(), client.getViewportHeight()));
            List<WorldPoint> path = plugin.getPath();
            if (config.showPath())
            {
                Point previous = null;
                // Only join validated tile edges; do not bridge missing projections.
                for (WorldPoint step : path)
                {
                    LocalPoint local = LocalPoint.fromWorld(client, step);
                    Point next = local == null ? null : Perspective.localToCanvas(client, local, step.getPlane());
                    if (previous != null && next != null) { WintertodtHighlight.routeEdge(floor, previous, next, color, strength); }
                    previous = next;
                }
            }
            if (config.highlightStandingTiles())
            {
                WorldPoint destination = standingPoint(s, o, path);
                Polygon tile = destination == null ? null : tile(destination);
                if (tile != null)
                {
                    WintertodtHighlight.tile(floor, tile, color, strength, true);
                }
            }
        }
        finally { floor.dispose(); }
        return null;
    }
    private Polygon tile(WorldPoint point)
    {
        LocalPoint local = LocalPoint.fromWorld(client, point);
        return local == null ? null : Perspective.getCanvasTilePoly(client, local);
    }
    static WorldPoint standingPoint(WintertodtSnapshot s, WintertodtObjective o, List<WorldPoint> path)
    {
        if (o.getTarget() == TargetKind.NONE && s.getCurrentActivity() != PlayerActivity.MOVING
            && (o.getType() == ObjectiveType.FLETCH || o.getType() == ObjectiveType.MAKE_REJUVENATION))
        { return s.getPlayerLocation(); }
        return path.isEmpty() ? null : path.get(path.size() - 1);
    }
}
