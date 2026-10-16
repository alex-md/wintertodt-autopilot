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
        BufferedImage image = new BufferedImage(1220, 2000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(34, 38, 41)); graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13)); graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawString("Synthetic overlay preview - constructed snapshots, not a live game", 20, 25);
        WintertodtSnapshot base = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).points(425).warmth(78).rawWarmth(780)
            .estimatedSecondsLeft(145).roundBudgetSeconds(127).brazierDistance(0).firemakingLevel(85).warmClothingCount(4).rejuvenationDoses(3).wintertodtEnergy(64).brumaRoots(17).freeSlots(7).localBrazierState(BrazierState.LIT)
            .localPyromancerKnown(true).workingCorner(WorkingCorner.SOUTHEAST).currentActivity(PlayerActivity.FLETCHING).build();
        WintertodtObjective fletch = new WintertodtObjective(ObjectiveType.FLETCH, "Fletch 17 roots", "batch_fletch", TargetKind.NONE, 17, false, ItemID.KNIFE, ItemID.WINT_BRUMA_ROOT);
        render(graphics, 20, 55, base, fletch, new WintertodtAutopilotConfig() { });
        render(graphics, 320, 55, base.toBuilder().interrupted(true).playerIdle(true).currentActivity(PlayerActivity.IDLE).build(),
            new WintertodtObjective(ObjectiveType.FLETCH, "Continue fletching", "batch_fletch", TargetKind.NONE, 17, false, ItemID.KNIFE, ItemID.WINT_BRUMA_ROOT),
            new WintertodtAutopilotConfig() { });
        render(graphics, 920, 55, base, fletch, new WintertodtAutopilotConfig() { @Override public boolean debug() { return true; } });
        render(graphics, 620, 55, base.toBuilder().warmth(15).build(),
            new WintertodtObjective(ObjectiveType.RESTORE_WARMTH, "Drink rejuvenation potion", "warmth_low", TargetKind.NONE, 1, true),
            new WintertodtAutopilotConfig() { });
        render(graphics, 20, 300, base, fletch,
            new WintertodtAutopilotConfig() { @Override public boolean pauseGuidance() { return true; } });
        render(graphics, 320, 300, base, fletch,
            new WintertodtAutopilotConfig() { @Override public OverlayDetail overlayDetail() { return OverlayDetail.MINIMAL; } });
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13)); graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawString("Optional walking cues (off by default) and inventory highlight samples", 20, 630);
        Color color = WintertodtOverlay.color(fletch);
        net.runelite.api.Point a = new net.runelite.api.Point(60, 710), b = new net.runelite.api.Point(210, 710),
            c = new net.runelite.api.Point(210, 775);
        WintertodtHighlight.routeEdge(graphics, a, b, color, 1);
        WintertodtHighlight.routeEdge(graphics, b, c, color, 1);
        Polygon tile = new Polygon(new int[]{160, 210, 260, 210}, new int[]{775, 750, 775, 800}, 4);
        WintertodtHighlight.tile(graphics, tile, color, 1, true);
        WintertodtSnapshot feeding = base.toBuilder().currentActivity(PlayerActivity.FEEDING).build();
        WintertodtObjective feed = new WintertodtObjective(ObjectiveType.FEED, "Feed", "batch_feed",
            TargetKind.BRAZIER, 17, false, ItemID.WINT_BRUMA_ROOT);
        graphics.drawString("Already feeding: no highlight", 350, 665);
        if (WintertodtHighlight.inventoryItem(feeding, feed, ItemID.WINT_BRUMA_ROOT))
        { WintertodtHighlight.inventory(graphics, new Rectangle(350, 685, 45, 40), color, 1); }
        graphics.setColor(Color.WHITE); graphics.drawString("Roots", 355, 710);
        graphics.drawString("Brazier out: fuel pulses (dim / bright)", 560, 665);
        WintertodtSnapshot stopped = feeding.toBuilder().localBrazierState(BrazierState.UNLIT).build();
        WintertodtObjective light = new WintertodtObjective(ObjectiveType.LIGHT, "Relight", "brazier_unlit",
            TargetKind.BRAZIER, 0, false, ItemID.TINDERBOX);
        if (WintertodtHighlight.inventoryItem(stopped, light, ItemID.WINT_BRUMA_ROOT))
        {
            WintertodtHighlight.inventory(graphics, new Rectangle(565, 685, 45, 40), color,
                WintertodtHighlight.strengthAt(stopped, light, true, 900_000_000L));
            WintertodtHighlight.inventory(graphics, new Rectangle(640, 685, 45, 40), color,
                WintertodtHighlight.strengthAt(stopped, light, true, 300_000_000L));
        }
        graphics.setColor(Color.WHITE); graphics.drawString("Roots", 569, 710); graphics.drawString("Roots", 644, 710);
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawString("Supply instructions appear in the main panel only - no floating scene labels", 20, 855);
        TargetKind[] supplies = {TargetKind.HAMMER_CRATE, TargetKind.POTION_CRATE, TargetKind.HERBS, TargetKind.BREWMA};
        for (int i = 0; i < supplies.length; i++)
        {
            int count = i == 0 ? 1 : 2;
            WintertodtObjective supply = new WintertodtObjective(i == 0 ? ObjectiveType.GET_TOOL : ObjectiveType.MAKE_REJUVENATION,
                WintertodtGuidance.supplyInstruction(supplies[i], count), "prepare_supply", supplies[i], count, false);
            render(graphics, 20 + i * 300, 880, base.toBuilder().roundState(RoundState.COUNTDOWN)
                .currentActivity(PlayerActivity.IDLE).points(0).brumaRoots(0).secondsToStart(45).build(), supply,
                new WintertodtAutopilotConfig() { });
        }
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawString("Solo: sustain / deliberate recovery / finish request / missing observations", 20, 1150);
        String[] instructions = {"Fletch 17 roots", "Let energy recover to 12% - do not relight",
            "Light the Northeast brazier to finish", "Check all brazier indicators before relighting"};
        String[] reasons = {"solo_batch_fletch", "solo_recovery_wait", "solo_light", "solo_coverage_unknown"};
        for (int i = 0; i < 4; i++)
        {
            final int index = i;
            render(graphics, 20 + i * 300, 1170, base.toBuilder().wintertodtEnergy(i == 0 ? 26 : 9)
                .currentActivity(PlayerActivity.IDLE).build(), new WintertodtObjective(i == 0 ? ObjectiveType.FLETCH : i == 2 ? ObjectiveType.LIGHT : ObjectiveType.WAIT,
                    instructions[i], reasons[i], i == 2 ? TargetKind.BRAZIER : TargetKind.NONE, 0, false),
                new WintertodtAutopilotConfig() {
                    @Override public WintertodtMode mode() { return WintertodtMode.SOLO; }
                    @Override public boolean finishSoloRound() { return index == 2; }
                });
        }
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawString("Reward secured stays visible as the target advances (Normal / Minimal / Solo)", 20, 1560);
        render(graphics, 20, 1580, base.toBuilder().points(500).build(), fletch,
            new WintertodtAutopilotConfig() { });
        render(graphics, 320, 1580, base.toBuilder().points(1000).build(), fletch,
            new WintertodtAutopilotConfig() { @Override public OverlayDetail overlayDetail() { return OverlayDetail.MINIMAL; } });
        render(graphics, 620, 1580, base.toBuilder().points(500).build(), fletch,
            new WintertodtAutopilotConfig() { @Override public WintertodtMode mode() { return WintertodtMode.SOLO; } });
        graphics.dispose();
        Path path = Path.of("build", "preview", "overlays.png"); Files.createDirectories(path.getParent());
        ImageIO.write(image, "png", path.toFile());
    }
    private static void render(Graphics2D graphics, int x, int y, WintertodtSnapshot snapshot,
        WintertodtObjective objective, WintertodtAutopilotConfig config)
    {
        WintertodtAutopilotPlugin plugin = mock(WintertodtAutopilotPlugin.class);
        when(plugin.getPotionTargetDoses()).thenReturn(config.mode() == WintertodtMode.SOLO ? config.finishSoloRound() ? 8 : 24 : 4);
        when(plugin.getSoloPhase()).thenReturn(config.finishSoloRound() ? SoloPhase.FINISH
            : objective.getReason().contains("recovery") ? SoloPhase.RECOVER_ENERGY
            : objective.getReason().contains("unknown") ? SoloPhase.REASSESS : SoloPhase.SUSTAIN);
        when(plugin.getSoloEnergyTrend()).thenReturn("STABLE");
        when(plugin.isSoloFinishRequested()).thenReturn(config.finishSoloRound());
        when(plugin.isPaused()).thenReturn(config.pauseGuidance());
        when(plugin.getSnapshot()).thenReturn(snapshot); when(plugin.getObjective()).thenReturn(objective);
        WintertodtOverlay overlay = new WintertodtOverlay(plugin, config);
        Graphics2D local = (Graphics2D) graphics.create(); local.translate(x, y); local.setFont(FontManager.getRunescapeFont());
        // RuneLite's panel caches child dimensions for its next frame.
        overlay.render(local); overlay.render(local); overlay.render(local); local.dispose();
    }
}
