package com.wintertodtautopilot;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(name = "Wintertodt Autopilot",
    description = "One next action with local targets, warmth and reward planning for Wintertodt",
    tags = {"wintertodt", "firemaking", "minigame", "helper"})
@Slf4j
public class WintertodtAutopilotPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private WintertodtAutopilotConfig config;
    @Inject private WintertodtStateTracker tracker;
    @Inject private WintertodtObjectTracker objects;
    @Inject private WintertodtNpcTracker npcs;
    @Inject private WintertodtHintArrow arrow;
    @Inject private WintertodtPathTracker paths;
    @Inject private OverlayManager overlays;
    @Inject private WintertodtOverlay panel;
    @Inject private WintertodtSceneOverlay scene;
    @Inject private WintertodtInventoryOverlay inventory;
    @Inject private Notifier notifier;
    private final WintertodtDecisionEngine engine = new WintertodtDecisionEngine();
    private final WintertodtAttentionTracker attention = new WintertodtAttentionTracker();
    @Getter private WintertodtSnapshot snapshot = WintertodtSnapshot.builder().build();
    @Getter private WintertodtObjective objective;
    @Getter private TileObject targetObject;
    @Getter private NPC targetNpc;
    @Getter private WorldPoint targetPoint;
    @Getter private int potionTargetDoses;
    public java.util.List<WorldPoint> getPath() { return paths.getPath(); }
    private boolean bootstrapNeeded = true;

    @Provides
    WintertodtAutopilotConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(WintertodtAutopilotConfig.class);
    }
    @Override
    protected void startUp()
    {
        overlays.add(panel); overlays.add(scene); overlays.add(inventory);
        clientThread.invoke(this::reset);
    }
    @Override
    protected void shutDown()
    {
        overlays.remove(panel); overlays.remove(scene); overlays.remove(inventory);
        clientThread.invoke(this::reset);
    }
    private void reset()
    {
        arrow.clear(); paths.reset(); potionTargetDoses = 0; tracker.reset(); objects.reset(); npcs.reset(); engine.reset(); attention.reset();
        snapshot = WintertodtSnapshot.builder().build(); objective = null;
        targetObject = null; targetNpc = null; targetPoint = null; bootstrapNeeded = true;
    }
    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        switch (event.getGameState())
        {
            case LOADING:
            case HOPPING:
            case LOGIN_SCREEN:
            case CONNECTION_LOST: reset(); break;
            default: break;
        }
    }
    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN) { return; }
        Player player = client.getLocalPlayer();
        boolean inRegion = player != null && player.getWorldLocation().getRegionID() == WintertodtConstants.REGION;
        if (!inRegion)
        {
            if (snapshot.getRoundState() != RoundState.OUTSIDE) { reset(); }
            return;
        }
        if (bootstrapNeeded)
        {
            objects.bootstrap();
            for (NPC npc : client.getTopLevelWorldView().npcs()) { npcs.add(npc); }
            bootstrapNeeded = false;
        }
        WintertodtSnapshot next = tracker.snapshot(config.workingCorner());
        if (snapshot.active() != next.active()) { engine.reset(); }
        WintertodtObjective chosen = engine.decide(next, PlannerSettings.from(config));
        if (config.debug() && (objective == null || objective.getType() != chosen.getType()
            || !objective.getReason().equals(chosen.getReason())))
        {
            log.debug("{} -> {} reason={}", objective == null ? "NONE" : objective.getType(), chosen.getType(), chosen.getReason());
        }
        snapshot = next; objective = chosen;
        potionTargetDoses = engine.getPreparationDoseTarget();
        resolveTarget();
        paths.update(targetPoint, targetObject, targetNpc, config.showPath() || config.highlightStandingTiles());
        arrow.update(targetNpc, targetPoint, config.showHintArrow());
        String message = attention.notification(snapshot, objective, config, client.getTickCount());
        if (message != null) { notifier.notify(message); }
    }
    private void resolveTarget()
    {
        targetObject = null; targetNpc = null; targetPoint = null;
        TargetKind target = objective.getTarget();
        if (target == TargetKind.NONE) { return; }
        if (target == TargetKind.PYROMANCER || target == TargetKind.BREWMA)
        {
            targetNpc = npcs.target(target, target == TargetKind.PYROMANCER
                ? snapshot.getWorkingCorner().location() : snapshot.getPlayerLocation());
            if (targetNpc != null) { targetPoint = targetNpc.getWorldLocation(); }
        }
        else
        {
            targetObject = objects.target(target, snapshot.getPlayerLocation());
            if (targetObject != null) { targetPoint = targetObject.getWorldLocation(); }
        }
        if (targetPoint == null && (target == TargetKind.BRAZIER || target == TargetKind.PYROMANCER))
        {
            targetPoint = snapshot.getWorkingCorner().location();
        }
    }
    @Subscribe public void onAnimationChanged(AnimationChanged e) { tracker.animation(e.getActor()); }
    @Subscribe public void onChatMessage(ChatMessage e) { tracker.chat(e); }
    @Subscribe public void onMenuOptionClicked(MenuOptionClicked e) { tracker.clicked(e); }
    @Subscribe public void onGameObjectSpawned(GameObjectSpawned e) { paths.invalidate(); objects.add(e.getGameObject()); }
    @Subscribe public void onGameObjectDespawned(GameObjectDespawned e) { removed(e.getGameObject()); }
    @Subscribe public void onWallObjectSpawned(WallObjectSpawned e) { paths.invalidate(); objects.add(e.getWallObject()); }
    @Subscribe public void onWallObjectDespawned(WallObjectDespawned e) { removed(e.getWallObject()); }
    @Subscribe public void onDecorativeObjectSpawned(DecorativeObjectSpawned e) { paths.invalidate(); objects.add(e.getDecorativeObject()); }
    @Subscribe public void onDecorativeObjectDespawned(DecorativeObjectDespawned e) { removed(e.getDecorativeObject()); }
    @Subscribe public void onGroundObjectSpawned(GroundObjectSpawned e) { paths.invalidate(); objects.add(e.getGroundObject()); }
    @Subscribe public void onGroundObjectDespawned(GroundObjectDespawned e) { removed(e.getGroundObject()); }
    private void removed(TileObject object)
    {
        paths.invalidate(); objects.remove(object);
        if (targetObject == object) { targetObject = null; arrow.clear(); }
    }
    @Subscribe public void onNpcSpawned(NpcSpawned e) { npcs.add(e.getNpc()); }
    @Subscribe public void onNpcChanged(NpcChanged e) { npcs.remove(e.getNpc()); npcs.add(e.getNpc()); }
    @Subscribe public void onNpcDespawned(NpcDespawned e)
    {
        npcs.remove(e.getNpc());
        if (targetNpc == e.getNpc()) { targetNpc = null; arrow.clear(); }
    }
}
