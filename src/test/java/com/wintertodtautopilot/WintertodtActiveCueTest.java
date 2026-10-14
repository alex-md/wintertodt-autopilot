package com.wintertodtautopilot;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WintertodtActiveCueTest
{
    @Test public void activeChoppingFeedingAndFletchingDrawNoSceneCues()
    {
        Client client = mock(Client.class);
        ModelOutlineRenderer outlines = mock(ModelOutlineRenderer.class);
        WintertodtAutopilotPlugin plugin = mock(WintertodtAutopilotPlugin.class);
        WintertodtAutopilotConfig config = new WintertodtAutopilotConfig() { };
        WintertodtSceneOverlay scene = new WintertodtSceneOverlay(client, plugin, config, outlines);
        WintertodtInventoryOverlay inventory = new WintertodtInventoryOverlay(plugin, config);
        net.runelite.api.widgets.WidgetItem item = mock(net.runelite.api.widgets.WidgetItem.class);
        Graphics2D graphics = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try
        {
            ObjectiveType[] types = {ObjectiveType.CHOP, ObjectiveType.FEED, ObjectiveType.FLETCH};
            PlayerActivity[] activities = {PlayerActivity.CHOPPING, PlayerActivity.FEEDING, PlayerActivity.FLETCHING};
            for (int i = 0; i < types.length; i++)
            {
                when(plugin.getObjective()).thenReturn(new WintertodtObjective(types[i], "Action", "batch", TargetKind.ROOTS, 1, false, net.runelite.api.gameval.ItemID.WINT_BRUMA_ROOT));
                when(plugin.getSnapshot()).thenReturn(WintertodtSnapshot.builder().roundState(RoundState.ACTIVE)
                    .currentActivity(activities[i]).build());
                assertNull(scene.render(graphics));
                inventory.renderItemOverlay(graphics, net.runelite.api.gameval.ItemID.WINT_BRUMA_ROOT, item);
            }
            // No outlines, scene projections, routes or inventory borders while working.
            verifyNoInteractions(client, outlines, item);
            verify(plugin, never()).getPath();
        }
        finally { graphics.dispose(); }
    }

    @Test public void choppingHidesOwnedArrowAndPathUntilInterrupted() throws Exception
    {
        WintertodtAutopilotPlugin plugin = new WintertodtAutopilotPlugin();
        Client client = mock(Client.class);
        Player player = mock(Player.class);
        WintertodtStateTracker tracker = mock(WintertodtStateTracker.class);
        WintertodtObjectTracker objects = mock(WintertodtObjectTracker.class);
        WintertodtHintArrow arrow = mock(WintertodtHintArrow.class);
        WintertodtPathTracker paths = mock(WintertodtPathTracker.class);
        TrackerTestSupport.inject(plugin, "client", client);
        TrackerTestSupport.inject(plugin, "tracker", tracker);
        TrackerTestSupport.inject(plugin, "objects", objects);
        TrackerTestSupport.inject(plugin, "arrow", arrow);
        TrackerTestSupport.inject(plugin, "paths", paths);
        TrackerTestSupport.inject(plugin, "config", new WintertodtAutopilotConfig() {
            @Override public boolean showPath() { return true; }
        });
        TrackerTestSupport.inject(plugin, "bootstrapNeeded", false);
        WorldPoint location = new WorldPoint(1639, 3997, 0);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getWorldLocation()).thenReturn(location);
        WintertodtSnapshot working = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE)
            .wintertodtEnergy(80).warmth(90).points(0).playerLocation(location)
            .hasAxe(true).hasHammer(true).hasKnife(true).hasTinderbox(true)
            .firemakingLevel(99).warmClothingCount(4).rejuvenationDoses(12).freeSlots(20)
            .localBrazierState(BrazierState.LIT).localPyromancerKnown(true)
            .workingCorner(WorkingCorner.SOUTHEAST).currentActivity(PlayerActivity.CHOPPING).build();
        when(tracker.snapshot(any())).thenReturn(working);
        plugin.onGameTick(new GameTick());
        assertEquals(ObjectiveType.CHOP, plugin.getObjective().getType());
        verify(paths).update(null, null, null, false);
        verify(arrow).update(null, null, false);
        when(tracker.snapshot(any())).thenReturn(working.toBuilder()
            .currentActivity(PlayerActivity.IDLE).interrupted(true).playerIdle(true).build());
        plugin.onGameTick(new GameTick());
        assertEquals(ObjectiveType.CHOP, plugin.getObjective().getType());
        verify(paths).update(null, null, null, true);
        verify(arrow).update(null, null, true);
    }
}
