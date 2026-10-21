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
import net.runelite.client.events.ConfigChanged;

@PluginDescriptor(name = "Wintertodt Autopilot",
    description = "Mass and Solo Wintertodt guidance for supplies, warmth, points and rewards. Follow the highlights. You do the clicking.",
    tags = {"wintertodt", "wt", "firemaking", "fm", "woodcutting", "fletching", "skilling", "minigame",
        "helper", "guide", "brazier", "bruma", "warmth", "points", "rewards", "mass", "solo", "pyromancer", "potions", "kindling"})
@Slf4j
public class WintertodtAutopilotPlugin extends Plugin
{
    static final String UPDATE_NOTICE_VERSION = "0.3.0";
    static final String UPDATE_NOTICE_KEY = "lastUpdateNotice";
    static final String UPDATE_NOTICE = "Wintertodt Autopilot 0.3.0: Solo mode is ready! Pick Solo in settings, set your point goal, and choose Comfortable or Active. Potion prep, recovery and healing cues have been improved. You still do every click.";
    @Inject private Client client;
    @Inject private WintertodtSoloCapture soloCapture;
    @Inject private ClientThread clientThread;
    @Inject private ConfigManager configManager;
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
    public boolean isPaused() { return config.pauseGuidance(); }
    private final WintertodtDecisionEngine engine = new WintertodtDecisionEngine();
    private final WintertodtSoloEngine solo = new WintertodtSoloEngine();
    public SoloPhase getSoloPhase() { return solo.getPhase(); }
    public String getSoloEnergyTrend() { return solo.getEnergyTrend(); }
    public boolean isSoloFinishRequested() { return solo.isFinishRequested(); }
    private final WintertodtAttentionTracker attention = new WintertodtAttentionTracker();
    @Getter private WintertodtSnapshot snapshot = WintertodtSnapshot.builder().build();
    @Getter private WintertodtObjective objective;
    @Getter private TileObject targetObject;
    @Getter private NPC targetNpc;
    @Getter private WorldPoint targetPoint;
    @Getter private int potionTargetDoses;
    public java.util.List<WorldPoint> getPath() { return paths.getPath(); }
    private boolean bootstrapNeeded = true;
    public String getSoloReadiness() { return solo.getReadiness(); }
    public String getSoloReserveDetail() { return solo.getReserveDetail(); }
    private boolean captureRunning;
    private boolean captureFailed;
    private long captureSequence;
    private WintertodtSoloCapture.Lifecycle captureLifecycle = WintertodtSoloCapture.Lifecycle.RESET;
    private final WintertodtSoloRunSummary runSummary = new WintertodtSoloRunSummary();

    @Provides
    WintertodtAutopilotConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(WintertodtAutopilotConfig.class);
    }
    @Override
    protected void startUp() throws Exception
    {
        overlays.add(panel); overlays.add(scene); overlays.add(inventory);
        clientThread.invoke(this::reset);
    }
    @Override
    protected void shutDown() throws Exception
    {
        overlays.remove(panel); overlays.remove(scene); overlays.remove(inventory);
        clientThread.invoke(this::reset);
    }
    private void reset()
    {
        if (config != null && config.debug())
        {
            String summary = runSummary.end("session_reset");
            if (summary != null) { log.debug("{}", summary); }
        }
        else { runSummary.reset(); }
        clearObservations(); solo.reset();
        captureLifecycle = WintertodtSoloCapture.Lifecycle.RESET;
    }
    private void clearObservations()
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
            case CONNECTION_LOST:
                clearObservations();
                solo.suspendObservations();
                if (captureLifecycle != WintertodtSoloCapture.Lifecycle.RESET)
                { captureLifecycle = WintertodtSoloCapture.Lifecycle.SUSPEND; }
                break;
            case HOPPING:
            case LOGIN_SCREEN:
                reset();
                if (config.finishSoloRound())
                { configManager.setConfiguration("wintertodtautopilot", "finishSoloRound", false); }
                break;
            default: break;
        }
    }
    private void showUpdateNotice()
    {
        // Run anywhere after login, including for players who have paused gameplay guidance.
        if (UPDATE_NOTICE_VERSION.equals(configManager.getConfiguration("wintertodtautopilot", UPDATE_NOTICE_KEY)))
        { return; }
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", UPDATE_NOTICE, null);
        configManager.setConfiguration("wintertodtautopilot", UPDATE_NOTICE_KEY, UPDATE_NOTICE_VERSION);
    }
    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN) { return; }
        showUpdateNotice();
        Player player = client.getLocalPlayer();
        if (player == null) { return; }
        boolean inRegion = player.getWorldLocation().getRegionID() == WintertodtConstants.REGION;
        if (!inRegion)
        {
            if (snapshot.getRoundState() != RoundState.OUTSIDE || solo.isAwaitingObservations()) { reset(); }
            if (config.finishSoloRound())
            { configManager.setConfiguration("wintertodtautopilot", "finishSoloRound", false); }
            return;
        }
        if (bootstrapNeeded)
        {
            if (client.getTopLevelWorldView() == null) { return; }
            objects.bootstrap();
            for (NPC npc : client.getTopLevelWorldView().npcs()) { npcs.add(npc); }
            bootstrapNeeded = false;
        }
        boolean soloMode = config.mode() == WintertodtMode.SOLO;
        WintertodtSnapshot next = tracker.snapshot(config.workingCorner());
        if (soloMode && next.active() && (next.getWintertodtEnergy() > 28 || solo.isFinishRequested()
            || config.finishSoloRound() || next.getPoints() >= config.soloPointGoal()))
        {
            next = next.toBuilder().clearBrazierRoutes().brazierRoutes(paths.observeBrazierRoutes(next, objects)).build();
        }
        if (snapshot.active() != next.active()) { engine.reset(); }
        boolean verifiedBoundary = next.getRoundState() == RoundState.COMPLETE
            || next.getRoundState() == RoundState.COUNTDOWN && next.getSecondsToStart() > 0
            || next.active() && snapshot.getRoundState() == RoundState.COUNTDOWN && snapshot.getSecondsToStart() > 0;
        if (soloMode && verifiedBoundary && config.finishSoloRound())
        { configManager.setConfiguration("wintertodtautopilot", "finishSoloRound", false); }
        boolean capture = config.debug() && soloMode && !captureFailed;
        com.google.gson.JsonElement before = null;
        if (capture)
        {
            try { before = soloCapture.checkpoint(solo); }
            catch (RuntimeException e) { captureFault(e); capture = false; }
        }
        boolean requestFinish = soloMode && !verifiedBoundary && next.active() && config.finishSoloRound();
        solo.configure(config.soloPointGoal(), requestFinish);
        WintertodtPlanner planner = soloMode ? solo : engine;
        PlannerSettings settings = PlannerSettings.from(config);
        WintertodtObjective chosen = planner.decide(next, settings);
        if (config.debug() && (objective == null || objective.getType() != chosen.getType()
            || !objective.getReason().equals(chosen.getReason())))
        {
            log.debug("{} -> {} reason={}", objective == null ? "NONE" : objective.getType(), chosen.getType(), chosen.getReason());
        }
        if (config.debug())
        {
            String trace = WintertodtObservationTrace.format(next, chosen, config.mode(), soloMode ? solo.getPhase() : null,
                solo.getEnergyTrend(), solo.getTransitionReason(), solo.isFinishRequested());
            if (soloMode) { trace += " batch=" + solo.batchStage() + " batchTarget=" + solo.batchTarget() + " recoveryLatched=" + solo.recovering(); }
            log.debug("{}", trace);
        }
        if (capture)
        {
            try
            {
                if (!captureRunning) { captureSequence = 0; }
                log.debug("{}", soloCapture.encode(next, settings, config.soloPointGoal(), requestFinish,
                    captureLifecycle, !captureRunning, captureSequence++, before, chosen, solo.getPhase(), client));
                captureLifecycle = WintertodtSoloCapture.Lifecycle.NONE;
                String summary = runSummary.observe(next, chosen, solo.getPhase());
                if (summary != null) { log.debug("{}", summary); }
            }
            catch (RuntimeException e) { captureFault(e); capture = false; }
        }
        else { runSummary.reset(); }
        captureRunning = capture;
        snapshot = next; objective = chosen;
        potionTargetDoses = planner.getPreparationDoseTarget();
        updateCues();
        if (isPaused()) { return; }
        String message = attention.notification(snapshot, objective, config, client.getTickCount());
        if (message != null) { notifier.notify(message); }
    }
    private void captureFault(RuntimeException e)
    {
        captureFailed = true; runSummary.reset();
        log.warn("Solo diagnostic capture stopped; gameplay guidance continues. Toggle debug to retry.", e);
    }
    @Subscribe
    public void onClientTick(ClientTick event)
    {
        // The warmth varbit can change between game ticks. Retire an obsolete
        // drink/retreat instruction before another frame displays its potion cue.
        if (objective == null || objective.getType() != ObjectiveType.RESTORE_WARMTH
            || client.getGameState() != GameState.LOGGED_IN) { return; }
        Player player = client.getLocalPlayer();
        if (player == null || player.getWorldLocation().getRegionID() != WintertodtConstants.REGION) { return; }
        int rawWarmth = client.getVarbitValue(net.runelite.api.gameval.VarbitID.WINT_WARMTH);
        if (rawWarmth < 0 || rawWarmth > 1000) { return; }
        int warmth = rawWarmth / 10;
        PlannerSettings settings = PlannerSettings.from(config);
        if (warmth < 100 && warmth <= settings.getRestoreWarmth()) { return; }
        snapshot = snapshot.toBuilder().warmth(warmth).rawWarmth(rawWarmth).build();
        // Retire stale cues now; decisions and state transitions happen only on GameTick.
        objective = new WintertodtObjective(ObjectiveType.WAIT, "Warmth restored", "warmth_restored", TargetKind.NONE, 0, false);
        updateCues();
    }
    private void updateCues()
    {
        if (isPaused()) { clearGuidance(); return; }
        resolveTarget();
        boolean needsCue = !WintertodtGuidance.performing(snapshot, objective);
        paths.update(targetPoint, targetObject, targetNpc, needsCue && (config.showPath() || config.highlightStandingTiles()));
        arrow.update(targetNpc, targetPoint, needsCue && config.showHintArrow());
    }
    private void clearGuidance()
    {
        arrow.clear(); paths.reset();
        targetObject = null; targetNpc = null; targetPoint = null;
    }
    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if ("wintertodtautopilot".equals(event.getGroup()))
        {
            clientThread.invoke(() -> {
                if ("pauseGuidance".equals(event.getKey()))
                {
                    attention.reset();
                    if (isPaused()) { clearGuidance(); }
                    else { engine.reset(); }
                }
                else if ("debug".equals(event.getKey()))
                { captureFailed = false; captureRunning = false; runSummary.reset(); }
                else if ("mode".equals(event.getKey()))
                {
                    engine.reset(); solo.reset(); clearGuidance();
                    captureLifecycle = WintertodtSoloCapture.Lifecycle.RESET;
                    if (config.finishSoloRound())
                    { configManager.setConfiguration("wintertodtautopilot", "finishSoloRound", false); }
                }
                else if ("soloPointGoal".equals(event.getKey()) || "finishSoloRound".equals(event.getKey()))
                { clearGuidance(); }
                else if ("soloEffort".equals(event.getKey()) || "soloStrategy".equals(event.getKey()) || "strategy".equals(event.getKey()) || "pointGoal".equals(event.getKey())
                    || "fletchMode".equals(event.getKey()) || "workingCorner".equals(event.getKey()))
                {
                    engine.reset();
                    clearGuidance();
                }
                // Apply visual settings immediately, even between game ticks.
                if (!config.showHintArrow()) { arrow.clear(); }
                if (!config.showPath() && !config.highlightStandingTiles()) { paths.reset(); }
            });
        }
    }
    private void resolveTarget()
    {
        targetObject = null; targetNpc = null; targetPoint = null;
        TargetKind target = objective.getTarget();
        WorkingCorner destination = objective.getDestination() == WorkingCorner.AUTOMATIC
            ? snapshot.getWorkingCorner() : objective.getDestination();
        if (target == TargetKind.NONE) { return; }
        if (target == TargetKind.PYROMANCER || target == TargetKind.BREWMA)
        {
            targetNpc = npcs.target(target, target == TargetKind.PYROMANCER
                ? destination.location() : snapshot.getPlayerLocation());
            if (targetNpc != null) { targetPoint = targetNpc.getWorldLocation(); }
        }
        else
        {
            targetObject = objective.getDestination() == WorkingCorner.AUTOMATIC
                ? objects.target(target, snapshot.getPlayerLocation())
                : objects.target(target, snapshot.getPlayerLocation(), destination);
            if (targetObject != null) { targetPoint = targetObject.getWorldLocation(); }
        }
        if (targetPoint == null && (target == TargetKind.BRAZIER || target == TargetKind.PYROMANCER))
        {
            targetPoint = destination.location();
        }
    }
    @Subscribe public void onAnimationChanged(AnimationChanged e) { tracker.animation(e.getActor()); }
    @Subscribe public void onChatMessage(ChatMessage e) { tracker.chat(e); }
    @Subscribe public void onMenuOptionClicked(MenuOptionClicked e) { tracker.clicked(e); }
    @Subscribe public void onGraphicsObjectCreated(GraphicsObjectCreated e)
    {
        if (!config.debug() || config.mode() != WintertodtMode.SOLO) { return; }
        Player player = client.getLocalPlayer();
        GraphicsObject effect = e.getGraphicsObject();
        WorldView view = client.getTopLevelWorldView();
        if (player == null || player.getWorldLocation().getRegionID() != WintertodtConstants.REGION
            || view == null || effect == null || effect.getWorldView() != view || effect.getLocation() == null) { return; }
        WorldPoint point = WorldPoint.fromScene(view, effect.getLocation().getSceneX(),
            effect.getLocation().getSceneY(), effect.getLevel());
        log.debug("solo_effect_v1 tick={} id={} x={} y={} plane={} startCycle={} observedCycle={}",
            client.getTickCount(), effect.getId(), point.getX(), point.getY(), point.getPlane(),
            effect.getStartCycle(), client.getGameCycle());
    }
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
