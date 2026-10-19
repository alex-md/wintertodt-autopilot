package com.wintertodtautopilot;

/** Shared, player-facing explanations. No client access or speculative next steps. */
public final class WintertodtGuidance
{
    private WintertodtGuidance() { }

    public static int rewardTarget(PointGoal goal, int points)
    {
        int target = goal.target(points);
        return points >= target ? PointGoal.NEXT_THRESHOLD.target(points) : target;
    }

    public static boolean needsKnife(PlannerSettings settings)
    {
        return settings.getFletchMode() == FletchMode.ALWAYS
            || settings.getFletchMode() == FletchMode.AUTOMATIC
            && settings.getStrategy() != WintertodtStrategy.MASS_XP;
    }

    public static boolean isSupplyTarget(WintertodtObjective o)
    {
        return o != null && (o.getType() == ObjectiveType.GET_TOOL
            || o.getType() == ObjectiveType.MAKE_REJUVENATION && o.getTarget() != TargetKind.NONE);
    }

    public static String supplyInstruction(TargetKind target, int quantity)
    {
        int count = Math.max(1, quantity);
        switch (target)
        {
            case AXE_CRATE: return "Take 1 axe";
            case HAMMER_CRATE: return "Take 1 hammer";
            case KNIFE_CRATE: return "Take 1 knife";
            case TINDERBOX_CRATE: return "Take 1 tinderbox";
            case POTION_CRATE: return "Take " + count + " unfinished potion" + (count == 1 ? "" : "s");
            case HERBS: return "Pick " + count + " bruma herb" + (count == 1 ? "" : "s");
            case BREWMA: return "Mix " + count + " potion" + (count == 1 ? "" : "s") + " with Brew'ma";
            default: return null;
        }
    }

    public static String phase(WintertodtSnapshot s)
    {
        switch (s.getRoundState())
        {
            case OUTSIDE: return "Ready when you are";
            case LOBBY: return "Preparation area";
            case COUNTDOWN: return s.getSecondsToStart() > 0 ? "Starts in " + s.getSecondsToStart() + "s" : "Waiting for next round";
            case COMPLETE: return "Round complete";
            case ENDING: return "Round ending";
            default: return "Round in progress";
        }
    }

    /** Acknowledges an observed action so the player does not keep clicking it. */
    public static boolean performing(WintertodtSnapshot s, WintertodtObjective o)
    {
        if (o == null) { return false; }
        switch (o.getType())
        {
            case CHOP: return s.getCurrentActivity() == PlayerActivity.CHOPPING;
            case FLETCH: return s.getCurrentActivity() == PlayerActivity.FLETCHING;
            case FEED: return s.getCurrentActivity() == PlayerActivity.FEEDING;
            case LIGHT: return s.getCurrentActivity() == PlayerActivity.LIGHTING;
            case REPAIR: return s.getCurrentActivity() == PlayerActivity.REPAIRING;
            case HEAL_PYROMANCER: return s.getCurrentActivity() == PlayerActivity.HEALING;
            case RESTORE_WARMTH: return s.getCurrentActivity() == PlayerActivity.RESTORING_WARMTH;
            case MAKE_REJUVENATION: return s.getCurrentActivity() == PlayerActivity.MAKING_POTION
                && "mix_rejuvenation".equals(o.getReason());
            default: return false;
        }
    }

    public static String status(WintertodtSnapshot s, WintertodtObjective o)
    {
        if ("inventory_blocked".equals(o.getReason()) || "supply_inventory_blocked".equals(o.getReason()))
        { return "ACTION NEEDED"; }
        if (performing(s, o)) { return "IN PROGRESS"; }
        if (WintertodtHighlight.needsRestart(s, o)) { return "RESUME"; }
        if (o.isUrgent()) { return "ACT NOW"; }
        return o.actionable() ? "NEXT ACTION" : "STAND BY";
    }

    public static String reassurance(WintertodtSnapshot s, WintertodtObjective o)
    {
        if (o.getReason().startsWith("solo_recovery"))
        { return "Leave braziers unlit. Recovery can take time; watch the energy trend."; }
        if ("solo_coverage_unknown".equals(o.getReason()) || "solo_signals_unknown".equals(o.getReason()))
        { return "Guidance needs fresh observations before recommending a relight."; }
        if ("solo_restock".equals(o.getReason()))
        { return "Refill the potion reserve for warmth and pyromancer healing."; }
        if (o.getType() == ObjectiveType.HEAL_PYROMANCER)
        { return "Click the highlighted pyromancer to heal them."; }
        if ("solo_remote_lit".equals(o.getReason())) { return "A remote brazier is still lit. Avoid adding drain at home."; }
        if ("solo_rapid_drain".equals(o.getReason())) { return "Energy is dropping quickly. Feed carried fuel while checking the arena."; }
        if (performing(s, o)) { return "Keep going. Guidance updates when you need to act."; }
        if ("finish_feed".equals(o.getReason()) || "round_ending".equals(o.getReason()))
        { return "Feed your fuel before the round ends."; }
        if (o.getType() == ObjectiveType.WAIT || o.getType() == ObjectiveType.ROUND_COMPLETE)
        { return "inventory_blocked".equals(o.getReason()) || "supply_inventory_blocked".equals(o.getReason())
            ? "Free an inventory slot to continue." : "Guidance updates automatically. Watch for the next action."; }
        if (o.getType() == ObjectiveType.FLETCH) { return "Use your knife on the highlighted roots."; }
        if (o.getType() == ObjectiveType.RESTORE_WARMTH) { return "Restore warmth before continuing."; }
        if ("mix_rejuvenation".equals(o.getReason()))
        { return o.getTarget() == TargetKind.BREWMA ? "Brew'ma can mix the ingredients for you." : "Use a bruma herb on an unfinished potion."; }
        return o.getTarget() == TargetKind.NONE ? "Follow the highlighted inventory items." : "Click the highlighted target.";
    }

    public static String explanation(WintertodtObjective o)
    {
        if (o == null) { return "Visit Wintertodt on a mass world. Your next action and supply checks appear here automatically."; }
        String reason = o.getReason();
        switch (reason)
        {
            case "solo_restock": return "Replenish the Solo working reserve for warmth and pyromancer healing. Further restocks may be needed.";
            case "solo_light": return "Current energy and all four brazier observations permit lighting this corner.";
            case "solo_repair": return "Repair this destination before lighting. Energy is checked again on the next tick.";
            case "solo_points_heal": return "In Points mode, heal observed damage for points. While fletching, nearby injured pyromancers are useful healing opportunities.";
            case "solo_fletch_inside": return "Fletching is allowed at the roots and while running, but not in the safe preparation area.";
            case "solo_heal": return "Heal this corner's incapacitated pyromancer. Keep a spare dose for personal warmth.";
            case "solo_heal_reserve": return "Prepare more potions before spending your last personal warmth dose.";
            case "solo_goal_feed": return "Carried points could meet your goal. Feed them first; only earned points trigger finishing.";
            case "solo_finish_feed": return "Finishing is active. Feed carried fuel before lighting additional braziers.";
            case "solo_finish_drain": return "Maintain the selected finish pair, or one nearby brazier when run energy is low.";
            case "solo_batch_chop": return "Gather this Solo batch. The mass finish-time estimate does not control Solo gathering.";
            case "solo_batch_fletch": return "Convert the current roots to kindling before feeding them.";
            case "solo_batch_feed": return "Feed your batch to earn its points, then gather toward the fixed Solo goal.";
            case "solo_observe_corner": return "Move to this destination and acquire the scene object before maintenance guidance.";
            case "solo_observe_pyromancer": return "The destination's pyromancer must be directly observed before lighting.";
            case "warmth_low": return "Warmth has reached your restore threshold. Use the highlighted food or potion before continuing.";
            case "no_warmth_supplies": return "You have no detected usable food or potion. Reach the safe lobby before gathering more supplies.";
            case "prepare_tools": return "Take the missing tool from the highlighted crate. Equipped axes and bruma torches count too.";
            case "missing_axe": return "An axe is required to chop bruma roots. Take one from the crate or equip your own.";
            case "missing_hammer": return "A hammer is needed to repair this brazier.";
            case "missing_tinderbox": return "Use a tinderbox or bruma torch to relight the brazier.";
            case "supply_inventory_blocked": return "Free an inventory slot, then follow the highlighted supply step.";
            case "inventory_blocked": return "Your inventory is full and contains no usable bruma fuel. Free a slot to continue.";
            case "mix_rejuvenation": return o.getTarget() == TargetKind.BREWMA
                ? "Brew'ma can mix your bruma herb and unfinished potion without Druidic Ritual."
                : "Use a bruma herb on an unfinished rejuvenation potion. Each finished bottle provides four doses.";
            case "prepare_supply": return "Gather a reserve for warmth and pyromancer healing. This is a mass-round estimate; food and warm clothing reduce it.";
            case "heal_supply": return "Your local pyromancer needs a potion dose. Gather ingredients to help them.";
            case "brazier_broken": return "Repair your working brazier before feeding or relighting it.";
            case "local_pyromancer_down": return "Click Heal on the highlighted pyromancer. This uses one rejuvenation potion dose.";
            case "brazier_unlit": return "Your pyromancer is healthy. Relight the brazier before feeding fuel.";
            case "brazier_unobserved": return "The local brazier is not currently observed. Move closer so guidance can check its state.";
            case "pyromancer_unobserved": return "Move closer to check the pyromancer before trying to relight.";
            case "ending_pyromancer_blocked": return "The brazier cannot be relit until its pyromancer is healed. Avoid a long supply trip this late in the round.";
            case "fletch_at_brazier": return "Move to the highlighted tile beside your working brazier, then use the knife on your roots. Stay close for repairs, relighting and feeding.";
            case "finish_fletch": return "The observed round pace leaves time for this smaller fletching batch after reserving walking and feeding time. Feed as soon as it is ready.";
            case "finish_feed": return "The remaining time is needed for walking and feeding. Stop chopping or fletching and use your carried fuel now.";
            case "finish_wait": return "The time budget is too short to chop another root and feed it. Wait for the next round.";
            case "batch_chop": return "Gather the remaining roots for this batch. Low warmth and the end of the round can interrupt it.";
            case "batch_fletch": return "Use your knife on bruma roots. Kindling earns 25 points per feed; a root earns 10.";
            case "batch_feed": return "Feed your carried fuel to secure its points. Inventory projections only count once the fuel is fed.";
            case "ending_secure_500": return "The round is ending. Feed existing fuel now to work toward the 500-point reward minimum.";
            case "round_ending": return "Feed existing fuel before the round ends; there may not be time to finish fletching.";
            case "ending_secured": return "You have at least 500 points and no fuel remaining. Wait for the round to finish.";
            case "ending_no_fuel": return "The round is almost over and you have no fuel remaining.";
            case "countdown_position": return "Stand near your working brazier so you are ready when the round starts.";
            case "round_complete": return "The round has finished. Check your supplies before the next countdown.";
            case "lobby": return "Enter the prison to join a round. Supply preparation follows once you are inside.";
            case "countdown": return "Your supplies are prepared. Wait for the next round.";
            default: return reason.startsWith("make_space_for_")
                ? "Feed carried fuel to make room for the missing supplies."
                : "Follow the highlighted target or inventory items. You perform every interaction.";
        }
    }
}
