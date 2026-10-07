package com.wintertodtautopilot;

/** A conservative mass-round reserve, not a prediction of every random attack. */
public final class WintertodtSupplyPlan
{
    private WintertodtSupplyPlan() { }
    public static int recommendedPotions(WintertodtSnapshot s, int restoreAt)
    {
        int fm = Math.max(50, Math.min(99, s.getFiremakingLevel()));
        int warm = s.getWarmClothingCount() < 0 ? 2 : Math.min(4, s.getWarmClothingCount());
        // Use three lit braziers for planning a mass round, not the currently unlit start state.
        int coldDamage = Math.max(1, (16 - warm - 6) * 100 / fm);
        double remaining = s.active() && s.getWintertodtEnergy() >= 0 ? s.getWintertodtEnergy() / 100.0 : 1;
        // About 20 ordinary hits over a typical mass round; likelihood falls with energy.
        double ordinary = 20 * remaining * remaining * coldDamage;
        double specials = 2 * coldDamage * remaining;
        double regeneration = (8 + warm) * 4 * remaining;
        int warmth = s.active() && s.getWarmth() >= 0 ? s.getWarmth() : 100;
        double available = Math.max(0, warmth - restoreAt) + s.getFoodCount() * 35;
        int drinkingDoses = (int) Math.ceil(Math.max(0, ordinary + specials - regeneration - available) / 30);
        int doses = drinkingDoses + 1; // One spare dose for the local pyromancer.
        return Math.max(1, Math.min(3, (doses + 3) / 4));
    }
}
