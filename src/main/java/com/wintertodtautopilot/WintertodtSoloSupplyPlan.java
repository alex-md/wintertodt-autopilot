package com.wintertodtautopilot;

import net.runelite.api.gameval.ItemID;

/** A replenished working reserve, never a promise to cover an unlimited Solo session. */
final class WintertodtSoloSupplyPlan
{
    private int target;
    private boolean restocking = true;
    private boolean mixing;
    // Engineering horizons, not an estimate of the remaining round or a survival guarantee.
    private int lastTick = -1, observedTicks, personalUsed, healingUsed;
    private int windowPersonal, windowHealing;
    private int personalRateTarget, healingRateTarget, personalFloor = 2;
    private int lastFiremaking = -1, lastWarmItems = -1;
    private SoloEffort lastEffort;
    private WintertodtStrategy lastStrategy;
    void suspend() { lastTick = -1; observedTicks = 0; windowPersonal = 0; windowHealing = 0; }
    void observe(WintertodtSnapshot s, PlannerSettings settings)
    {
        if (s.getFiremakingLevel() != lastFiremaking || s.getWarmClothingCount() != lastWarmItems
            || settings.getSoloEffort() != lastEffort || settings.getStrategy() != lastStrategy)
        {
            suspend(); personalRateTarget = 0; healingRateTarget = 0; personalFloor = 2;
            lastFiremaking = s.getFiremakingLevel(); lastWarmItems = s.getWarmClothingCount();
            lastEffort = settings.getSoloEffort(); lastStrategy = settings.getStrategy();
        }
        int tick = s.getObservationTick();
        if (tick == lastTick) { return; }
        if (!s.active() || !s.isInventoryKnown() || !s.isEquipmentKnown() || s.getWarmth() < 0
            || lastTick < 0 || tick != lastTick + 1)
        { suspend(); lastTick = s.active() && s.isInventoryKnown() && s.isEquipmentKnown() && s.getWarmth() >= 0 ? tick : -1; return; }
        lastTick = tick; observedTicks++;
        int personal = Math.max(0, Math.min(1, s.getPersonalDosesUsed()));
        int healing = Math.max(0, Math.min(1, s.getHealingDosesUsed()));
        personalUsed += personal; healingUsed += healing;
        windowPersonal += personal; windowHealing += healing;
        // Require three minutes of consecutive valid samples and three attributed uses.
        // Unknown losses, missing frames, food and regeneration cannot shrink the baseline.
        if (observedTicks >= 300 && windowPersonal + windowHealing >= 3)
        {
            personalRateTarget = Math.max(personalRateTarget, ceiling(windowPersonal * 500, observedTicks) + 4);
            healingRateTarget = Math.max(healingRateTarget, ceiling(windowHealing * 500, observedTicks) + 2);
            personalFloor = Math.max(personalFloor, Math.min(8, Math.max(2, ceiling(windowPersonal * 100, observedTicks) + 2)));
        }
        if (observedTicks >= 500) { suspend(); lastTick = tick; }
    }
    private static int ceiling(int n, int d) { return (n + d - 1) / d; }
    int personalMinimum() { return personalFloor; }
    private int personalAllowance()
    { return Math.min(40, Math.max(activeHealing() ? 16 : 24, personalRateTarget)); }
    private boolean activeHealing() { return lastEffort == SoloEffort.ACTIVE && lastStrategy != WintertodtStrategy.MASS_XP; }
    private int healingAllowance() { return activeHealing() ? Math.min(40 - personalAllowance(), Math.max(8, healingRateTarget)) : 0; }
    private int reserve(PlannerSettings settings, SoloPhase phase)
    {
        if (phase == SoloPhase.FINISH) { return Math.min(16, Math.max(8, personalFloor + 4)); }
        boolean healing = settings.getSoloEffort() == SoloEffort.ACTIVE && settings.getStrategy() != WintertodtStrategy.MASS_XP;
        int personal = Math.min(40, Math.max(healing ? 16 : 24, personalRateTarget));
        return Math.min(40, personal + (healing ? Math.max(8, healingRateTarget) : 0));
    }
    String detail()
    {
        return "Working allowance: " + personalAllowance() + " warmth + " + healingAllowance() + " healing. "
            + "Keep " + personalMinimum() + "+ personal; observed " + personalUsed + " personal / " + healingUsed + " healing";
    }
    String readiness(WintertodtSnapshot s, WintertodtObjective o)
    {
        if (!s.isInventoryKnown() || !s.isEquipmentKnown()) { return "Waiting for inventory and equipment"; }
        if (o.getType() == ObjectiveType.RESTORE_WARMTH) { return "Restore warmth first"; }
        if ("prepare_tools".equals(o.getReason())) { return "Missing tool"; }
        if ("supply_inventory_blocked".equals(o.getReason()) || "solo_recovery_space".equals(o.getReason())) { return "Make room for supplies"; }
        if (target == 0) { return "Checking encounter and supplies"; }
        if (restocking) { return "Preparing potions: " + s.getRejuvenationDoses() + " / " + target + " doses"; }
        return "Supplies ready: " + s.getRejuvenationDoses() + " doses"
            + (s.getWarmClothingCount() < 0 ? "; clothing unknown" : s.getWarmClothingCount() < 4 ? "; " + s.getWarmClothingCount() + "/4 warm items" : "");
    }
    int getTarget() { return target; }
    void reset()
    {
        target = 0; restocking = true; mixing = false; suspend(); personalUsed = 0; healingUsed = 0;
        personalRateTarget = 0; healingRateTarget = 0; personalFloor = 2;
        lastFiremaking = -1; lastWarmItems = -1; lastEffort = null; lastStrategy = null;
    }
    /** Inventory survives encounter boundaries, including an unfinished preparation batch. */
    void nextRound() { suspend(); if (!restocking && target > 0) { target = 24; } }

    WintertodtObjective prepare(WintertodtSnapshot s, PlannerSettings settings, SoloPhase phase)
    {
        TargetKind tool = phase != SoloPhase.FINISH && !s.isHasAxe() ? TargetKind.AXE_CRATE : !s.isHasHammer() ? TargetKind.HAMMER_CRATE
            : phase != SoloPhase.FINISH && WintertodtGuidance.needsKnife(settings) && !s.isHasKnife() ? TargetKind.KNIFE_CRATE
            : !s.isHasTinderbox() && !s.isHasBrumaTorch() ? TargetKind.TINDERBOX_CRATE : TargetKind.NONE;
        if (tool != TargetKind.NONE)
        {
            if (s.getFreeSlots() == 0) { return blocked("tools"); }
            return new WintertodtObjective(ObjectiveType.GET_TOOL, WintertodtGuidance.supplyInstruction(tool, 1),
                "prepare_tools", tool, 1, false);
        }
        int reserve = reserve(settings, phase);
        int restockAt = phase != SoloPhase.FINISH && settings.getStrategy() != WintertodtStrategy.MASS_XP ? 8 : 4;
        restockAt = Math.max(restockAt, Math.min(12, personalFloor + 2));
        boolean carryingFuel = s.getBrumaRoots() + s.getBrumaKindling() > 0;
        boolean finishFuelFirst = s.active() && carryingFuel && s.getRejuvenationDoses() > 1;
        if (target == 0)
        {
            target = reserve;
            // Enabling mid-round with usable supplies must not force an opening
            // top-up merely because the reserve is below six full bottles.
            if (s.active() && (s.getRejuvenationDoses() >= restockAt || finishFuelFirst))
            { restocking = false; }
        }
        if (!restocking && s.getRejuvenationDoses() < restockAt)
        {
            // Defer a new trip until the fuel batch is fed. Once ingredients are
            // requested, complete that trip; the last personal dose cannot wait.
            if (finishFuelFirst) { return null; }
            restocking = true; target = reserve;
        }
        if (phase == SoloPhase.FINISH) { target = Math.min(target, reserve); }
        else if (!restocking) { target = reserve; }
        if (s.getRejuvenationDoses() >= target) { restocking = false; mixing = false; }
        if (!restocking) { return null; }
        return ingredients(s, Math.max(1, (target - s.getRejuvenationDoses() + 3) / 4));
    }
    private WintertodtObjective ingredients(WintertodtSnapshot s, int bottles)
    {
        int vials = s.getUnfinishedPotionCount(), herbs = s.getBrumaHerbCount(), free = s.getFreeSlots();
        int pairs = Math.min(vials, herbs);
        // Collect a complete feasible batch. Existing ingredients occupy slots;
        // a new pair needs two slots until mixing turns it into one bottle.
        int batch = Math.min(bottles, Math.min((vials + herbs + free) / 2,
            Math.min(vials + free, herbs + free)));
        if (pairs > 0 && (mixing || batch > 0 && vials >= batch && herbs >= batch))
        {
            mixing = true;
            int count = Math.min(bottles, pairs);
            return new WintertodtObjective(ObjectiveType.MAKE_REJUVENATION,
                s.isCanMixPotion() ? "Mix " + count + " potion" + (count == 1 ? "" : "s")
                    : WintertodtGuidance.supplyInstruction(TargetKind.BREWMA, count),
                "mix_rejuvenation", s.isCanMixPotion() ? TargetKind.NONE : TargetKind.BREWMA,
                count, false, ItemID.WINT_HERB, ItemID.WINT_VIAL);
        }
        mixing = false;
        if (batch <= 0) { return blocked("potion ingredients"); }
        boolean vial = vials < batch;
        int count = vial ? batch - vials : batch - herbs;
        TargetKind target = vial ? TargetKind.POTION_CRATE : TargetKind.HERBS;
        return new WintertodtObjective(ObjectiveType.MAKE_REJUVENATION,
            WintertodtGuidance.supplyInstruction(target, count), "solo_restock", target, count, false);
    }
    private WintertodtObjective blocked(String resource)
    {
        return new WintertodtObjective(ObjectiveType.WAIT, "Make space for " + resource,
            "supply_inventory_blocked", TargetKind.NONE, 0, false);
    }
}
