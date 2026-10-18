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
    private static final Color FLAME = new Color(255, 180, 40);
    private static final Color SUPPLY_FLAME = new Color(255, 210, 75);
    private final WintertodtAutopilotPlugin plugin;
    private final WintertodtAutopilotConfig config;
    @Inject
    WintertodtOverlay(WintertodtAutopilotPlugin plugin, WintertodtAutopilotConfig config)
    {
        super(plugin); this.plugin = plugin; this.config = config;
        setPosition(OverlayPosition.BOTTOM_LEFT);
        panelComponent.setBackgroundColor(new Color(24, 29, 33, 235));
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
            body("Uncheck Pause guidance in plugin settings.");
            body("All guidance and notifications are paused.");
            return super.render(graphics);
        }
        Color color = color(o);
        Color textColor = o.getType() == ObjectiveType.RESTORE_WARMTH && o.isUrgent()
            ? new Color(255, 145, 130) : color;
        panelComponent.getChildren().add(LineComponent.builder()
            .left(WintertodtGuidance.status(s, o)).leftColor(o.isUrgent() ? textColor : FLAME)
            .leftFont(new Font(Font.SANS_SERIF, Font.BOLD, 11)).build());
        panelComponent.getChildren().add(new WintertodtInstructionComponent(o.getInstruction(), o.isUrgent() ? textColor : Color.WHITE));
        if (config.overlayDetail() != OverlayDetail.MINIMAL)
        {
            body(WintertodtGuidance.reassurance(s, o));
        }
        line("Warmth", percent(s.getWarmth()), s.getWarmth() >= 0 && s.getWarmth() <= config.warmthWarning() ? Color.ORANGE : Color.WHITE);
        boolean soloMode = config.mode() == WintertodtMode.SOLO;
        int target = soloMode ? Math.max(500, Math.min(100000, config.soloPointGoal()))
            : WintertodtGuidance.rewardTarget(config.pointGoal(), s.getPoints());
        if (soloMode)
        {
            line("Solo phase", plugin.getSoloPhase().toString(), new Color(130, 200, 150));
            if (plugin.isSoloFinishRequested()) { body("Finish this round requested"); }
            line("Energy / trend", percent(s.getWintertodtEnergy()) + " / " + plugin.getSoloEnergyTrend(), Color.LIGHT_GRAY);
        }
        if (s.rewardThresholdReached())
        {
            line("Reward secured", "500 points", new Color(130, 200, 150));
        }
        line(soloMode ? "Solo goal" : s.rewardThresholdReached() ? "Next target" : "Reward progress",
            number(s.getPoints()) + " / " + target, Color.WHITE);
        if (config.overlayDetail() != OverlayDetail.MINIMAL)
        {
            if (s.inventoryPoints() > 0) { line("Fuel to feed", "+" + s.inventoryPoints() + " pts", Color.LIGHT_GRAY); }
            if (s.active() && !soloMode)
            {
                line("Round ends in", s.getEstimatedSecondsLeft() < 0 ? "Learning pace" : "~" + s.getEstimatedSecondsLeft() + "s", Color.LIGHT_GRAY);
            }
            else if (!s.active()) { body(WintertodtGuidance.phase(s)); }
        }
        if (config.overlayDetail() == OverlayDetail.DETAILED || config.debug())
        {
            body(WintertodtGuidance.explanation(o));
            line("Potion doses / reserve", s.getRejuvenationDoses() + " / " + plugin.getPotionTargetDoses(), Color.WHITE);
            line("Roots / kindling", s.getBrumaRoots() + " / " + s.getBrumaKindling(), Color.WHITE);
            if (!soloMode) { line("Usable time budget", s.getRoundBudgetSeconds() < 0 ? "Unknown" : s.getRoundBudgetSeconds() + "s", Color.LIGHT_GRAY); }
            line("Wintertodt", percent(s.getWintertodtEnergy()) + (soloMode ? "" : " / " + s.getEndUrgency()), Color.WHITE);
            line("Projected total", number(s.projectedPoints()), Color.WHITE);
            line("Potential if fletched", s.getPoints() < 0 ? "Unknown" : Integer.toString(s.getPoints() + s.potentialPointsIfFletched()), Color.WHITE);
            line("Need for goal", s.getPoints() < 0 ? "Unknown" : Integer.toString(Math.max(0, target - s.getPoints())), Color.WHITE);
            line("Brazier", s.getLocalBrazierState().toString(), Color.WHITE);
            line("Pyromancer", !s.isLocalPyromancerKnown() ? "Unknown" : s.isLocalPyromancerIncapacitated() ? "Incapacitated" : "Healthy", Color.WHITE);
            line("Activity", s.getCurrentActivity().toString(), Color.WHITE);
            line("Firemaking / warm items", s.getFiremakingLevel() + " / " + (s.getWarmClothingCount() < 0 ? "Unknown" : s.getWarmClothingCount()), Color.WHITE);
            if (soloMode) { line("Run energy", percent(s.getRunEnergyPercent()), Color.WHITE); }
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
        if (WintertodtGuidance.isSupplyTarget(o)) { return SUPPLY_FLAME; }
        if (!o.actionable()) { return new Color(130, 200, 150); }
        return FLAME;
    }
    private static String number(int n) { return n < 0 ? "Unknown" : Integer.toString(n); }
    private static String percent(int n) { return n < 0 ? "Unknown" : n + "%"; }
    private void body(String text)
    {
        panelComponent.getChildren().add(new WintertodtInstructionComponent(text, Color.LIGHT_GRAY,
            new Font(Font.SANS_SERIF, Font.PLAIN, 12)));
    }
    private void line(String left, String right, Color color)
    {
        panelComponent.getChildren().add(LineComponent.builder().left(left + ":").leftColor(Color.LIGHT_GRAY)
            .leftFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12)).rightFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12)).right(right).rightColor(color).build());
    }
}
