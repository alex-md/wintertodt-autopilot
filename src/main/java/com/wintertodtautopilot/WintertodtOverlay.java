package com.wintertodtautopilot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Font;
import javax.inject.Inject;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.LineComponent;

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
        if (plugin.isPaused())
        {
            panelComponent.getChildren().add(new WintertodtInstructionComponent("Guidance paused", Color.LIGHT_GRAY));
            line("Resume", "Wintertodt sidebar", Color.LIGHT_GRAY);
            return super.render(graphics);
        }
        Color color = color(o);
        if (config.overlayDetail() != OverlayDetail.MINIMAL)
        {
            panelComponent.getChildren().add(LineComponent.builder().left("WINTERTODT AUTOPILOT").leftColor(Color.LIGHT_GRAY)
                .leftFont(new Font(Font.SANS_SERIF, Font.BOLD, 11)).build());
        }
        panelComponent.getChildren().add(new WintertodtInstructionComponent(
            (WintertodtHighlight.needsRestart(s, o) ? "RESUME: " : s.isPlayerIdle() && o.actionable() ? "IDLE - " : "NEXT: ") + o.getInstruction(), o.isUrgent() ? color : Color.WHITE));
        int target = WintertodtGuidance.rewardTarget(config.pointGoal(), s.getPoints());
        line("Points", number(s.getPoints()) + " / " + target, s.rewardThresholdReached() ? Color.GREEN : Color.WHITE);
        if (config.overlayDetail() != OverlayDetail.MINIMAL)
        {
            if (s.active())
            {
                line("Estimated finish", s.getEstimatedSecondsLeft() < 0 ? "Learning pace" : "~" + s.getEstimatedSecondsLeft() + "s", Color.LIGHT_GRAY);
            }
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
            line("Usable time budget", s.getRoundBudgetSeconds() < 0 ? "Unknown" : s.getRoundBudgetSeconds() + "s", Color.LIGHT_GRAY);
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
            line("Raw warmth / restore at", s.getRawWarmth() + " / " + config.restoreWarmth() + "%", Color.GRAY);
            line("Round", s.getRoundState().toString(), Color.GRAY);
            line("Objective", o.getType().toString(), Color.GRAY);
            line("Reason", o.getReason(), Color.GRAY);
            line("Interaction", s.getInteraction(), Color.GRAY);
            line("Food / doses", s.getFoodCount() + " / " + s.getRejuvenationDoses(), Color.GRAY);
            line("Idle ticks", Integer.toString(s.getInactiveTicks()), Color.GRAY);
        }
        Dimension size = super.render(graphics);
        if (size != null && config.pulseInterruptions() && WintertodtHighlight.needsAttention(s, o))
        {
            Graphics2D g = (Graphics2D) graphics.create();
            try
            {
                float strength = WintertodtHighlight.strength(s, o, true);
                g.setColor(WintertodtHighlight.alpha(color, (int) (255 * strength)));
                g.setStroke(new java.awt.BasicStroke(1 + 2 * strength));
                g.drawRoundRect(1, 1, Math.max(0, size.width - 3), Math.max(0, size.height - 3), 6, 6);
            }
            finally { g.dispose(); }
        }
        return size;
    }
    static Color color(WintertodtObjective o)
    {
        if (o.getType() == ObjectiveType.RESTORE_WARMTH) { return o.isUrgent() ? Color.RED : Color.ORANGE; }
        if (o.isUrgent()) { return Color.ORANGE; }
        if (!o.actionable()) { return new Color(130, 200, 150); }
        return new Color(0, 160, 145);
    }
    private static String number(int n) { return n < 0 ? "Unknown" : Integer.toString(n); }
    private static String percent(int n) { return n < 0 ? "Unknown" : n + "%"; }
    private void line(String left, String right, Color color)
    {
        panelComponent.getChildren().add(LineComponent.builder().left(left + ":").leftColor(Color.LIGHT_GRAY)
            .leftFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12)).rightFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12)).right(right).rightColor(color).build());
    }
}
