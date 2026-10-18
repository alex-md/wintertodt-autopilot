package com.wintertodtautopilot;

import lombok.Value;

/** Immutable scene evidence. Missing scene entities require current HUD evidence; history alone is unknown. */
@Value
public class CornerObservation
{
    public enum Source { DIRECT_SCENE, HUD_WIDGET, NONE }
    public enum PyromancerState { ALIVE, INCAPACITATED, UNKNOWN }
    public static final int STALE_AFTER_TICKS = 8;

    WorkingCorner corner;
    BrazierState brazierState;
    PyromancerState pyromancerState;
    int pyromancerHealthPercent;
    Source brazierSource;
    Source pyromancerSource;
    int brazierObservedTick;
    int pyromancerObservedTick;
    boolean brazierVisible;
    boolean pyromancerVisible;

    public boolean brazierFresh(int tick) { return fresh(brazierObservedTick, tick); }
    public boolean pyromancerFresh(int tick) { return fresh(pyromancerObservedTick, tick); }
    private static boolean fresh(int observed, int tick)
    {
        return observed >= 0 && tick >= observed && tick - observed <= STALE_AFTER_TICKS;
    }
}
