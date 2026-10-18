package com.wintertodtautopilot;

import net.runelite.api.gameval.ItemID;

/** A replenished working reserve, never a promise to cover an unlimited Solo session. */
final class WintertodtSoloSupplyPlan
{
    private int target;
    private boolean restocking = true;
    int getTarget() { return target; }
    void reset() { target = 0; restocking = true; }

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
        int reserve = phase == SoloPhase.FINISH ? 8 : 24;
        int restockAt = phase != SoloPhase.FINISH && settings.getStrategy() != WintertodtStrategy.MASS_XP ? 8 : 4;
        if (!restocking && s.getRejuvenationDoses() < restockAt) { restocking = true; target = reserve; }
        if (target == 0) { target = reserve; }
        if (phase == SoloPhase.FINISH) { target = Math.min(target, reserve); }
        if (s.getRejuvenationDoses() >= target) { restocking = false; }
        if (!restocking) { return null; }
        return ingredients(s, Math.max(1, (target - s.getRejuvenationDoses() + 3) / 4));
    }
    private WintertodtObjective ingredients(WintertodtSnapshot s, int bottles)
    {
        if (s.getUnfinishedPotionCount() > 0 && s.getBrumaHerbCount() > 0)
        {
            int count = Math.min(bottles, Math.min(s.getUnfinishedPotionCount(), s.getBrumaHerbCount()));
            return new WintertodtObjective(ObjectiveType.MAKE_REJUVENATION,
                s.isCanMixPotion() ? "Mix " + count + " potion" + (count == 1 ? "" : "s")
                    : WintertodtGuidance.supplyInstruction(TargetKind.BREWMA, count),
                "mix_rejuvenation", s.isCanMixPotion() ? TargetKind.NONE : TargetKind.BREWMA,
                count, false, ItemID.WINT_HERB, ItemID.WINT_VIAL);
        }
        if (s.getFreeSlots() == 0) { return blocked("potion ingredients"); }
        boolean vial = s.getUnfinishedPotionCount() < bottles
            && (s.getFreeSlots() > 1 || s.getUnfinishedPotionCount() == 0 || s.getBrumaHerbCount() > 0);
        int capacity = vial && s.getBrumaHerbCount() == 0 ? s.getFreeSlots() - 1 : s.getFreeSlots();
        if (capacity <= 0) { return blocked("potion ingredients"); }
        int count = Math.min(capacity, vial ? bottles - s.getUnfinishedPotionCount() : Math.max(1, bottles - s.getBrumaHerbCount()));
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
