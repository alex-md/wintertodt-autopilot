package com.wintertodtautopilot;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WintertodtSoloIntegrationTest
{
    @Test public void loadingAndConnectionLossPreserveActualRecoveryController() throws Exception
    {
        WintertodtAutopilotPlugin plugin = new WintertodtAutopilotPlugin();
        TrackerTestSupport.inject(plugin, "config", new WintertodtAutopilotConfig() { });
        TrackerTestSupport.inject(plugin, "tracker", mock(WintertodtStateTracker.class));
        TrackerTestSupport.inject(plugin, "objects", mock(WintertodtObjectTracker.class));
        TrackerTestSupport.inject(plugin, "npcs", mock(WintertodtNpcTracker.class));
        TrackerTestSupport.inject(plugin, "arrow", mock(WintertodtHintArrow.class));
        TrackerTestSupport.inject(plugin, "paths", mock(WintertodtPathTracker.class));
        java.lang.reflect.Field field = WintertodtAutopilotPlugin.class.getDeclaredField("solo");
        field.setAccessible(true);
        WintertodtSoloEngine engine = (WintertodtSoloEngine) field.get(plugin);
        engine.decide(WintertodtSoloReliabilityTest.observed(1, 9, 100, 0, BrazierState.UNLIT), WintertodtSoloReliabilityTest.SETTINGS);
        for (GameState state : new GameState[]{GameState.LOADING, GameState.CONNECTION_LOST})
        {
            GameStateChanged event = mock(GameStateChanged.class); when(event.getGameState()).thenReturn(state);
            plugin.onGameStateChanged(event);
            assertTrue(engine.recovering()); assertTrue(engine.isAwaitingObservations());
            assertEquals(6, engine.batchTarget());
        }
        assertEquals(ObjectiveType.CHOP, engine.decide(WintertodtSoloReliabilityTest.observed(10, 11, 100, 2, BrazierState.UNLIT),
            WintertodtSoloReliabilityTest.SETTINGS).getType());
        assertEquals(SoloPhase.RECOVER_ENERGY, engine.getPhase());
    }

    @Test public void diagnosticFailureDoesNotPreventGuidanceOrRepeatEveryTick() throws Exception
    {
        WintertodtAutopilotPlugin plugin = new WintertodtAutopilotPlugin();
        Client client = mock(Client.class); Player player = mock(Player.class);
        WintertodtStateTracker tracker = mock(WintertodtStateTracker.class);
        TrackerTestSupport.inject(plugin, "client", client); TrackerTestSupport.inject(plugin, "tracker", tracker);
        TrackerTestSupport.inject(plugin, "configManager", mock(ConfigManager.class));
        TrackerTestSupport.inject(plugin, "arrow", mock(WintertodtHintArrow.class));
        TrackerTestSupport.inject(plugin, "paths", mock(WintertodtPathTracker.class));
        TrackerTestSupport.inject(plugin, "bootstrapNeeded", false);
        TrackerTestSupport.inject(plugin, "config", new WintertodtAutopilotConfig() {
            @Override public WintertodtMode mode() { return WintertodtMode.SOLO; }
            @Override public boolean pauseGuidance() { return true; }
            @Override public boolean debug() { return true; }
        });
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN); when(client.getLocalPlayer()).thenReturn(player);
        when(player.getWorldLocation()).thenReturn(WorkingCorner.SOUTHWEST.location());
        when(client.getWidget(anyInt())).thenThrow(new IllegalStateException("Synthetic capture failure"));
        when(tracker.snapshot(any())).thenReturn(WintertodtSoloReliabilityTest.observed(1, 28, 100, 0, BrazierState.LIT));
        plugin.onGameTick(new GameTick());
        assertEquals(ObjectiveType.CHOP, plugin.getObjective().getType());
        when(tracker.snapshot(any())).thenReturn(WintertodtSoloReliabilityTest.observed(2, 26, 100, 1, BrazierState.LIT));
        plugin.onGameTick(new GameTick());
        assertEquals(ObjectiveType.CHOP, plugin.getObjective().getType());
        verify(client, times(1)).getWidget(anyInt());
    }
    @Test public void modeSelectionPreservesAutomaticCornerTracking() throws Exception
    {
        WintertodtAutopilotPlugin plugin = new WintertodtAutopilotPlugin();
        Client client = mock(Client.class); Player player = mock(Player.class);
        WintertodtStateTracker tracker = mock(WintertodtStateTracker.class);
        TrackerTestSupport.inject(plugin, "client", client);
        TrackerTestSupport.inject(plugin, "configManager", mock(ConfigManager.class));
        TrackerTestSupport.inject(plugin, "tracker", tracker);
        TrackerTestSupport.inject(plugin, "arrow", mock(WintertodtHintArrow.class));
        TrackerTestSupport.inject(plugin, "paths", mock(WintertodtPathTracker.class));
        TrackerTestSupport.inject(plugin, "bootstrapNeeded", false);
        TrackerTestSupport.inject(plugin, "config", new WintertodtAutopilotConfig() {
            @Override public WintertodtMode mode() { return WintertodtMode.SOLO; }
            @Override public boolean pauseGuidance() { return true; }
        });
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN); when(client.getLocalPlayer()).thenReturn(player);
        when(player.getWorldLocation()).thenReturn(WorkingCorner.SOUTHEAST.location());
        when(tracker.snapshot(WorkingCorner.AUTOMATIC)).thenReturn(WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true)
            .roundState(RoundState.ACTIVE).observationTick(1).wintertodtEnergy(100).warmth(80).points(0)
            .workingCorner(WorkingCorner.SOUTHEAST).freeSlots(20).build());
        plugin.onGameTick(new GameTick());
        verify(tracker).snapshot(WorkingCorner.AUTOMATIC);
        assertEquals(WorkingCorner.SOUTHEAST, plugin.getSnapshot().getWorkingCorner());
        assertEquals(ObjectiveType.GET_TOOL, plugin.getObjective().getType());
        assertEquals(SoloPhase.PREPARE, plugin.getSoloPhase());
        assertNull(plugin.getTargetPoint()); // Pause keeps observations and planning, with no cues.
        PlannerSettings settings = PlannerSettings.from(new WintertodtAutopilotConfig() {
            @Override public WintertodtMode mode() { return WintertodtMode.SOLO; }
            @Override public SoloStrategy soloStrategy() { return SoloStrategy.FIREMAKING_XP; }
        });
        assertEquals(WintertodtStrategy.MASS_XP, settings.getStrategy());
        assertFalse(settings.fletch(WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).hasKnife(true).build()));
    }

    @Test public void remoteObjectNpcAndFallbackTileUseObjectiveDestination() throws Exception
    {
        WintertodtAutopilotPlugin plugin = new WintertodtAutopilotPlugin();
        WintertodtObjectTracker objects = mock(WintertodtObjectTracker.class);
        WintertodtNpcTracker npcs = mock(WintertodtNpcTracker.class);
        TrackerTestSupport.inject(plugin, "objects", objects); TrackerTestSupport.inject(plugin, "npcs", npcs);
        WintertodtSnapshot s = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).workingCorner(WorkingCorner.SOUTHWEST)
            .playerLocation(WorkingCorner.SOUTHWEST.location()).build();
        TrackerTestSupport.inject(plugin, "snapshot", s);
        java.lang.reflect.Method resolve = WintertodtAutopilotPlugin.class.getDeclaredMethod("resolveTarget");
        resolve.setAccessible(true);
        for (TargetKind target : new TargetKind[]{TargetKind.BRAZIER, TargetKind.ROOTS, TargetKind.PYROMANCER})
        {
            TrackerTestSupport.inject(plugin, "objective", new WintertodtObjective(ObjectiveType.POSITION,
                "Remote target", "solo_observe_corner", target, WorkingCorner.NORTHEAST, 0, false));
            resolve.invoke(plugin);
            if (target != TargetKind.ROOTS) { assertEquals(WorkingCorner.NORTHEAST.location(), plugin.getTargetPoint()); }
        }
        verify(objects).target(TargetKind.BRAZIER, s.getPlayerLocation(), WorkingCorner.NORTHEAST);
        verify(objects).target(TargetKind.ROOTS, s.getPlayerLocation(), WorkingCorner.NORTHEAST);
        verify(npcs).target(TargetKind.PYROMANCER, WorkingCorner.NORTHEAST.location());
        NPC remote = mock(NPC.class); WorldPoint location = new WorldPoint(1638, 4016, 0);
        when(remote.getWorldLocation()).thenReturn(location);
        when(npcs.target(TargetKind.PYROMANCER, WorkingCorner.NORTHEAST.location())).thenReturn(remote);
        resolve.invoke(plugin); assertSame(remote, plugin.getTargetNpc()); assertEquals(location, plugin.getTargetPoint());
    }

    @Test public void verifiedCompletionClearsSavedFinishControl() throws Exception
    {
        WintertodtAutopilotPlugin plugin = new WintertodtAutopilotPlugin();
        Client client = mock(Client.class); Player player = mock(Player.class);
        WintertodtStateTracker tracker = mock(WintertodtStateTracker.class); ConfigManager manager = mock(ConfigManager.class);
        TrackerTestSupport.inject(plugin, "client", client); TrackerTestSupport.inject(plugin, "tracker", tracker);
        TrackerTestSupport.inject(plugin, "configManager", manager);
        TrackerTestSupport.inject(plugin, "arrow", mock(WintertodtHintArrow.class));
        TrackerTestSupport.inject(plugin, "paths", mock(WintertodtPathTracker.class));
        TrackerTestSupport.inject(plugin, "bootstrapNeeded", false);
        TrackerTestSupport.inject(plugin, "config", new WintertodtAutopilotConfig() {
            @Override public WintertodtMode mode() { return WintertodtMode.SOLO; }
            @Override public boolean pauseGuidance() { return true; }
            @Override public boolean finishSoloRound() { return true; }
        });
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN); when(client.getLocalPlayer()).thenReturn(player);
        when(player.getWorldLocation()).thenReturn(WorkingCorner.SOUTHWEST.location());
        WintertodtSnapshot active = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).roundState(RoundState.ACTIVE).observationTick(1)
            .wintertodtEnergy(30).warmth(80).points(0).workingCorner(WorkingCorner.SOUTHWEST).freeSlots(20).build();
        when(tracker.snapshot(any())).thenReturn(active);
        plugin.onGameTick(new GameTick()); assertTrue(plugin.isSoloFinishRequested());
        when(tracker.snapshot(any())).thenReturn(active.toBuilder().roundState(RoundState.COUNTDOWN)
            .wintertodtEnergy(-1).secondsToStart(0).build());
        plugin.onGameTick(new GameTick()); assertTrue(plugin.isSoloFinishRequested());
        verify(manager, never()).setConfiguration("wintertodtautopilot", "finishSoloRound", false);
        when(tracker.snapshot(any())).thenReturn(active.toBuilder().roundState(RoundState.COMPLETE).wintertodtEnergy(0).build());
        plugin.onGameTick(new GameTick()); assertFalse(plugin.isSoloFinishRequested());
        verify(manager).setConfiguration("wintertodtautopilot", "finishSoloRound", false);
    }
    @Test public void sceneReloadKeepsLatchedIntentAndHopClearsIt() throws Exception
    {
        WintertodtAutopilotPlugin plugin = new WintertodtAutopilotPlugin();
        ConfigManager manager = mock(ConfigManager.class);
        TrackerTestSupport.inject(plugin, "configManager", manager);
        TrackerTestSupport.inject(plugin, "config", new WintertodtAutopilotConfig() {
            @Override public boolean finishSoloRound() { return true; }
        });
        TrackerTestSupport.inject(plugin, "tracker", mock(WintertodtStateTracker.class));
        TrackerTestSupport.inject(plugin, "objects", mock(WintertodtObjectTracker.class));
        TrackerTestSupport.inject(plugin, "npcs", mock(WintertodtNpcTracker.class));
        TrackerTestSupport.inject(plugin, "arrow", mock(WintertodtHintArrow.class));
        TrackerTestSupport.inject(plugin, "paths", mock(WintertodtPathTracker.class));
        java.lang.reflect.Field field = WintertodtAutopilotPlugin.class.getDeclaredField("solo");
        field.setAccessible(true); ((WintertodtSoloEngine) field.get(plugin)).configure(5000, true);
        GameStateChanged loading = mock(GameStateChanged.class);
        when(loading.getGameState()).thenReturn(GameState.LOADING);
        plugin.onGameStateChanged(loading);
        assertTrue(plugin.isSoloFinishRequested()); verifyNoInteractions(manager);
        assertEquals(RoundState.OUTSIDE, plugin.getSnapshot().getRoundState());
        when(loading.getGameState()).thenReturn(GameState.HOPPING);
        plugin.onGameStateChanged(loading);
        assertFalse(plugin.isSoloFinishRequested());
        verify(manager).setConfiguration("wintertodtautopilot", "finishSoloRound", false);
    }

}
