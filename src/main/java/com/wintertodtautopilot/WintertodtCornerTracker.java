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
    private int lastTick = -1;

    public void reset() { previous.clear(); lastTick = -1; }

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
            CornerObservation observation = new CornerObservation(corner,
                brazierKnown ? brazier : BrazierState.UNKNOWN, pyro, health,
                source,
                pyroVisible ? CornerObservation.Source.DIRECT_SCENE : CornerObservation.Source.NONE,
                brazierKnown ? tick : old == null ? -1 : old.getBrazierObservedTick(),
                pyroVisible ? tick : old == null ? -1 : old.getPyromancerObservedTick(),
                brazierVisible, pyroVisible);
            previous.put(corner, observation);
            result.add(observation);
        }
        return Collections.unmodifiableList(result);
    }
}
