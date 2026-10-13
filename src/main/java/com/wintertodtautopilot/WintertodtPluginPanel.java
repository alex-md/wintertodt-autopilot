package com.wintertodtautopilot;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

/** RuneLite sidebar. All components are created and updated on the EDT. */
public final class WintertodtPluginPanel extends PluginPanel
{
    private static final Color ACCENT = new Color(178, 196, 214);
    private static final Color READY = new Color(134, 211, 158);
    private static final Color CHECK = new Color(245, 192, 112);
    private final JLabel phase = new JLabel();
    private final JTextArea instruction = text(16, true);
    private final JTextArea explanation = text(12, false);
    private final JTextArea forecast = text(12, false);
    private final JLabel warmth = new JLabel();
    private final JProgressBar reward = new JProgressBar();
    private final JTextArea projection = text(12, false);
    private final JPanel requirements = new JPanel();
    private final JComboBox<WintertodtStrategy> strategy = new JComboBox<>(WintertodtStrategy.values());
    private final JComboBox<PointGoal> goal = new JComboBox<>(PointGoal.values());
    private final JComboBox<WorkingCorner> corner = new JComboBox<>(WorkingCorner.values());
    private final JComboBox<FletchMode> fletch = new JComboBox<>(FletchMode.values());
    private final JTextArea strategyHelp = text(12, false);
    private final JCheckBox path = new JCheckBox("Show walking path");
    private final JButton pause = new JButton("Pause guidance");
    private final BiConsumer<String, Object> save;
    private boolean updating;
    private WintertodtPanelState lastState;

    public WintertodtPluginPanel(Runnable togglePause, BiConsumer<String, Object> save)
    {
        this.save = save;
        setLayout(new BorderLayout());
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(12, 10, 12, 10));
        body.setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(body, BorderLayout.NORTH);
        JLabel title = new JLabel("Wintertodt Autopilot");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        title.setForeground(ACCENT);
        add(body, title); gap(body, 5);
        phase.setForeground(Color.LIGHT_GRAY); add(body, phase); gap(body, 14);
        add(body, instruction); gap(body, 7); add(body, explanation); gap(body, 10);
        pause.addActionListener(e -> togglePause.run());
        pause.setToolTipText("Pause all guidance highlights, arrows and notifications. Observations continue.");
        add(body, pause); gap(body, 18);
        heading(body, "THIS ROUND"); add(body, warmth); gap(body, 8);
        add(body, forecast); gap(body, 8);
        reward.setStringPainted(true); reward.setForeground(ACCENT);
        reward.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        add(body, reward); gap(body, 7); add(body, projection); gap(body, 18);
        heading(body, "SUPPLY CHECK");
        requirements.setLayout(new BoxLayout(requirements, BoxLayout.Y_AXIS));
        requirements.setOpaque(false); add(body, requirements); gap(body, 6);
        JTextArea reserveHelp = text(11, false);
        reserveHelp.setText("Potion reserve is an estimate. A partly used reserve is normal during a round.");
        add(body, reserveHelp); gap(body, 18);
        heading(body, "YOUR PLAN");
        choice(body, "Strategy", strategy, "strategy");
        add(body, strategyHelp); gap(body, 10);
        choice(body, "Reward goal", goal, "pointGoal");
        choice(body, "Working brazier", corner, "workingCorner");
        corner.setToolTipText("Automatic keeps your established corner until you leave Wintertodt.");
        choice(body, "Fletch roots", fletch, "fletchMode");
        path.setOpaque(false);
        path.addActionListener(e -> { if (!updating) { save.accept("showPath", path.isSelected()); } });
        add(body, path); gap(body, 18);
        heading(body, "GETTING STARTED");
        JTextArea help = text(12, false);
        help.setText("Join a mass world and enter the prison. Follow one highlighted action at a time. A teal border marks the objective; pulsing borders call for attention.\n\nYou perform every click. Hold Alt to move the in-game panel. Right-click it to open all plugin settings.");
        add(body, help);
        readableFonts(body);
    }

    private static void readableFonts(Container parent)
    {
        for (Component child : parent.getComponents())
        {
            if (!(child instanceof JTextArea))
            {
                Font old = child.getFont();
                if (old != null) { child.setFont(new Font(Font.SANS_SERIF, old.getStyle(),
                    old.getFamily().equals(Font.SANS_SERIF) ? old.getSize() : Math.min(12, old.getSize()))); }
            }
            if (child instanceof Container) { readableFonts((Container) child); }
        }
    }

    public void update(WintertodtPanelState state)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> update(state));
            return;
        }
        if (state.equals(lastState)) { return; }
        lastState = state;
        updating = true;
        try
        {
            WintertodtSnapshot s = state.getSnapshot();
            WintertodtObjective o = state.getObjective();
            boolean observed = s.getRoundState() != RoundState.OUTSIDE;
            phase.setText(state.isPaused() ? "Guidance paused" : WintertodtGuidance.phase(s));
            instruction.setText(state.isPaused() ? "Take a break"
                : o == null ? "Start at Wintertodt"
                : (WintertodtHighlight.needsRestart(s, o) ? "Resume: " : "") + o.getInstruction());
            instruction.setForeground(o != null && o.isUrgent() && !state.isPaused() ? WintertodtOverlay.color(o) : Color.WHITE);
            explanation.setText(state.isPaused()
                ? "Highlights and notifications are paused, including low warmth alerts. Resume when you want guidance again."
                : WintertodtGuidance.explanation(o));
            pause.setText(state.isPaused() ? "Resume guidance" : "Pause guidance");
            warmth.setText(!observed ? "Live stats appear at Wintertodt" : "Warmth " + percent(s.getWarmth())
                + "  ·  Energy " + percent(s.getWintertodtEnergy()));
            warmth.setToolTipText("Restore at " + state.getSettings().getRestoreWarmth() + "% warmth. Rejuvenation potions take priority over food.");
            warmth.setForeground(s.active() && s.getWarmth() >= 0 && s.getWarmth() <= state.getSettings().getRestoreWarmth()
                ? CHECK : Color.LIGHT_GRAY);
            forecast.setText(!s.active() ? "Finish estimate appears during a round."
                : s.getEstimatedSecondsLeft() < 0 ? "Learning round pace. Energy warnings apply until a recent drain rate is available."
                : "Estimated finish ~" + s.getEstimatedSecondsLeft() + "s · planning with " + s.getRoundBudgetSeconds()
                    + "s after a safety margin. This changes with the round pace.");
            int target = WintertodtGuidance.rewardTarget(state.getSettings().getPointGoal(), s.getPoints());
            reward.setMaximum(target); reward.setValue(Math.max(0, s.getPoints()));
            reward.setString(!observed || s.getPoints() < 0 ? "Points unknown" : s.getPoints() + " / " + target + " points");
            projection.setText(!observed ? "Earn at least 500 points for a rewarded round."
                : (s.getPoints() < 0 ? "" : s.rewardThresholdReached() ? "500-point minimum secured.\n" : "500-point minimum not yet secured.\n")
                + (s.getPoints() < 0 ? "Current points unavailable.\n" : "")
                + "+" + s.inventoryPoints() + " carried points; +" + s.potentialPointsIfFletched()
                + " if all fuel is kindling. Feed before the round ends to earn them.");
            requirements.removeAll();
            for (WintertodtPanelState.Requirement row : state.requirements())
            {
                String status = row.getStatus() == WintertodtPanelState.Status.READY ? "Ready"
                    : row.getStatus() == WintertodtPanelState.Status.CHECK ? "Check"
                    : row.getDetail().startsWith("Optional") ? "Optional" : "Unknown";
                String detail = row.getName().equals("Potion reserve") || row.getName().equals("Warm clothing")
                    ? row.getDetail().equals("Checked at Wintertodt") ? "Unknown" : row.getDetail() : status;
                JLabel label = new JLabel(row.getName() + " · " + detail);
                label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                label.setToolTipText(row.getDetail());
                label.setForeground(row.getStatus() == WintertodtPanelState.Status.READY ? READY
                    : row.getStatus() == WintertodtPanelState.Status.CHECK ? CHECK : Color.LIGHT_GRAY);
                add(requirements, label); gap(requirements, 7);
            }
            strategy.setSelectedItem(state.getSettings().getStrategy());
            goal.setSelectedItem(state.getSettings().getPointGoal());
            corner.setSelectedItem(state.getSelectedCorner());
            fletch.setSelectedItem(state.getSettings().getFletchMode());
            path.setSelected(state.isShowPath());
            strategyHelp.setText(WintertodtGuidance.strategy(state.getSettings().getStrategy()));
        }
        finally { updating = false; }
        revalidate(); repaint();
    }

    private void choice(JPanel body, String label, JComboBox<?> box, String key)
    {
        JLabel caption = new JLabel(label); caption.setForeground(Color.LIGHT_GRAY);
        add(body, caption); gap(body, 4);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        box.addActionListener(e -> { if (!updating) { save.accept(key, box.getSelectedItem()); } });
        add(body, box); gap(body, 10);
    }
    private static String percent(int n) { return n < 0 ? "?" : n + "%"; }
    private static JTextArea text(int size, boolean bold)
    {
        JTextArea area = new WrappingText(); area.setEditable(false); area.setFocusable(false);
        area.setLineWrap(true); area.setWrapStyleWord(true); area.setOpaque(false);
        area.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size));
        area.setForeground(Color.LIGHT_GRAY);
        return area;
    }
    /** BoxLayout needs the wrapped height before it assigns component bounds. */
    private static final class WrappingText extends JTextArea
    {
        @Override public Dimension getPreferredSize()
        {
            Container parent = getParent();
            Insets insets = parent == null ? new Insets(0, 0, 0, 0) : parent.getInsets();
            int width = parent != null && parent.getWidth() > 0
                ? parent.getWidth() - insets.left - insets.right : PANEL_WIDTH - BORDER_OFFSET * 2 - 20;
            setSize(Math.max(1, width), Short.MAX_VALUE);
            Dimension size = super.getPreferredSize();
            return new Dimension(width, size.height);
        }
        @Override public Dimension getMaximumSize()
        {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }
    private static void heading(JPanel body, String title)
    {
        JLabel label = new JLabel(title); label.setForeground(Color.GRAY);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f)); add(body, label); gap(body, 8);
    }
    private static void add(JPanel body, JComponent child)
    {
        child.setAlignmentX(Component.LEFT_ALIGNMENT); body.add(child);
    }
    private static void gap(JPanel body, int height) { body.add(Box.createVerticalStrut(height)); }

    /** Packaged resource so the sidebar and Hub share the same brazier artwork. */
    public static BufferedImage icon()
    {
        return ImageUtil.loadImageResource(WintertodtPluginPanel.class, "icon.png");
    }
}
