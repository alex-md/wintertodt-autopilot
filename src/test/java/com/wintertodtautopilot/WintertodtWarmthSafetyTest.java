package com.wintertodtautopilot;

import net.runelite.api.*;
import net.runelite.api.events.ClientTick;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WintertodtWarmthSafetyTest
{
    @Test public void fullWarmthNeverRequestsRestorationAtAnyConfiguredThreshold()
    {
        for (int threshold = 1; threshold <= 100; threshold++)
        {
            PlannerSettings settings = new PlannerSettings(WintertodtStrategy.MASS_POINTS,
                PointGoal.MINIMUM_500, FletchMode.AUTOMATIC, threshold, threshold);
            WintertodtSnapshot full = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).roundState(RoundState.ACTIVE)
                .warmth(100).rawWarmth(1000).warmthItem(ItemID.WINT_POTION4).build();
            assertNull(WintertodtSafety.objective(full, settings));
            assertNull(WintertodtSafety.objective(full.toBuilder().warmthItem(-1).build(), settings));
        }
    }

    @Test public void fullWarmthDoesNotSendALowWarmthAlertAtMaximumThreshold()
    {
        WintertodtAutopilotConfig config = new WintertodtAutopilotConfig() {
            @Override public int restoreWarmth() { return 100; }
            @Override public int emergencyWarmth() { return 100; }
        };
        WintertodtSnapshot full = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).roundState(RoundState.ACTIVE).warmth(100).build();
        WintertodtObjective chop = new WintertodtObjective(ObjectiveType.CHOP, "Chop", "batch_chop", TargetKind.ROOTS, 6, false);
        assertNull(new WintertodtAttentionTracker().notification(full, chop, config, 10));
    }

    @Test public void recoveredWarmthImmediatelyReplacesAStaleDrinkInstructionInBothModes() throws Exception
    {
        for (WintertodtMode mode : WintertodtMode.values())
        {
            WintertodtAutopilotPlugin plugin = new WintertodtAutopilotPlugin();
            Client client = mock(Client.class); Player player = mock(Player.class);
            TrackerTestSupport.inject(plugin, "client", client);
            TrackerTestSupport.inject(plugin, "config", new WintertodtAutopilotConfig() {
                @Override public WintertodtMode mode() { return mode; }
            });
            TrackerTestSupport.inject(plugin, "arrow", mock(WintertodtHintArrow.class));
            TrackerTestSupport.inject(plugin, "paths", mock(WintertodtPathTracker.class));
            TrackerTestSupport.inject(plugin, "objects", mock(WintertodtObjectTracker.class));
            TrackerTestSupport.inject(plugin, "npcs", mock(WintertodtNpcTracker.class));
            net.runelite.client.Notifier notifier = mock(net.runelite.client.Notifier.class);
            TrackerTestSupport.inject(plugin, "notifier", notifier);
            when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
            when(client.getLocalPlayer()).thenReturn(player);
            when(player.getWorldLocation()).thenReturn(WorkingCorner.SOUTHWEST.location());
            WintertodtSnapshot cold = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).roundState(RoundState.ACTIVE)
                .observationTick(10).wintertodtEnergy(28).points(0).warmth(20).rawWarmth(200)
                .warmthItem(ItemID.WINT_POTION4).potionItem(ItemID.WINT_POTION4).rejuvenationDoses(24)
                .hasAxe(true).hasHammer(true).hasKnife(true).hasTinderbox(true).freeSlots(20)
                .workingCorner(WorkingCorner.SOUTHWEST).localBrazierState(BrazierState.LIT).build();
            WintertodtObjective drink = WintertodtSafety.objective(cold, PlannerSettings.from(new WintertodtAutopilotConfig() { }));
            TrackerTestSupport.inject(plugin, "snapshot", cold);
            TrackerTestSupport.inject(plugin, "objective", drink);
            when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(400);
            plugin.onClientTick(new ClientTick());
            assertSame(drink, plugin.getObjective()); // Still below the restore threshold.
            when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(10000);
            plugin.onClientTick(new ClientTick());
            assertSame(drink, plugin.getObjective()); // Invalid data cannot invent recovery.
            when(client.getVarbitValue(VarbitID.WINT_WARMTH)).thenReturn(1000);
            java.lang.reflect.Field soloField = WintertodtAutopilotPlugin.class.getDeclaredField("solo");
            soloField.setAccessible(true);
            WintertodtSoloCapture capture = new WintertodtSoloCapture(net.runelite.http.api.RuneLiteAPI.GSON);
            com.google.gson.JsonElement before = capture.checkpoint((WintertodtSoloEngine) soloField.get(plugin));
            plugin.onClientTick(new ClientTick());
            assertEquals(100, plugin.getSnapshot().getWarmth());
            assertNotEquals(ObjectiveType.RESTORE_WARMTH, plugin.getObjective().getType());
            assertFalse(WintertodtHighlight.inventoryItem(plugin.getSnapshot(), plugin.getObjective(), ItemID.WINT_POTION4));
            assertEquals(10, plugin.getSnapshot().getObservationTick()); // No extra game tick was manufactured.
            assertEquals(before, capture.checkpoint((WintertodtSoloEngine) soloField.get(plugin)));
            WintertodtObjective resumed = plugin.getObjective();
            plugin.onClientTick(new ClientTick());
            assertSame(resumed, plugin.getObjective());
            verifyNoInteractions(notifier); // Alerts remain tied to the regular game tick.
        }
    }
}
