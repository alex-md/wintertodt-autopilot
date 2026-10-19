package com.wintertodtautopilot;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/** Solo controller. Global evidence gates relighting; carried points never count as earned. */
public final class WintertodtSoloEngine implements WintertodtPlanner
{
    private final WintertodtSoloEnergyModel energy = new WintertodtSoloEnergyModel();
    private final WintertodtSoloSupplyPlan supplies = new WintertodtSoloSupplyPlan();
    @Getter private SoloPhase phase = SoloPhase.PREPARE;
    @Getter private String transitionReason = "initial";
    @Getter private boolean finishRequested;
    private boolean finishedRound;
    private boolean sawActive;
    private int pointGoal = 5000;
    private boolean reducingEnergy = true;
    private WorkingCorner route = WorkingCorner.AUTOMATIC;
    private ObjectiveType batch;
    private int batchTarget;
    private WorkingCorner home;
    private WorkingCorner finishAnchor = WorkingCorner.AUTOMATIC;
    private WorkingCorner finishSecondary = WorkingCorner.AUTOMATIC;
    private PlannerSettings previousSettings;

    public void configure(int pointGoal, boolean requestFinish)
    {
        this.pointGoal = Math.max(500, Math.min(100000, pointGoal));
        if (requestFinish && !finishedRound) { finishRequested = true; }
    }
    @Override public int getPreparationDoseTarget() { return supplies.getTarget(); }
    public String getEnergyTrend() { return energy.getTrend().name(); }
    @Override public void reset()
    {
        energy.reset(); supplies.reset(); phase = SoloPhase.PREPARE; transitionReason = "reset";
        finishRequested = false; finishedRound = false; sawActive = false; reducingEnergy = true;
        route = WorkingCorner.AUTOMATIC; batch = null; batchTarget = 0; home = null; previousSettings = null;
        finishAnchor = WorkingCorner.AUTOMATIC; finishSecondary = WorkingCorner.AUTOMATIC;
    }

    @Override public WintertodtObjective decide(WintertodtSnapshot s, PlannerSettings settings)
    {
        WorkingCorner selected = s.getWorkingCorner() == WorkingCorner.AUTOMATIC
            ? s.getPlayerLocation() == null ? WorkingCorner.SOUTHWEST : WorkingCorner.nearest(s.getPlayerLocation())
            : s.getWorkingCorner();
        if (!settings.equals(previousSettings))
        { previousSettings = settings; route = WorkingCorner.AUTOMATIC; batch = null; }
        // Automatic working-corner changes must not discard an opening/finish destination mid-walk.
        if (home != selected) { home = selected; batch = null; }
        // Only explicit completion/countdown evidence ends a session. A missing HUD is not a new round.
        if (s.getRoundState() == RoundState.COMPLETE
            || sawActive && s.getRoundState() == RoundState.COUNTDOWN && s.getSecondsToStart() > 0)
        {
            finishedRound = true; finishRequested = false; sawActive = false;
            reducingEnergy = true; energy.reset(); supplies.reset(); batch = null; route = WorkingCorner.AUTOMATIC;
        }
        if (s.active() && finishedRound)
        {
            finishedRound = false; finishRequested = false;
        }
        WintertodtObjective safety = WintertodtSafety.objective(s, settings);
        if (safety != null) { return safety; }
        if (!s.active())
        {
            if (s.getRoundState() == RoundState.OUTSIDE) { return waitFor("Waiting for Wintertodt", "outside"); }
            if (s.getRoundState() == RoundState.LOBBY)
            { return action(ObjectiveType.ENTER, "Enter the Wintertodt prison", "lobby", TargetKind.DOOR, home, 0, false); }
            if (s.getRoundState() == RoundState.COMPLETE)
            { setPhase(SoloPhase.PREPARE, "round_complete"); return action(ObjectiveType.ROUND_COMPLETE, "Round complete", "round_complete", TargetKind.NONE, home, 0, false); }
            if (sawActive && s.getSecondsToStart() <= 0)
            { setPhase(SoloPhase.REASSESS, "solo_hud_missing"); return waitFor("Waiting for current round observations", "solo_hud_missing"); }
            setPhase(SoloPhase.PREPARE, "solo_prepare");
            WintertodtObjective prep = supplies.prepare(s, settings, phase);
            return prep != null ? prep : action(ObjectiveType.POSITION, "Get ready at the " + home + " brazier",
                "countdown_position", TargetKind.BRAZIER, home, 0, false);
        }
        sawActive = true;
        if (s.getWintertodtEnergy() < 1 || s.getWintertodtEnergy() > 100 || s.getWarmth() < 0 || s.getObservationTick() < 0 || s.getPoints() < 0 && !finishRequested)
        {
            energy.observe(s.getObservationTick(), -1);
            setPhase(SoloPhase.REASSESS, "solo_signals_unknown");
            return waitFor("Check energy, points and warmth observations", "solo_signals_unknown");
        }
        energy.observe(s.getObservationTick(), s.getWintertodtEnergy());
        boolean finish = finishRequested || s.getPoints() >= pointGoal;
        if (finish) { setPhase(SoloPhase.FINISH, finishRequested ? "solo_finish_requested" : "solo_goal_earned"); }
        else if (energy.isRecovering()) { setPhase(SoloPhase.RECOVER_ENERGY, "solo_energy_low"); }
        else
        {
            if (s.getWintertodtEnergy() <= 28) { reducingEnergy = false; }
            else if (s.getWintertodtEnergy() > 35) { reducingEnergy = true; }
            setPhase(reducingEnergy ? SoloPhase.REDUCE_ENERGY : SoloPhase.SUSTAIN,
                reducingEnergy ? "solo_opening" : "solo_energy_band");
        }
        // Feed promptly on an unexplained rapid drop, without claiming another player is responsible.
        if (!finish && phase != SoloPhase.REDUCE_ENERGY && energy.isRapidDrop() && fuel(s) > 0
            && (!gathering(s, settings) || s.getWintertodtEnergy() <= WintertodtSoloEnergyModel.RECOVERY_TRIGGER))
        {
            WorkingCorner lit = nearestLit(s);
            if (lit != WorkingCorner.AUTOMATIC)
            {
                setPhase(SoloPhase.REASSESS, "solo_rapid_drain");
                return feed(s, lit, "solo_rapid_drain", true);
            }
        }
        // Converting fuel is separate from controlling energy. Feed an already lit home even during recovery.
        if (fuel(s) > 0 && (finish || s.projectedPoints() >= pointGoal))
        {
            WorkingCorner lit = nearestLit(s);
            if (lit != WorkingCorner.AUTOMATIC) { return feed(s, lit, finish ? "solo_finish_feed" : "solo_goal_feed", finish); }
        }
        WintertodtObjective prep = supplies.prepare(s, settings, phase);
        if (prep != null)
        {
            if ("supply_inventory_blocked".equals(prep.getReason()) && fuel(s) > 0 && nearestLit(s) != WorkingCorner.AUTOMATIC)
            { return feed(s, nearestLit(s), "solo_supply_space", false); }
            if ("supply_inventory_blocked".equals(prep.getReason()) && fuel(s) > 0
                && s.isHasHammer() && (s.isHasTinderbox() || s.isHasBrumaTorch())
                && (finish || !energy.isRecovering() && s.getWintertodtEnergy() > WintertodtSoloEnergyModel.RECOVERY_TRIGGER))
            {
                CornerObservation local = s.corner(home);
                if (local != null && local.getPyromancerState() == CornerObservation.PyromancerState.ALIVE)
                {
                    WintertodtObjective maintenance = maintain(s, home, finish);
                    if (maintenance != null) { return maintenance; }
                }
            }
            if (phase == SoloPhase.REDUCE_ENERGY) { setPhase(SoloPhase.PREPARE, "solo_prepare"); }
            return prep;
        }
        if (!finish && (phase == SoloPhase.RECOVER_ENERGY
            || s.getWintertodtEnergy() <= WintertodtSoloEnergyModel.RECOVERY_TRIGGER))
        {
            if (!gathering(s, settings) && fuel(s) > 0 && nearestLit(s) != WorkingCorner.AUTOMATIC
                && (batch == ObjectiveType.FEED || s.getBrumaRoots() == 0 && s.getBrumaKindling() > 0 || !settings.fletch(s)))
            { return feed(s, nearestLit(s), "solo_recovery_feed", false); }
            WintertodtObjective healing = pointsHealing(s, settings, true);
            if (healing != null) { return healing; }
            return productive(s, settings, true);
        }
        if (phase == SoloPhase.REDUCE_ENERGY || phase == SoloPhase.FINISH)
        {
            WorkingCorner destination = phase == SoloPhase.FINISH ? finishCorner(s) : maintenanceCorner(s);
            if (destination != WorkingCorner.AUTOMATIC)
            {
                WintertodtObjective maintenance = maintain(s, destination, true);
                if (maintenance != null) { return maintenance; }
            }
            if (phase == SoloPhase.FINISH)
            {
                if (fuel(s) > 0 && nearestLit(s) != WorkingCorner.AUTOMATIC)
                { return feed(s, nearestLit(s), "solo_finish_feed", true); }
                return waitFor("Finish requested - maintain drain until the round ends", "solo_finish_drain");
            }
        }
        else
        {
            WintertodtObjective healing = pointsHealing(s, settings, false);
            if (healing != null) { return healing; }
            // An unlit/broken brazier cannot stop woodcutting. Finish gathering
            // before asking the player to run back for maintenance.
            if (gathering(s, settings)) { return productive(s, settings, false); }
            WintertodtObjective maintenance = maintain(s, home, false);
            if (maintenance != null) { return maintenance; }
        }
        return productive(s, settings, false);
    }

    private WintertodtObjective maintain(WintertodtSnapshot s, WorkingCorner destination, boolean multi)
    {
        CornerObservation c = s.corner(destination);
        if (c == null || !c.brazierFresh(s.getObservationTick()) || !c.isBrazierVisible()
            || c.getBrazierState() == BrazierState.UNKNOWN)
        { return position(destination, "Move to observe the " + destination + " brazier", "solo_observe_corner"); }
        if (c.getBrazierState() == BrazierState.LIT)
        {
            if (c.isPyromancerVisible() && c.pyromancerFresh(s.getObservationTick())
                && c.getPyromancerState() == CornerObservation.PyromancerState.INCAPACITATED)
            {
                if (s.observedGlobalLitBraziers() < 0)
                { setPhase(SoloPhase.REASSESS, "solo_coverage_unknown"); return waitFor("Check all brazier indicators before restoring drain", "solo_coverage_unknown"); }
                return heal(s, destination);
            }
            return null;
        }
        // Repairs and healing can restart drain too; hold them during the low-energy lockout.
        if (phase != SoloPhase.FINISH && (s.getWintertodtEnergy() <= WintertodtSoloEnergyModel.RECOVERY_TRIGGER || energy.isRecovering()))
        { return waitFor("Let energy recover - do not relight", "solo_recovery_wait"); }
        if (s.observedGlobalLitBraziers() < 0)
        {
            setPhase(SoloPhase.REASSESS, "solo_coverage_unknown");
            return waitFor("Check all brazier indicators before relighting", "solo_coverage_unknown");
        }
        if (!multi)
        {
            for (CornerObservation other : s.getCorners())
            {
                if (other.getCorner() != home && other.getBrazierState() == BrazierState.LIT)
                { return productiveRecovery(s); }
            }
        }
        if (!c.isPyromancerVisible() || !c.pyromancerFresh(s.getObservationTick())
            || c.getPyromancerState() == CornerObservation.PyromancerState.UNKNOWN)
        { return position(destination, "Move closer to observe the " + destination + " pyromancer", "solo_observe_pyromancer"); }
        if (c.getPyromancerState() == CornerObservation.PyromancerState.INCAPACITATED) { return heal(s, destination); }
        if (c.getBrazierState() == BrazierState.BROKEN)
        { return action(ObjectiveType.REPAIR, "Repair the " + destination + " brazier", "solo_repair", TargetKind.BRAZIER, destination, 0, false, ItemID.HAMMER); }
        return action(ObjectiveType.LIGHT, "Light the " + destination + " brazier", "solo_light",
            TargetKind.BRAZIER, destination, 0, false, ItemID.TINDERBOX, ItemID.WINT_TORCH, ItemID.WINT_TORCH_OFFHAND);
    }

    private WintertodtObjective heal(WintertodtSnapshot s, WorkingCorner destination)
    {
        if (s.getRejuvenationDoses() <= 1)
        { return waitFor("Keep the last dose for warmth - prepare more potions", "solo_heal_reserve"); }
        return action(ObjectiveType.HEAL_PYROMANCER, "Heal the " + destination + " pyromancer", "solo_heal",
            TargetKind.PYROMANCER, destination, 1, false, s.getPotionItem());
    }
    private boolean gathering(WintertodtSnapshot s, PlannerSettings settings)
    {
        // Batch state survives animation gaps, warmth interruptions and relights.
        int carried = settings.fletch(s) ? s.potentialPointsIfFletched() : s.inventoryPoints();
        return batch == ObjectiveType.CHOP && s.getBrumaRoots() > 0 && fuel(s) < batchTarget && s.getFreeSlots() > 0
            && s.getCurrentActivity() != PlayerActivity.FEEDING && s.getCurrentActivity() != PlayerActivity.FLETCHING
            && s.getPoints() + carried < pointGoal;
    }

    private WintertodtObjective productiveRecovery(WintertodtSnapshot s)
    {
        WorkingCorner lit = nearestLit(s);
        if (!gathering(s, previousSettings) && fuel(s) > 0 && lit != WorkingCorner.AUTOMATIC
            && (batch == ObjectiveType.FEED || s.getBrumaRoots() == 0 && s.getBrumaKindling() > 0 || !previousSettings.fletch(s)))
        { return feed(s, lit, "solo_remote_feed", false); }
        return unlitWork(s, previousSettings, false);
    }
    private WintertodtObjective unlitWork(WintertodtSnapshot s, PlannerSettings settings, boolean recovery)
    {
        boolean fletching = settings.fletch(s);
        if (batch == ObjectiveType.FLETCH && (s.getBrumaRoots() == 0 || !fletching)) { batch = null; }
        int carried = fletching ? s.potentialPointsIfFletched() : s.inventoryPoints();
        int remainingPoints = Math.max(0, pointGoal - s.getPoints() - carried);
        boolean processRoots = s.getBrumaRoots() > 0 && fletching
            && (s.getFreeSlots() == 0 || (s.getBrumaKindling() > 0 && batch != ObjectiveType.CHOP) || remainingPoints == 0
                || batch == ObjectiveType.FLETCH || s.getCurrentActivity() == PlayerActivity.FLETCHING);
        if (processRoots)
        {
            batch = ObjectiveType.FLETCH;
            return fletch(s, recovery ? "solo_recovery_fletch" : "solo_remote_lit");
        }
        if (s.getFreeSlots() > 0 && remainingPoints > 0)
        {
            batch = ObjectiveType.CHOP;
            int unit = fletching ? 25 : 10;
            int count = Math.min(s.getFreeSlots(), (remainingPoints + unit - 1) / unit);
            // Recovery and remote-brazier work share the sustain batch. Carry its
            // total target across the relight so it cannot collapse to one root.
            batchTarget = fuel(s) + count;
            return chop(s, count,
                recovery ? "solo_recovery_chop" : "solo_remote_lit");
        }
        return waitFor(recovery ? "Let energy recover to 12% - do not relight" : "Remote brazier is lit - leave home unlit",
            recovery ? "solo_recovery_wait" : "solo_remote_lit");
    }

    private WintertodtObjective productive(WintertodtSnapshot s, PlannerSettings settings, boolean recovery)
    {
        if (recovery) { return unlitWork(s, settings, true); }
        if (batch == ObjectiveType.FEED && fuel(s) == 0) { batch = null; }
        if (batch == ObjectiveType.FLETCH && (s.getBrumaRoots() == 0 || !settings.fletch(s))) { batch = ObjectiveType.FEED; }
        if (batch == ObjectiveType.CHOP && (fuel(s) >= batchTarget || s.getFreeSlots() == 0))
        { batch = settings.fletch(s) && s.getBrumaRoots() > 0 ? ObjectiveType.FLETCH : ObjectiveType.FEED; }
        if (batch == null)
        {
            if (s.getBrumaKindling() > 0 || fuel(s) > 0 && (s.getFreeSlots() == 0
                || s.getPoints() >= 0 && s.getPoints() + (settings.fletch(s) ? s.potentialPointsIfFletched() : s.inventoryPoints()) >= pointGoal))
            { batch = settings.fletch(s) && s.getBrumaRoots() > 0 ? ObjectiveType.FLETCH : ObjectiveType.FEED; }
            else
            {
                batch = ObjectiveType.CHOP;
                int capacity = fuel(s) + s.getFreeSlots();
                int needed = s.getPoints() < 0 ? capacity : Math.max(1, (pointGoal - s.getPoints() + (settings.fletch(s) ? 24 : 9)) / (settings.fletch(s) ? 25 : 10));
                batchTarget = Math.min(capacity, needed);
            }
        }
        if (batch == ObjectiveType.FLETCH) { return fletch(s, "solo_batch_fletch"); }
        if (batch == ObjectiveType.FEED && fuel(s) > 0) { return feed(s, home, "solo_batch_feed", false); }
        if (s.getFreeSlots() == 0) { return waitFor("Make space for bruma roots", "inventory_blocked"); }
        return chop(s, Math.max(1, batchTarget - fuel(s)), "solo_batch_chop");
    }
    private WintertodtObjective fletch(WintertodtSnapshot s, String reason)
    {
        // Fletching is allowed at roots and while running; only the safe preparation area forbids it.
        if (s.getPlayerLocation() != null && s.getPlayerLocation().getY() < 3988)
        { return position(home, "Leave the preparation area to fletch", "solo_fletch_inside"); }
        return action(ObjectiveType.FLETCH, "Fletch " + s.getBrumaRoots() + " roots", reason,
            TargetKind.NONE, home, s.getBrumaRoots(), false, ItemID.KNIFE, ItemID.FLETCHING_KNIFE, ItemID.WINT_BRUMA_ROOT);
    }
    private WintertodtObjective chop(WintertodtSnapshot s, int count, String reason)
    { return action(ObjectiveType.CHOP, "Chop " + count + " bruma root" + (count == 1 ? "" : "s"), reason, TargetKind.ROOTS, home, count, false); }
    private WintertodtObjective feed(WintertodtSnapshot s, WorkingCorner destination, String reason, boolean urgent)
    {
        // Once a fuel trip starts, finish it instead of returning for a new root
        // as soon as feeding opens the first inventory slot.
        if (!"solo_supply_space".equals(reason)) { batch = ObjectiveType.FEED; }
        return action(ObjectiveType.FEED, "Feed fuel at the " + destination + " brazier", reason,
            TargetKind.BRAZIER, destination, fuel(s), urgent,
            s.getBrumaKindling() > 0 ? ItemID.WINT_BRUMA_KINDLING : ItemID.WINT_BRUMA_ROOT);
    }
    private WintertodtObjective position(WorkingCorner destination, String text, String reason)
    { return action(ObjectiveType.POSITION, text, reason, TargetKind.BRAZIER, destination, 0, false); }
    private WintertodtObjective waitFor(String text, String reason)
    { return action(ObjectiveType.WAIT, text, reason, TargetKind.NONE, home == null ? WorkingCorner.SOUTHWEST : home, 0, false); }
    private WintertodtObjective action(ObjectiveType type, String text, String reason, TargetKind target,
        WorkingCorner destination, int count, boolean urgent, int... items)
    { return new WintertodtObjective(type, text, reason, target, destination, count, urgent, items); }
    private void setPhase(SoloPhase next, String reason)
    {
        if (phase != next)
        {
            phase = next; transitionReason = reason; route = WorkingCorner.AUTOMATIC;
            if (next != SoloPhase.FINISH)
            { finishAnchor = WorkingCorner.AUTOMATIC; finishSecondary = WorkingCorner.AUTOMATIC; }
        }
    }
    private static int fuel(WintertodtSnapshot s) { return s.getBrumaRoots() + s.getBrumaKindling(); }
    private WorkingCorner nearestLit(WintertodtSnapshot s)
    {
        WorkingCorner result = WorkingCorner.AUTOMATIC;
        int distance = Integer.MAX_VALUE;
        for (CornerObservation c : s.getCorners())
        {
            if (c.getBrazierState() != BrazierState.LIT || !c.isBrazierVisible() || !c.brazierFresh(s.getObservationTick())) { continue; }
            int d = s.getPlayerLocation() == null ? (c.getCorner() == home ? 0 : 100) : s.getPlayerLocation().distanceTo(c.getCorner().location());
            if (d < distance) { result = c.getCorner(); distance = d; }
        }
        return result;
    }
    private WintertodtObjective pointsHealing(WintertodtSnapshot s, PlannerSettings settings, boolean recovery)
    {
        if (settings.getStrategy() == WintertodtStrategy.MASS_XP || s.getRejuvenationDoses() <= 1
            || gathering(s, settings)) { return null; }
        // Optional remote healing fits the processing stage, not each newly chopped root.
        boolean roaming = s.getBrumaRoots() > 0 && (batch == ObjectiveType.FLETCH
            || s.getCurrentActivity() == PlayerActivity.FLETCHING || s.getFreeSlots() == 0);
        WorkingCorner target = WorkingCorner.AUTOMATIC;
        int distance = Integer.MAX_VALUE;
        for (CornerObservation c : s.getCorners())
        {
            if (!c.isPyromancerVisible() || !c.pyromancerFresh(s.getObservationTick())
                || c.getPyromancerHealthPercent() < 0 || c.getPyromancerHealthPercent() >= 100
                || c.getPyromancerState() == CornerObservation.PyromancerState.UNKNOWN) { continue; }
            // Healing an incapacitated pyro at a lit brazier would restart drain during recovery.
            if (recovery && (!c.brazierFresh(s.getObservationTick())
                || c.getBrazierState() != BrazierState.UNLIT && c.getBrazierState() != BrazierState.BROKEN)) { continue; }
            if (c.getCorner() != home && (!roaming || s.getRunEnergyPercent() >= 0 && s.getRunEnergyPercent() <= 10)) { continue; }
            if (!recovery && c.getPyromancerState() == CornerObservation.PyromancerState.INCAPACITATED
                && c.getBrazierState() == BrazierState.LIT && s.observedGlobalLitBraziers() < 0) { continue; }
            int d = c.getCorner() == home ? 0 : s.getPlayerLocation() == null ? 100
                : s.getPlayerLocation().distanceTo(c.getCorner().location());
            if (d < distance) { distance = d; target = c.getCorner(); }
        }
        return target == WorkingCorner.AUTOMATIC ? null : action(ObjectiveType.HEAL_PYROMANCER,
            "Heal the " + target + " pyromancer", "solo_points_heal", TargetKind.PYROMANCER, target, 1, false, s.getPotionItem());
    }

    private WorkingCorner finishCorner(WintertodtSnapshot s)
    {
        if (finishAnchor == WorkingCorner.AUTOMATIC)
        {
            finishAnchor = home;
            int best = Integer.MAX_VALUE;
            for (WorkingCorner c : WorkingCorner.values())
            {
                if (c == WorkingCorner.AUTOMATIC || c == finishAnchor
                    || c.location().getX() != finishAnchor.location().getX() && c.location().getY() != finishAnchor.location().getY()) { continue; }
                CornerObservation observation = s.corner(c);
                int distance = s.getPlayerLocation() == null ? 100 : s.getPlayerLocation().distanceTo(c.location());
                if (observation != null && observation.brazierFresh(s.getObservationTick()) && observation.getBrazierState() == BrazierState.LIT)
                { distance -= 1000; }
                if (distance < best) { best = distance; finishSecondary = c; }
            }
        }
        boolean lowRun = s.getRunEnergyPercent() >= 0 && s.getRunEnergyPercent() <= 10;
        WorkingCorner anchor = lowRun ? home : finishAnchor;
        for (WorkingCorner c : lowRun ? new WorkingCorner[]{anchor} : new WorkingCorner[]{anchor, finishSecondary})
        {
            CornerObservation observation = s.corner(c);
            if (observation == null || !observation.brazierFresh(s.getObservationTick()) || observation.getBrazierState() != BrazierState.LIT
                || observation.getPyromancerState() == CornerObservation.PyromancerState.INCAPACITATED) { return c; }
        }
        return WorkingCorner.AUTOMATIC;
    }

    private WorkingCorner maintenanceCorner(WintertodtSnapshot s)
    {
        CornerObservation current = s.corner(route);
        if (route != WorkingCorner.AUTOMATIC && (current == null || current.getBrazierState() != BrazierState.LIT
            || current.getPyromancerState() == CornerObservation.PyromancerState.INCAPACITATED)) { return route; }
        route = WorkingCorner.AUTOMATIC;
        int distance = Integer.MAX_VALUE;
        for (WorkingCorner c : WorkingCorner.values())
        {
            if (c == WorkingCorner.AUTOMATIC) { continue; }
            CornerObservation observation = s.corner(c);
            if (observation != null && observation.brazierFresh(s.getObservationTick())
                && observation.getBrazierState() == BrazierState.LIT
                && observation.getPyromancerState() != CornerObservation.PyromancerState.INCAPACITATED) { continue; }
            int d = s.getPlayerLocation() == null ? (c == home ? 0 : 100) : s.getPlayerLocation().distanceTo(c.location());
            if (d < distance) { route = c; distance = d; }
        }
        return route;
    }
}
