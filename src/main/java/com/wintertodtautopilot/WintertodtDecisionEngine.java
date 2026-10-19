package com.wintertodtautopilot;

import net.runelite.api.gameval.ItemID;

/** Deterministic tick planner. Emergency objectives never overwrite the normal batch. */
public final class WintertodtDecisionEngine implements WintertodtPlanner
{
    private ObjectiveType normal;
    private int batchTarget;
    private int kindlingTarget;
    private WorkingCorner corner;
    private PlannerSettings previousSettings;
    private boolean suppliesReady;
    private int preparationDoseTarget;
    private boolean finishing;
    private boolean finishComplete;

    public int getPreparationDoseTarget() { return preparationDoseTarget; }

    public void reset()
    {
        normal = null;
        batchTarget = 0;
        kindlingTarget = 0;
        corner = null;
        previousSettings = null;
        suppliesReady = false;
        preparationDoseTarget = 0;
        finishing = false;
        finishComplete = false;
    }

    public WintertodtObjective decide(WintertodtSnapshot s, PlannerSettings settings)
    {
        if (!settings.equals(previousSettings))
        {
            reset();
            previousSettings = settings;
        }
        if (corner != s.getWorkingCorner())
        {
            normal = null;
            corner = s.getWorkingCorner();
            finishing = false;
            finishComplete = false;
        }
        WintertodtObjective safety = WintertodtSafety.objective(s, settings);
        if (safety != null) { return safety; }

        if (!s.active())
        {
            normal = null;
            finishing = false;
            finishComplete = false;
            if (s.getRoundState() == RoundState.OUTSIDE)
            {
                return action(ObjectiveType.WAIT, "Waiting for Wintertodt", "outside", TargetKind.NONE);
            }
            if (s.getRoundState() == RoundState.LOBBY)
            {
                return action(ObjectiveType.ENTER, "Enter the Wintertodt prison", "lobby", TargetKind.DOOR);
            }
            WintertodtObjective preparation = prepare(s, settings);
            if (preparation != null) { return preparation; }
            if (s.getRoundState() == RoundState.COMPLETE)
            {
                return action(ObjectiveType.ROUND_COMPLETE, "Round complete", "round_complete", TargetKind.NONE);
            }
            if (s.getSecondsToStart() >= 0 && s.getSecondsToStart() <= 15)
            {
                return action(ObjectiveType.POSITION, "Get ready at your brazier", "countdown_position", TargetKind.BRAZIER);
            }
            return action(ObjectiveType.WAIT, "Wait for the round", "countdown", TargetKind.NONE);
        }

        int goal = WintertodtGuidance.rewardTarget(settings.getPointGoal(), s.getPoints());
        boolean timed = s.getRoundBudgetSeconds() >= 0;
        boolean ending = finishing || (timed ? additionalRoots(s, settings, goal) == 0 : s.getEndUrgency() != EndUrgency.NORMAL);
        int usefulBatch = minimumBatch(s, settings, goal);
        if (s.getBrumaRoots() + s.getBrumaKindling() == 0 && finishComplete
            && ((!timed && settings.getStrategy() != WintertodtStrategy.MASS_XP)
                || (timed && additionalRoots(s, settings, goal) < usefulBatch)))
        {
            return action(ObjectiveType.WAIT, "Final fuel fed - wait for the next round", "finish_wait", TargetKind.NONE);
        }
        if (timed && s.getBrumaRoots() + s.getBrumaKindling() == 0
            && additionalRoots(s, settings, goal) < usefulBatch)
        {
            return action(ObjectiveType.WAIT, "Round ending - no time for another root", "finish_wait", TargetKind.NONE);
        }
        if ((!timed && s.getEndUrgency() == EndUrgency.IMMINENT || timed && s.getRoundBudgetSeconds() < 6) && s.getBrumaRoots() + s.getBrumaKindling() == 0
            && s.rewardThresholdReached())
        {
            return action(ObjectiveType.WAIT, "Round ending - rewards secured", "ending_secured", TargetKind.NONE);
        }
        // Cold does not stop chopping. Keep the resource batch ahead of distant maintenance.
        if ((s.getCurrentActivity() == PlayerActivity.CHOPPING
            || normal == ObjectiveType.CHOP && s.getBrumaRoots() > 0
                && s.getBrumaRoots() + s.getBrumaKindling() < batchTarget && s.getFreeSlots() > 0
                && s.getCurrentActivity() != PlayerActivity.FEEDING && s.getCurrentActivity() != PlayerActivity.FLETCHING) && !ending
            && !finishing && (!timed || additionalRoots(s, settings, goal) > 0))
        {
            return normal(s, settings, goal);
        }
        if (s.getLocalBrazierState() == BrazierState.BROKEN)
        {
            if (!s.isHasHammer())
            {
                return tool(TargetKind.HAMMER_CRATE, "missing_hammer");
            }
            return new WintertodtObjective(ObjectiveType.REPAIR, "Repair the brazier", "brazier_broken",
                TargetKind.BRAZIER, 0, true, ItemID.HAMMER);
        }
        if (s.isLocalPyromancerKnown() && s.isLocalPyromancerIncapacitated()
            && (s.getLocalBrazierState() == BrazierState.UNLIT || !ending))
        {
            if (s.getRejuvenationDoses() > 0)
            {
                return new WintertodtObjective(ObjectiveType.HEAL_PYROMANCER, "Heal the pyromancer",
                    "local_pyromancer_down", TargetKind.PYROMANCER, 1, false, s.getPotionItem());
            }
            // Do not make long potion trips while other mass-world players can heal near the end.
            if (!ending) { return potion(s, 1, "heal_supply"); }
            if (s.getLocalBrazierState() == BrazierState.UNLIT)
            {
                return action(ObjectiveType.WAIT, "Wait for the local pyromancer to be healed",
                    "ending_pyromancer_blocked", TargetKind.PYROMANCER);
            }
        }

        if (s.getLocalBrazierState() == BrazierState.UNKNOWN
            || s.getLocalBrazierState() == BrazierState.UNAVAILABLE)
        {
            return action(ObjectiveType.POSITION, "Move to your working brazier", "brazier_unobserved", TargetKind.BRAZIER);
        }
        if (s.getLocalBrazierState() == BrazierState.UNLIT)
        {
            if (!s.isLocalPyromancerKnown())
            {
                return action(ObjectiveType.POSITION, "Move closer to your pyromancer", "pyromancer_unobserved", TargetKind.BRAZIER);
            }
            if (!s.isHasTinderbox() && !s.isHasBrumaTorch())
            {
                return tool(TargetKind.TINDERBOX_CRATE, "missing_tinderbox");
            }
            return new WintertodtObjective(ObjectiveType.LIGHT, "Relight the brazier", "brazier_unlit",
                TargetKind.BRAZIER, 0, false, ItemID.TINDERBOX, ItemID.WINT_TORCH, ItemID.WINT_TORCH_OFFHAND);
        }
        WintertodtObjective preparation = ending && s.getLocalBrazierState() == BrazierState.LIT
            && (s.getBrumaRoots() + s.getBrumaKindling() > 0 || finishing || finishComplete) ? null : prepare(s, settings);
        if (preparation != null) { return preparation; }

        WintertodtObjective deadline = finish(s, settings, goal);
        if (deadline != null) { return deadline; }
        if (ending && !timed)
        {
            if (s.getBrumaKindling() + s.getBrumaRoots() > 0)
            {
                return feed(s, s.rewardThresholdReached() ? "round_ending" : "ending_secure_500", true);
            }
            if (s.getEndUrgency() == EndUrgency.IMMINENT)
            {
                return action(ObjectiveType.WAIT, "Round ending - no fuel remaining", "ending_no_fuel", TargetKind.NONE);
            }
        }
        return normal(s, settings, goal);
    }

    private WintertodtObjective normal(WintertodtSnapshot s, PlannerSettings settings, int goal)
    {
        int materials = s.getBrumaRoots() + s.getBrumaKindling();
        boolean fletch = settings.fletch(s) && (s.getRoundBudgetSeconds() >= 0 || s.getEndUrgency() == EndUrgency.NORMAL);
        if (normal == ObjectiveType.FLETCH && (!fletch || s.getBrumaRoots() == 0 || s.getBrumaKindling() >= kindlingTarget))
        {
            normal = ObjectiveType.FEED;
        }
        if (normal == ObjectiveType.FEED && materials == 0) { normal = null; }
        if (normal == ObjectiveType.CHOP && (materials >= batchTarget || s.getFreeSlots() == 0))
        {
            if (fletch && s.getBrumaRoots() > 0)
            {
                normal = ObjectiveType.FLETCH;
                int needed = upgrades(s, settings, goal);
                kindlingTarget = s.getBrumaKindling() + Math.min(s.getBrumaRoots(), needed);
                if (kindlingTarget == s.getBrumaKindling()) { normal = ObjectiveType.FEED; }
            }
            else { normal = materials > 0 ? ObjectiveType.FEED : null; }
        }
        if (normal == null)
        {
            int projected = s.getPoints() < 0 ? 0 : s.getPoints() + (fletch ? s.potentialPointsIfFletched() : s.inventoryPoints());
            if (materials > 0 && (projected >= goal || s.getFreeSlots() == 0 || s.getBrumaKindling() > 0))
            {
                int neededUpgrades = upgrades(s, settings, goal);
                if (fletch && s.getBrumaRoots() > 0 && neededUpgrades > 0)
                {
                    normal = ObjectiveType.FLETCH;
                    kindlingTarget = s.getBrumaKindling() + Math.min(s.getBrumaRoots(), neededUpgrades);
                }
                else { normal = ObjectiveType.FEED; }
            }
            else if (s.getFreeSlots() > 0)
            {
                normal = ObjectiveType.CHOP;
                int capacity = materials + s.getFreeSlots();
                int unitPoints = fletch ? 25 : 10;
                int need = s.getPoints() < 0 ? capacity : Math.max(1, ceil(goal - s.getPoints(), unitPoints));
                if (settings.getStrategy() == WintertodtStrategy.LOW_ATTENTION
                    || settings.getPointGoal() == PointGoal.MAXIMUM_PRACTICAL) { need = capacity; }
                int preferred = settings.getStrategy() == WintertodtStrategy.MASS_BALANCED ? 20 : capacity;
                if (s.getRoundBudgetSeconds() < 0 && s.getEndUrgency() == EndUrgency.ENDING_SOON) { preferred = Math.min(preferred, 3); }
                batchTarget = Math.min(capacity, Math.max(materials + 1, Math.min(need, preferred)));
            }
            else
            {
                return action(ObjectiveType.WAIT, "Make space for bruma roots", "inventory_blocked", TargetKind.NONE);
            }
        }
        if (normal == ObjectiveType.FLETCH)
        {
            int count = Math.min(s.getBrumaRoots(), Math.max(0, kindlingTarget - s.getBrumaKindling()));
            if (s.getRoundBudgetSeconds() >= 0)
            {
                count = Math.min(count, WintertodtFinishPlan.fletchCapacity(s));
                kindlingTarget = Math.min(kindlingTarget, s.getBrumaKindling() + count);
            }
            if (count == 0) { normal = ObjectiveType.FEED; return feed(s, "finish_feed", true); }
            return fletch(s, count, "batch_fletch", false);
        }
        if (normal == ObjectiveType.FEED) { return feed(s, "batch_feed", false); }
        if (!s.isHasAxe()) { return tool(TargetKind.AXE_CRATE, "missing_axe"); }
        int remaining = Math.max(1, batchTarget - materials);
        if (s.getRoundBudgetSeconds() >= 0)
        {
            // Limit today's instruction without permanently shrinking the resource batch.
            // A corrected forecast may leave room for more of the original batch next tick.
            remaining = Math.min(remaining, Math.max(1, additionalRoots(s, settings, goal)));
        }
        return new WintertodtObjective(ObjectiveType.CHOP, "Chop " + remaining + " bruma roots",
            "batch_chop", TargetKind.ROOTS, remaining, false);
    }

    private static int minimumBatch(WintertodtSnapshot s, PlannerSettings settings, int goal)
    {
        // XP mode values every root that fits; reward strategies can avoid tiny trips.
        if (settings.getStrategy() == WintertodtStrategy.MASS_XP) { return 1; }
        // A small trip can still be worthwhile if it secures a reward threshold.
        return s.rewardThresholdReached() ? Math.min(6, Math.max(1, ceil(goal - s.getPoints(), settings.fletch(s) ? 25 : 10))) : 1;
    }

    private static int additionalRoots(WintertodtSnapshot s, PlannerSettings settings, int goal)
    {
        boolean fletch = settings.fletch(s);
        int existingFletches = fletch ? Math.min(s.getBrumaRoots(), upgrades(s, settings, goal)) : 0;
        return WintertodtFinishPlan.additionalRoots(s, fletch, existingFletches);
    }

    private WintertodtObjective finish(WintertodtSnapshot s, PlannerSettings settings, int goal)
    {
        int materials = s.getBrumaRoots() + s.getBrumaKindling();
        if (materials == 0)
        {
            if (finishing) { finishComplete = true; }
            finishing = false;
            if (s.getRoundBudgetSeconds() >= 0 && additionalRoots(s, settings, goal) == 0)
            { return action(ObjectiveType.WAIT, "Round ending - no time for another root", "finish_wait", TargetKind.NONE); }
            if (finishComplete && ((s.getRoundBudgetSeconds() >= 0
                && additionalRoots(s, settings, goal) < minimumBatch(s, settings, goal))
                || (s.getRoundBudgetSeconds() < 0 && settings.getStrategy() != WintertodtStrategy.MASS_XP)))
            { return action(ObjectiveType.WAIT, "Final fuel fed - wait for the next round", "finish_wait", TargetKind.NONE); }
            finishComplete = false;
            return null;
        }
        boolean timed = s.getRoundBudgetSeconds() >= 0;
        boolean stopChopping = timed && additionalRoots(s, settings, goal) == 0;
        if (!finishing && !stopChopping) { return null; }
        if (!finishing)
        {
            finishing = true;
            int count = settings.fletch(s) ? Math.min(upgrades(s, settings, goal), WintertodtFinishPlan.fletchCapacity(s)) : 0;
            // Keep an existing smaller fletching target; do not restart a feeding batch.
            if (normal == ObjectiveType.FEED) { count = 0; }
            else if (normal == ObjectiveType.FLETCH) { count = Math.min(count, Math.max(0, kindlingTarget - s.getBrumaKindling())); }
            kindlingTarget = s.getBrumaKindling() + count;
            normal = count > 0 ? ObjectiveType.FLETCH : ObjectiveType.FEED;
        }
        int count = Math.min(s.getBrumaRoots(), Math.max(0, kindlingTarget - s.getBrumaKindling()));
        if (timed) { count = Math.min(count, WintertodtFinishPlan.fletchCapacity(s)); }
        else { count = 0; } // Lost forecast: preserve feeding time rather than gamble on fletching.
        kindlingTarget = Math.min(kindlingTarget, s.getBrumaKindling() + count);
        if (normal == ObjectiveType.FLETCH && count > 0) { return fletch(s, count, "finish_fletch", true); }
        normal = ObjectiveType.FEED;
        return feed(s, "finish_feed", true);
    }

    private WintertodtObjective fletch(WintertodtSnapshot s, int count, String reason, boolean urgent)
    {
        if (s.getBrazierDistance() != 0)
        {
            return new WintertodtObjective(ObjectiveType.POSITION, "Move beside the brazier to fletch",
                "fletch_at_brazier", TargetKind.BRAZIER, count, urgent);
        }
        return new WintertodtObjective(ObjectiveType.FLETCH,
            urgent ? "Fletch " + count + " roots, then feed" : s.isInterrupted() ? "Continue fletching" : "Fletch " + count + " roots",
            reason, TargetKind.NONE, count, urgent, ItemID.KNIFE, ItemID.FLETCHING_KNIFE, ItemID.WINT_BRUMA_ROOT);
    }

    private WintertodtObjective prepare(WintertodtSnapshot s, PlannerSettings settings)
    {
        TargetKind crate = TargetKind.NONE;
        if (!s.isHasAxe()) { crate = TargetKind.AXE_CRATE; }
        else if (!s.isHasHammer()) { crate = TargetKind.HAMMER_CRATE; }
        else if (!s.isHasKnife() && WintertodtGuidance.needsKnife(settings))
        { crate = TargetKind.KNIFE_CRATE; }
        else if (!s.isHasTinderbox() && !s.isHasBrumaTorch())
        { crate = TargetKind.TINDERBOX_CRATE; }
        if (crate != TargetKind.NONE)
        {
            if (s.getFreeSlots() == 0) { return makeSpace(s, "tools"); }
            return tool(crate, "prepare_tools");
        }
        // A partial reserve is enough to finish a short remaining window. Do not
        // send carried fuel on a late supply trip just to round doses up to a bottle.
        boolean shortWindow = s.getRoundBudgetSeconds() >= 0 && s.getRoundBudgetSeconds() <= 60
            || s.getEndUrgency() != EndUrgency.NORMAL;
        if (s.active() && shortWindow && (s.getRejuvenationDoses() > 0 || s.getBrumaRoots() + s.getBrumaKindling() > 0))
        {
            if (preparationDoseTarget == 0)
            { preparationDoseTarget = WintertodtSupplyPlan.recommendedPotions(s, settings.getRestoreWarmth()) * 4; }
            suppliesReady = true;
            return null;
        }
        // Do not replenish every consumed dose mid-round. Start another supply trip only
        // when empty; a round transition resets this latch in the plugin.
        if (suppliesReady && s.active() && s.getRejuvenationDoses() > 0) { return null; }
        if (preparationDoseTarget == 0 || suppliesReady)
        {
            preparationDoseTarget = WintertodtSupplyPlan.recommendedPotions(s, settings.getRestoreWarmth()) * 4;
            suppliesReady = false;
        }
        if (s.getRejuvenationDoses() >= preparationDoseTarget)
        {
            suppliesReady = true;
            return null;
        }
        int bottles = (preparationDoseTarget - s.getRejuvenationDoses() + 3) / 4;
        return potion(s, bottles, "prepare_supply");
    }

    private WintertodtObjective makeSpace(WintertodtSnapshot s, String resource)
    {
        if (s.active() && s.getLocalBrazierState() == BrazierState.LIT
            && s.getBrumaRoots() + s.getBrumaKindling() > 0)
        {
            return feed(s, "make_space_for_" + resource, false);
        }
        return action(ObjectiveType.WAIT, "Make space for " + resource, "supply_inventory_blocked", TargetKind.NONE);
    }

    private WintertodtObjective potion(WintertodtSnapshot s, int bottles, String reason)
    {
        if (s.getUnfinishedPotionCount() > 0 && s.getBrumaHerbCount() > 0)
        {
            int count = Math.min(bottles, Math.min(s.getBrumaHerbCount(), s.getUnfinishedPotionCount()));
            return new WintertodtObjective(ObjectiveType.MAKE_REJUVENATION,
                s.isCanMixPotion() ? "Mix " + count + " potion" + (count == 1 ? "" : "s")
                    : WintertodtGuidance.supplyInstruction(TargetKind.BREWMA, count),
                "mix_rejuvenation", s.isCanMixPotion() ? TargetKind.NONE : TargetKind.BREWMA,
                count, false,
                ItemID.WINT_HERB, ItemID.WINT_VIAL);
        }
        if (s.getFreeSlots() == 0) { return makeSpace(s, "potion ingredients"); }
        // Gather enough vials, then herbs, but leave room for at least one herb to mix.
        boolean takeVial = s.getUnfinishedPotionCount() < bottles
            && (s.getFreeSlots() > 1 || s.getUnfinishedPotionCount() == 0 || s.getBrumaHerbCount() > 0);
        int capacity = takeVial && s.getBrumaHerbCount() == 0 ? s.getFreeSlots() - 1 : s.getFreeSlots();
        if (capacity == 0) { return makeSpace(s, "potion ingredients"); }
        int remaining = Math.min(capacity, takeVial ? bottles - s.getUnfinishedPotionCount() : Math.max(1, bottles - s.getBrumaHerbCount()));
        return new WintertodtObjective(ObjectiveType.MAKE_REJUVENATION,
            WintertodtGuidance.supplyInstruction(takeVial ? TargetKind.POTION_CRATE : TargetKind.HERBS, remaining),
            reason, takeVial ? TargetKind.POTION_CRATE : TargetKind.HERBS,
            Math.min(remaining, s.getFreeSlots()), false);
    }

    private WintertodtObjective feed(WintertodtSnapshot s, String reason, boolean urgent)
    {
        return new WintertodtObjective(ObjectiveType.FEED,
            "finish_feed".equals(reason) ? "Continue feeding brazier"
                : urgent && !s.rewardThresholdReached() ? "Feed now - secure 500 points"
                : s.isInterrupted() ? "Continue feeding" : "Feed the brazier",
            reason, TargetKind.BRAZIER, s.getBrumaRoots() + s.getBrumaKindling(), urgent,
            s.getBrumaKindling() > 0 ? ItemID.WINT_BRUMA_KINDLING : ItemID.WINT_BRUMA_ROOT);
    }
    private WintertodtObjective action(ObjectiveType type, String text, String reason, TargetKind target)
    {
        return new WintertodtObjective(type, text, reason, target, 0, false);
    }
    private WintertodtObjective tool(TargetKind target, String reason)
    {
        return new WintertodtObjective(ObjectiveType.GET_TOOL, WintertodtGuidance.supplyInstruction(target, 1),
            reason, target, 1, false);
    }
    private static int upgrades(WintertodtSnapshot s, PlannerSettings settings, int goal)
    {
        if (s.getPoints() < 0 || settings.getFletchMode() == FletchMode.ALWAYS
            || settings.getPointGoal() == PointGoal.MAXIMUM_PRACTICAL) { return s.getBrumaRoots(); }
        return Math.max(0, ceil(goal - s.getPoints() - s.inventoryPoints(),
            WintertodtConstants.KINDLING_POINTS - WintertodtConstants.ROOT_POINTS));
    }
    private static int ceil(int n, int d) { return (Math.max(0, n) + d - 1) / d; }
}
