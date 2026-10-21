package com.wintertodtautopilot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import net.runelite.api.NPC;

/** Read-only foundation for Solo. Scene observations take precedence; HUD compass positions provide remote coverage. */
public final class WintertodtCornerTracker
{
    private final EnumMap<WorkingCorner, CornerObservation> previous = new EnumMap<>(WorkingCorner.class);
    private final java.util.Map<WorkingCorner, NPC> healthActors = new EnumMap<>(WorkingCorner.class);
    private final java.util.Map<WorkingCorner, Integer> healthRatios = new EnumMap<>(WorkingCorner.class);
    private final java.util.Map<WorkingCorner, Integer> healthScales = new EnumMap<>(WorkingCorner.class);
    private final java.util.Map<WorkingCorner, Integer> healthTicks = new EnumMap<>(WorkingCorner.class);
    private int lastTick = -1;

    public void reset() { previous.clear(); healthActors.clear(); healthRatios.clear(); healthScales.clear(); healthTicks.clear(); lastTick = -1; }

    /** A spent healing dose invalidates the cached injury until the API value changes again. */
    public void invalidateHealth(WorkingCorner corner) { healthTicks.put(corner, -1); }

    public List<CornerObservation> observe(int tick, WintertodtObjectTracker objects, WintertodtNpcTracker npcs)
    {
        return observe(tick, objects, npcs, null);
    }

    public List<CornerObservation> observe(int tick, WintertodtObjectTracker objects,
        WintertodtNpcTracker npcs, net.runelite.api.Client client)
    {
        if (tick < lastTick) { reset(); }
        lastTick = tick;
        java.util.Map<WorkingCorner, BrazierState> hud = WintertodtBrazierHud.read(client);
        java.util.Map<WorkingCorner, BrazierState> scene = new EnumMap<>(WorkingCorner.class);
        boolean conflict = false;
        for (WorkingCorner c : WorkingCorner.values())
        {
            if (c == WorkingCorner.AUTOMATIC) { continue; }
            BrazierState state = objects.brazierAt(c);
            if (state == null) { state = BrazierState.UNKNOWN; }
            scene.put(c, state);
            BrazierState remote = hud.get(c);
            if (state != BrazierState.UNKNOWN && state != BrazierState.UNAVAILABLE
                && remote != null && remote != BrazierState.UNKNOWN && state != remote) { conflict = true; }
        }
        // A mismatch may be a redraw delay or a changed HUD. Do not trust remote readings that tick.
        if (conflict) { hud.clear(); }
        List<CornerObservation> result = new ArrayList<>(4);
        for (WorkingCorner corner : WorkingCorner.values())
        {
            if (corner == WorkingCorner.AUTOMATIC) { continue; }
            CornerObservation old = previous.get(corner);
            BrazierState brazier = scene.get(corner);
            boolean brazierVisible = brazier != BrazierState.UNKNOWN && brazier != BrazierState.UNAVAILABLE;
            CornerObservation.Source source = brazierVisible ? CornerObservation.Source.DIRECT_SCENE : CornerObservation.Source.NONE;
            if (!brazierVisible && hud.getOrDefault(corner, BrazierState.UNKNOWN) != BrazierState.UNKNOWN)
            { brazier = hud.get(corner); source = CornerObservation.Source.HUD_WIDGET; }
            boolean brazierKnown = source != CornerObservation.Source.NONE;
            NPC npc = npcs.target(TargetKind.PYROMANCER, corner.location());
            int id = npc == null ? -1 : WintertodtNpcTracker.id(npc);
            CornerObservation.PyromancerState pyro = id == net.runelite.api.gameval.NpcID.WINT_WIZARD
                ? CornerObservation.PyromancerState.ALIVE : id == net.runelite.api.gameval.NpcID.WINT_WIZARD_DOWN
                ? CornerObservation.PyromancerState.INCAPACITATED : CornerObservation.PyromancerState.UNKNOWN;
            boolean pyroVisible = pyro != CornerObservation.PyromancerState.UNKNOWN;
            int health = -1;
            if (pyro == CornerObservation.PyromancerState.INCAPACITATED) { health = 0; }
            else if (pyroVisible && npc.getHealthScale() > 0 && npc.getHealthRatio() >= 0
                && npc.getHealthRatio() <= npc.getHealthScale())
            {
                health = npc.getHealthRatio() * 100 / npc.getHealthScale();
            }
            // Actor visibility is not a new health update. Unchanged cached ratios expire.
            int healthTick = -1;
            if (health >= 0)
            {
                if (healthActors.get(corner) != npc || !java.util.Objects.equals(healthRatios.get(corner), npc.getHealthRatio())
                    || !java.util.Objects.equals(healthScales.get(corner), npc.getHealthScale())
                    || old == null || old.getPyromancerState() != pyro)
                { healthTicks.put(corner, tick); }
                healthActors.put(corner, npc); healthRatios.put(corner, npc.getHealthRatio()); healthScales.put(corner, npc.getHealthScale());
                healthTick = pyro == CornerObservation.PyromancerState.INCAPACITATED ? tick : healthTicks.getOrDefault(corner, -1);
                if (healthTick < 0 || tick - healthTick > CornerObservation.STALE_AFTER_TICKS) { health = -1; }
            }
            else
            { healthActors.remove(corner); healthRatios.remove(corner); healthScales.remove(corner); healthTicks.remove(corner); }
            CornerObservation observation = new CornerObservation(corner,
                brazierKnown ? brazier : BrazierState.UNKNOWN, pyro, health,
                source,
                pyroVisible ? CornerObservation.Source.DIRECT_SCENE : CornerObservation.Source.NONE,
                brazierKnown ? tick : old == null ? -1 : old.getBrazierObservedTick(),
                pyroVisible ? tick : old == null ? -1 : old.getPyromancerObservedTick(),
                brazierVisible, pyroVisible, healthTick);
            previous.put(corner, observation);
            result.add(observation);
        }
        return Collections.unmodifiableList(result);
    }
}
