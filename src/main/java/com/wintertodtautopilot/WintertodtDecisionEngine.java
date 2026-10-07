package com.wintertodtautopilot;

import net.runelite.api.gameval.ItemID;

/** Deterministic tick planner. Emergency objectives never overwrite the normal batch. */
public final class WintertodtDecisionEngine
{
    private ObjectiveType normal;
    private int batchTarget;
    private int kindlingTarget;
    private WorkingCorner corner;
    private PlannerSettings previousSettings;
    private boolean suppliesReady;
    private int preparationDoseTarget;

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
        }
        if (!s.active())
        {
            normal = null;
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

        if (s.getWarmth() >= 0 && s.getWarmth() <= settings.getRestoreWarmth()
            && s.getCurrentActivity() != PlayerActivity.RESTORING_WARMTH)
        {
            boolean emergency = s.getWarmth() <= settings.getEmergencyWarmth();
            if (s.getWarmthItem() >= 0)
            {
                return new WintertodtObjective(ObjectiveType.RESTORE_WARMTH,
                    emergency ? "LOW WARMTH - restore now" : "Restore warmth",
                    "warmth_low", TargetKind.NONE, 1, emergency, s.getWarmthItem());
            }
            // Never keep skilling or propose a long supply trip without a usable restorative.
            return new WintertodtObjective(ObjectiveType.RESTORE_WARMTH,
                "Move to the safe lobby - no warmth supplies", "no_warmth_supplies",
                TargetKind.DOOR, 0, true);
        }

        int goal = settings.getPointGoal().target(s.getPoints());
        if (s.getPoints() >= goal)
        {
            // Goals secure rewards, they do not end productive mass-world play.
            goal = PointGoal.NEXT_THRESHOLD.target(s.getPoints());
        }
        boolean ending = s.getEndUrgency() != EndUrgency.NORMAL;
        if (s.getEndUrgency() == EndUrgency.IMMINENT && s.getBrumaRoots() + s.getBrumaKindling() == 0
            && s.rewardThresholdReached())
        {
            return action(ObjectiveType.WAIT, "Round ending - rewards secured", "ending_secured", TargetKind.NONE);
        }
        // Cold does not stop chopping. Keep the resource batch ahead of distant maintenance.
        if (s.getCurrentActivity() == PlayerActivity.CHOPPING && !ending)
        {
            return normal(s, settings, goal);
        }
        if (s.getLocalBrazierState() == BrazierState.BROKEN)
        {
            if (!s.isHasHammer())
            {
                return tool("Get a hammer", TargetKind.HAMMER_CRATE, "missing_hammer");
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
        WintertodtObjective preparation = ending && s.getLocalBrazierState() == BrazierState.LIT
            && s.getBrumaRoots() + s.getBrumaKindling() > 0 ? null : prepare(s, settings);
        if (preparation != null) { return preparation; }

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
                return tool("Get a tinderbox", TargetKind.TINDERBOX_CRATE, "missing_tinderbox");
            }
            return new WintertodtObjective(ObjectiveType.LIGHT, "Relight the brazier", "brazier_unlit",
                TargetKind.BRAZIER, 0, false, ItemID.TINDERBOX, ItemID.WINT_TORCH, ItemID.WINT_TORCH_OFFHAND);
        }
        if (ending)
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
        boolean fletch = settings.fletch(s) && s.getEndUrgency() == EndUrgency.NORMAL;
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
                if (s.getEndUrgency() == EndUrgency.ENDING_SOON) { preferred = Math.min(preferred, 3); }
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
            return new WintertodtObjective(ObjectiveType.FLETCH,
                (s.isInterrupted() ? "Continue fletching" : "Fletch " + count + " roots"),
                "batch_fletch", TargetKind.NONE, count, false, ItemID.KNIFE, ItemID.WINT_BRUMA_ROOT);
        }
        if (normal == ObjectiveType.FEED) { return feed(s, "batch_feed", false); }
        if (!s.isHasAxe()) { return tool("Get an axe", TargetKind.AXE_CRATE, "missing_axe"); }
        int remaining = Math.max(1, batchTarget - materials);
        return new WintertodtObjective(ObjectiveType.CHOP, "Chop " + remaining + " bruma roots",
            "batch_chop", TargetKind.ROOTS, remaining, false);
    }

    private WintertodtObjective prepare(WintertodtSnapshot s, PlannerSettings settings)
    {
        String toolName = null;
        TargetKind crate = TargetKind.NONE;
        if (!s.isHasAxe()) { toolName = "an axe"; crate = TargetKind.AXE_CRATE; }
        else if (!s.isHasHammer()) { toolName = "a hammer"; crate = TargetKind.HAMMER_CRATE; }
        else if (!s.isHasKnife() && settings.getFletchMode() != FletchMode.NEVER)
        { toolName = "a knife"; crate = TargetKind.KNIFE_CRATE; }
        else if (!s.isHasTinderbox() && !s.isHasBrumaTorch())
        { toolName = "a tinderbox"; crate = TargetKind.TINDERBOX_CRATE; }
        if (toolName != null)
        {
            if (s.getFreeSlots() == 0) { return makeSpace(s, "tools"); }
            return tool("Get " + toolName + " before starting", crate, "prepare_tools");
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
            return new WintertodtObjective(ObjectiveType.MAKE_REJUVENATION,
                s.isCanMixPotion() ? "Combine herb and unfinished potion" : "Ask Brew'ma to mix your potion",
                "mix_rejuvenation", s.isCanMixPotion() ? TargetKind.NONE : TargetKind.BREWMA,
                Math.min(bottles, Math.min(s.getBrumaHerbCount(), s.getUnfinishedPotionCount())), false,
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
            takeVial ? "Take " + remaining + " unfinished rejuvenation potion" + (remaining == 1 ? "" : "s")
                : "Pick " + remaining + " bruma herb" + (remaining == 1 ? "" : "s"),
            reason, takeVial ? TargetKind.POTION_CRATE : TargetKind.HERBS,
            Math.min(remaining, s.getFreeSlots()), false);
    }

    private WintertodtObjective feed(WintertodtSnapshot s, String reason, boolean urgent)
    {
        return new WintertodtObjective(ObjectiveType.FEED,
            urgent && !s.rewardThresholdReached() ? "Feed now - secure 500 points"
                : s.isInterrupted() ? "Continue feeding" : "Feed the brazier",
            reason, TargetKind.BRAZIER, s.getBrumaRoots() + s.getBrumaKindling(), urgent,
            s.getBrumaKindling() > 0 ? ItemID.WINT_BRUMA_KINDLING : ItemID.WINT_BRUMA_ROOT);
    }
    private WintertodtObjective action(ObjectiveType type, String text, String reason, TargetKind target)
    {
        return new WintertodtObjective(type, text, reason, target, 0, false);
    }
    private WintertodtObjective tool(String text, TargetKind target, String reason)
    {
        return action(ObjectiveType.GET_TOOL, text, reason, target);
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
