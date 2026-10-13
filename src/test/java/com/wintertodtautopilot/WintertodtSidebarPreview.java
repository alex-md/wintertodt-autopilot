package com.wintertodtautopilot;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.*;
import net.runelite.client.ui.laf.RuneLiteLAF;

/** Headless visual check using RuneLite's actual look and feel. */
public final class WintertodtSidebarPreview
{
    public static void main(String[] args) throws Exception
    {
        UIManager.setLookAndFeel(new RuneLiteLAF());
        SwingUtilities.invokeAndWait(() -> {
            try
            {
                PlannerSettings settings = new PlannerSettings(WintertodtStrategy.MASS_BALANCED, PointGoal.MINIMUM_500, FletchMode.AUTOMATIC, 35, 20);
                WintertodtSnapshot s = WintertodtSnapshot.builder().roundState(RoundState.ENDING).points(350).warmth(78)
                    .wintertodtEnergy(8).estimatedSecondsLeft(36).roundBudgetSeconds(29).brazierDistance(0)
                    .localBrazierState(BrazierState.LIT).localPyromancerKnown(true).hasAxe(true).hasKnife(true).hasHammer(true).hasBrumaTorch(true).warmClothingCount(4)
                    .brumaRoots(8).freeSlots(14).rejuvenationDoses(3).workingCorner(WorkingCorner.SOUTHEAST).build();
                WintertodtObjective o = new WintertodtDecisionEngine().decide(s, settings);
                BufferedImage image = new BufferedImage(780, 1460, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics(); g.setColor(new Color(25, 27, 29)); g.fillRect(0, 0, 780, 1460);
                g.setColor(Color.LIGHT_GRAY); g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                g.drawString("Synthetic sidebar preview · RuneLite look and feel · no live game", 16, 22);
                render(g, 15, new WintertodtPanelState(WintertodtSnapshot.builder().build(), null, settings, WorkingCorner.AUTOMATIC, 0, false, true));
                render(g, 270, new WintertodtPanelState(s, o, settings, WorkingCorner.AUTOMATIC, 4, false, true));
                render(g, 525, new WintertodtPanelState(s, o, settings, WorkingCorner.SOUTHEAST, 4, true, false));
                g.dispose();
                Path output = Path.of("build", "preview", "sidebar.png"); Files.createDirectories(output.getParent());
                ImageIO.write(image, "png", output.toFile());
            }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }
    private static void render(Graphics2D g, int x, WintertodtPanelState state)
    {
        WintertodtPluginPanel panel = new WintertodtPluginPanel(() -> {}, (key, value) -> {});
        panel.update(state);
        panel.setSize(net.runelite.client.ui.PluginPanel.PANEL_WIDTH, 1400);
        for (int i = 0; i < 3; i++) { layout(panel); }
        Graphics2D local = (Graphics2D) g.create(); local.translate(x, 40);
        panel.printAll(local); local.dispose();
    }
    private static void layout(Container parent)
    {
        parent.doLayout();
        for (Component child : parent.getComponents()) { if (child instanceof Container) { layout((Container) child); } }
    }
}
