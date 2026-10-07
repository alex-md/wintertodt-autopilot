package com.wintertodtautopilot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class WintertodtOverlay extends OverlayPanel
{
    private final WintertodtAutopilotPlugin plugin;
    private final WintertodtAutopilotConfig config;
    @Inject
    WintertodtOverlay(WintertodtAutopilotPlugin plugin, WintertodtAutopilotConfig config)
    {
        super(plugin); this.plugin = plugin; this.config = config;
        setPosition(OverlayPosition.BOTTOM_LEFT);
        panelComponent.setPreferredSize(new Dimension(260, 0));
        addMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, OverlayManager.OPTION_CONFIGURE, "Wintertodt Autopilot");
    }
    @Override
    public Dimension render(Graphics2D graphics)
    {
        WintertodtSnapshot s = plugin.getSnapshot();
        WintertodtObjective o = plugin.getObjective();
        if (!config.showOverlay() || s.getRoundState() == RoundState.OUTSIDE || o == null) { return null; }
        Color color = color(o);
        if (config.overlayDetail() != OverlayDetail.MINIMAL)
        {
            panelComponent.getChildren().add(TitleComponent.builder().text("WINTERTODT AUTOPILOT").color(Color.LIGHT_GRAY).build());
        }
        panelComponent.getChildren().add(new WintertodtInstructionComponent(
            (WintertodtHighlight.needsRestart(s, o) ? "RESUME: " : s.isPlayerIdle() && o.actionable() ? "IDLE - " : "NEXT: ") + o.getInstruction(), color));
        int target = config.pointGoal().target(s.getPoints());
        if (s.getPoints() >= target) { target = PointGoal.NEXT_THRESHOLD.target(s.getPoints()); }
        line("Points", number(s.getPoints()) + " / " + target, s.rewardThresholdReached() ? Color.GREEN : Color.WHITE);
        if (config.overlayDetail() != OverlayDetail.MINIMAL)
        {
            line("Warmth", percent(s.getWarmth()), s.getWarmth() >= 0 && s.getWarmth() <= config.warmthWarning() ? Color.ORANGE : Color.WHITE);
            line("Potion doses / reserve", s.getRejuvenationDoses() + " / " + plugin.getPotionTargetDoses(), Color.WHITE);
            line("Roots / kindling", s.getBrumaRoots() + " / " + s.getBrumaKindling(), Color.WHITE);
            if (s.getRoundState() == RoundState.COUNTDOWN)
            {
                line("Starts in", s.getSecondsToStart() > 0 ? s.getSecondsToStart() + "s" : "Waiting", Color.WHITE);
            }
            line("Inventory points", "+" + s.inventoryPoints() + " (" + s.potentialPointsIfFletched() + " fletched)", Color.WHITE);
        }
        if (config.overlayDetail() == OverlayDetail.DETAILED || config.debug())
        {
            line("Wintertodt", percent(s.getWintertodtEnergy()) + " / " + s.getEndUrgency(), Color.WHITE);
            line("Projected total", number(s.projectedPoints()), Color.WHITE);
            line("Potential if fletched", s.getPoints() < 0 ? "Unknown" : Integer.toString(s.getPoints() + s.potentialPointsIfFletched()), Color.WHITE);
            line("Need for goal", s.getPoints() < 0 ? "Unknown" : Integer.toString(Math.max(0, target - s.getPoints())), Color.WHITE);
            line("Brazier", s.getLocalBrazierState().toString(), Color.WHITE);
            line("Pyromancer", !s.isLocalPyromancerKnown() ? "Unknown" : s.isLocalPyromancerIncapacitated() ? "Incapacitated" : "Healthy", Color.WHITE);
            line("Activity", s.getCurrentActivity().toString(), Color.WHITE);
            line("Firemaking / warm items", s.getFiremakingLevel() + " / " + (s.getWarmClothingCount() < 0 ? "Unknown" : s.getWarmClothingCount()), Color.WHITE);
            line("Corner", s.getWorkingCorner().toString(), Color.WHITE);
        }
        if (config.debug())
        {
            line("Round", s.getRoundState().toString(), Color.GRAY);
            line("Objective", o.getType().toString(), Color.GRAY);
            line("Reason", o.getReason(), Color.GRAY);
            line("Interaction", s.getInteraction(), Color.GRAY);
            line("Food / doses", s.getFoodCount() + " / " + s.getRejuvenationDoses(), Color.GRAY);
            line("Idle ticks", Integer.toString(s.getInactiveTicks()), Color.GRAY);
        }
        return super.render(graphics);
    }
    static Color color(WintertodtObjective o)
    {
        if (o.isUrgent()) { return o.getType() == ObjectiveType.RESTORE_WARMTH ? Color.RED : Color.ORANGE; }
        if (!o.actionable()) { return new Color(130, 200, 150); }
        return new Color(180, 70, 240); // Saturated violet stays distinct against snow and ice.
    }
    private static String number(int n) { return n < 0 ? "Unknown" : Integer.toString(n); }
    private static String percent(int n) { return n < 0 ? "Unknown" : n + "%"; }
    private void line(String left, String right, Color color)
    {
        panelComponent.getChildren().add(LineComponent.builder().left(left + ":").leftColor(Color.LIGHT_GRAY).right(right).rightColor(color).build());
    }
}
