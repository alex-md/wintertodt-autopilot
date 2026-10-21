package com.wintertodtautopilot;

import static com.wintertodtautopilot.WintertodtSnapshot.ActionState.*;

/** Bounded observation evidence, independent of the planner. A request never proves success. */
final class WintertodtActionTracker
{
    private PlayerActivity requested = PlayerActivity.IDLE;
    private WorkingCorner destination = WorkingCorner.AUTOMATIC;
    private int requestedAt = -1;
    private WintertodtSnapshot baseline;
    private int spent;

    void reset() { requested = PlayerActivity.IDLE; destination = WorkingCorner.AUTOMATIC; requestedAt = -1; baseline = null; spent = 0; }
    void request(PlayerActivity action, WorkingCorner corner, WintertodtSnapshot before, int tick)
    { reset(); requested = action; destination = corner == null ? WorkingCorner.AUTOMATIC : corner; baseline = before; requestedAt = tick; }

    WintertodtSnapshot observe(WintertodtSnapshot before, WintertodtSnapshot now, PlayerActivity animation)
    {
        WintertodtSnapshot.WintertodtSnapshotBuilder b = now.toBuilder().actionState(NONE)
            .actionActivity(PlayerActivity.IDLE).actionCorner(WorkingCorner.AUTOMATIC).personalDosesUsed(0).healingDosesUsed(0);
        int tick = now.getObservationTick();
        boolean consecutive = tick == before.getObservationTick() + 1 && (now.active() && before.active()
            || now.getRoundState() == RoundState.COUNTDOWN && before.getRoundState() == RoundState.COUNTDOWN);
        if (!consecutive || !now.isInventoryKnown() || !before.isInventoryKnown())
        {
            // Duplicate reads cannot consume or confirm anything a second time.
            if (tick != before.getObservationTick() || !now.isInventoryKnown() || !before.isInventoryKnown()) { reset(); }
            return b.actionState(NONE).actionActivity(PlayerActivity.IDLE).personalDosesUsed(0).healingDosesUsed(0).build();
        }
        int used = now.isInventoryKnown() && before.isInventoryKnown()
            ? Math.max(0, before.getRejuvenationDoses() - now.getRejuvenationDoses()) : 0;
        if (now.getWarmthAction() == WintertodtSnapshot.WarmthAction.CONFIRMED && used > 0
            && requested == PlayerActivity.RESTORING_WARMTH)
        { b.personalDosesUsed(Math.min(1, used)); }
        boolean pending = requestedAt >= 0 && tick >= requestedAt && tick - requestedAt <= 3;
        if (pending) { spent += used; }
        PlayerActivity confirmed = PlayerActivity.IDLE;
        if (now.isInventoryKnown() && before.isInventoryKnown())
        {
            if (now.getBrumaRoots() > before.getBrumaRoots()) { confirmed = PlayerActivity.CHOPPING; }
            else if (now.getBrumaRoots() < before.getBrumaRoots()
                && now.getBrumaKindling() - before.getBrumaKindling() == before.getBrumaRoots() - now.getBrumaRoots())
            { confirmed = PlayerActivity.FLETCHING; }
            else if (before.getPoints() >= 0 && now.getPoints() > before.getPoints()
                && now.getBrumaRoots() <= before.getBrumaRoots() && now.getBrumaKindling() <= before.getBrumaKindling()
                && now.getPoints() - before.getPoints() == before.inventoryPoints() - now.inventoryPoints())
            { confirmed = PlayerActivity.FEEDING; }
            else if (now.getRejuvenationDoses() > before.getRejuvenationDoses()
                && now.getUnfinishedPotionCount() < before.getUnfinishedPotionCount()
                && now.getBrumaHerbCount() < before.getBrumaHerbCount()) { confirmed = PlayerActivity.MAKING_POTION; }
        }
        if (pending && baseline != null)
        {
            CornerObservation old = baseline.corner(destination), current = now.corner(destination);
            if (old != null && current != null)
            {
                boolean scene = current.isBrazierVisible() && current.brazierFresh(tick)
                    && old.isBrazierVisible() && old.brazierFresh(baseline.getObservationTick());
                if (requested == PlayerActivity.LIGHTING && scene && old.getBrazierState() == BrazierState.UNLIT
                    && current.getBrazierState() == BrazierState.LIT) { confirmed = requested; }
                if (requested == PlayerActivity.REPAIRING && scene && old.getBrazierState() == BrazierState.BROKEN
                    && (current.getBrazierState() == BrazierState.UNLIT || current.getBrazierState() == BrazierState.LIT)) { confirmed = requested; }
                if (requested == PlayerActivity.HEALING && spent == 1 && current.isPyromancerVisible() && current.pyromancerFresh(tick)
                    && current.getPyromancerState() == CornerObservation.PyromancerState.ALIVE
                    && (old.getPyromancerState() == CornerObservation.PyromancerState.INCAPACITATED
                        || old.healthFresh(baseline.getObservationTick()) && current.healthFresh(tick)
                            && current.getPyromancerHealthPercent() > old.getPyromancerHealthPercent()))
                { confirmed = requested; b.healingDosesUsed(1); }
            }
        }
        if (now.getWarmthAction() == WintertodtSnapshot.WarmthAction.CONFIRMED) { confirmed = PlayerActivity.RESTORING_WARMTH; }
        if (confirmed != PlayerActivity.IDLE)
        {
            b.actionState(CONFIRMED).actionActivity(confirmed).actionCorner(destination);
            if (confirmed == requested) { reset(); }
        }
        else if (now.isInterrupted())
        { b.actionState(INTERRUPTED).actionActivity(requested == PlayerActivity.IDLE ? before.getCurrentActivity() : requested).actionCorner(destination); reset(); }
        else if (animation != PlayerActivity.IDLE)
        { b.actionState(IN_PROGRESS).actionActivity(animation).actionCorner(destination); }
        else if (requested != PlayerActivity.IDLE)
        {
            b.actionState(pending ? REQUESTED : UNCONFIRMED).actionActivity(requested).actionCorner(destination);
            if (!pending) { reset(); }
        }
        return b.build();
    }
}
