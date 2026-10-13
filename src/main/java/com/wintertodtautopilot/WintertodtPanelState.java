package com.wintertodtautopilot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Value;

/** Immutable handoff from the client thread to Swing. */
@Value
public class WintertodtPanelState
{
    WintertodtSnapshot snapshot;
    WintertodtObjective objective;
    PlannerSettings settings;
    WorkingCorner selectedCorner;
    int potionTargetDoses;
    boolean paused;
    boolean showPath;

    public List<Requirement> requirements()
    {
        WintertodtSnapshot s = snapshot;
        boolean observed = s.getRoundState() != RoundState.OUTSIDE;
        List<Requirement> rows = new ArrayList<>();
        rows.add(tool("Axe", s.isHasAxe(), observed, true));
        rows.add(tool("Hammer", s.isHasHammer(), observed, true));
        rows.add(tool("Knife", s.isHasKnife(), observed, WintertodtGuidance.needsKnife(settings)));
        rows.add(tool("Fire starter", s.isHasTinderbox() || s.isHasBrumaTorch(), observed, true));
        int reserve = potionTargetDoses > 0 ? potionTargetDoses
            : WintertodtSupplyPlan.recommendedPotions(s, settings.getRestoreWarmth()) * 4;
        rows.add(new Requirement("Potion reserve", !observed ? "Checked at Wintertodt"
            : s.getRejuvenationDoses() + " / " + reserve + " doses",
            !observed ? Status.UNKNOWN : s.getRejuvenationDoses() >= reserve ? Status.READY : Status.CHECK));
        rows.add(new Requirement("Warm clothing", !observed || s.getWarmClothingCount() < 0 ? "Unknown"
            : Math.min(4, s.getWarmClothingCount()) + " / 4 equipped",
            !observed || s.getWarmClothingCount() < 0 ? Status.UNKNOWN
                : s.getWarmClothingCount() >= 4 ? Status.READY : Status.CHECK));
        return Collections.unmodifiableList(rows);
    }

    private static Requirement tool(String name, boolean held, boolean observed, boolean required)
    {
        return new Requirement(name, !required ? "Optional for this strategy" : !observed ? "Checked at Wintertodt"
            : held ? "Ready" : "Take from supply crate",
            !required || !observed ? Status.UNKNOWN : held ? Status.READY : Status.CHECK);
    }

    enum Status { READY, CHECK, UNKNOWN }
    @Value static class Requirement
    {
        String name;
        String detail;
        Status status;
    }
}
