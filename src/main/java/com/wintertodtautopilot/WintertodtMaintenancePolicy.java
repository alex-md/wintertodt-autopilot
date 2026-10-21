package com.wintertodtautopilot;

/** One eligibility rule for every Solo action that can restore drain. No timing prediction. */
final class WintertodtMaintenancePolicy
{
    enum Verdict { ALLOW, RECOVERY, COVERAGE, REMOTE_FIRE, UNOBSERVED }
    static Verdict evaluate(WintertodtSnapshot s, WorkingCorner destination, ObjectiveType action,
        boolean finishing, boolean recovering, boolean multipleFires, WorkingCorner home)
    {
        CornerObservation c = s.corner(destination);
        int tick = s.getObservationTick();
        if (c == null || !c.brazierFresh(tick) || c.getBrazierState() == BrazierState.UNKNOWN
            || c.getBrazierState() == BrazierState.UNAVAILABLE || !c.isPyromancerVisible()
            || !c.pyromancerFresh(tick) || c.getPyromancerState() == CornerObservation.PyromancerState.UNKNOWN)
        { return Verdict.UNOBSERVED; }
        boolean recovery = !finishing && (recovering || s.getWintertodtEnergy() <= WintertodtSoloEnergyModel.RECOVERY_TRIGGER);
        boolean inactiveFire = c.getBrazierState() == BrazierState.UNLIT || c.getBrazierState() == BrazierState.BROKEN;
        // Healing at a known extinguished fire cannot restart drain. During recovery
        // even standing-pyromancer healing requires this evidence.
        if (action == ObjectiveType.HEAL_PYROMANCER && inactiveFire) { return Verdict.ALLOW; }
        if (action == ObjectiveType.HEAL_PYROMANCER && c.getPyromancerState() == CornerObservation.PyromancerState.ALIVE && !recovery)
        { return Verdict.ALLOW; }
        if (recovery) { return Verdict.RECOVERY; }
        if (s.getWintertodtEnergy() < 1 || s.getWintertodtEnergy() > 100 || s.observedGlobalLitBraziers() < 0)
        { return Verdict.COVERAGE; }
        if (!multipleFires)
        {
            for (CornerObservation other : s.getCorners())
            {
                if (other.getCorner() != home && other.getBrazierState() == BrazierState.LIT)
                { return Verdict.REMOTE_FIRE; }
            }
        }
        return Verdict.ALLOW;
    }
}
