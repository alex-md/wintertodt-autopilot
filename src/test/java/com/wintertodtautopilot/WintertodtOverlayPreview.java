package com.wintertodtautopilot;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.ui.FontManager;
import static org.mockito.Mockito.*;

/** Synthetic rendering harness; no client/game interaction. */
public final class WintertodtOverlayPreview
{
    public static void main(String[] args) throws Exception
    {
        BufferedImage image = new BufferedImage(920, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(34, 38, 41)); graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13)); graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawString("Synthetic overlay preview - constructed snapshots, not a live game", 20, 25);
        WintertodtSnapshot base = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).points(425).warmth(78)
            .firemakingLevel(85).warmClothingCount(4).rejuvenationDoses(3).wintertodtEnergy(64).brumaRoots(17).freeSlots(7).localBrazierState(BrazierState.LIT)
            .localPyromancerKnown(true).workingCorner(WorkingCorner.SOUTHEAST).currentActivity(PlayerActivity.FLETCHING).build();
        WintertodtObjective fletch = new WintertodtObjective(ObjectiveType.FLETCH, "Fletch 17 roots", "batch_fletch", TargetKind.NONE, 17, false, ItemID.KNIFE, ItemID.WINT_BRUMA_ROOT);
        render(graphics, 20, 55, base, fletch, new WintertodtAutopilotConfig() { });
        render(graphics, 320, 55, base.toBuilder().interrupted(true).playerIdle(true).currentActivity(PlayerActivity.IDLE).build(),
            new WintertodtObjective(ObjectiveType.FLETCH, "Continue fletching", "batch_fletch", TargetKind.NONE, 17, false, ItemID.KNIFE, ItemID.WINT_BRUMA_ROOT),
            new WintertodtAutopilotConfig() { });
        render(graphics, 620, 55, base, fletch, new WintertodtAutopilotConfig() { @Override public boolean debug() { return true; } });
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13)); graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawString("Synthetic walking path, destination tile and inventory highlight samples", 20, 470);
        Color color = WintertodtOverlay.color(fletch);
        net.runelite.api.Point a = new net.runelite.api.Point(60, 550), b = new net.runelite.api.Point(210, 550),
            c = new net.runelite.api.Point(210, 615);
        WintertodtHighlight.routeEdge(graphics, a, b, color, 1);
        WintertodtHighlight.routeEdge(graphics, b, c, color, 1);
        Polygon tile = new Polygon(new int[]{160, 210, 260, 210}, new int[]{615, 590, 615, 640}, 4);
        WintertodtHighlight.tile(graphics, tile, color, 1, true);
        graphics.setColor(Color.WHITE); graphics.drawString("STAND HERE", 165, 580);
        graphics.drawString("Inventory: normal", 350, 505);
        WintertodtHighlight.inventory(graphics, new Rectangle(350, 525, 45, 40), color, 1);
        graphics.setColor(Color.WHITE); graphics.drawString("Knife", 355, 550);
        graphics.drawString("Restart pulse: dim / bright", 560, 505);
        WintertodtHighlight.inventory(graphics, new Rectangle(565, 525, 45, 40), color, .7f);
        WintertodtHighlight.inventory(graphics, new Rectangle(640, 525, 45, 40), color, 1);
        graphics.setColor(Color.WHITE); graphics.drawString("Roots", 569, 550); graphics.drawString("Roots", 644, 550);
        graphics.dispose();
        Path path = Path.of("build", "preview", "overlays.png"); Files.createDirectories(path.getParent());
        ImageIO.write(image, "png", path.toFile());
    }
    private static void render(Graphics2D graphics, int x, int y, WintertodtSnapshot snapshot,
        WintertodtObjective objective, WintertodtAutopilotConfig config)
    {
        WintertodtAutopilotPlugin plugin = mock(WintertodtAutopilotPlugin.class);
        when(plugin.getPotionTargetDoses()).thenReturn(4);
        when(plugin.getSnapshot()).thenReturn(snapshot); when(plugin.getObjective()).thenReturn(objective);
        WintertodtOverlay overlay = new WintertodtOverlay(plugin, config);
        Graphics2D local = (Graphics2D) graphics.create(); local.translate(x, y); local.setFont(FontManager.getRunescapeFont());
        overlay.render(local); local.dispose();
    }
}
