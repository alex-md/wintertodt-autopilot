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

    public static String strategy(WintertodtStrategy strategy)
    {
        switch (strategy)
        {
            case MASS_XP: return "Feeds roots directly by default to focus on Firemaking XP.";
            case MASS_POINTS: return "Uses larger batches and kindling to work toward reward thresholds.";
            case LOW_ATTENTION: return "Fills available inventory space to reduce changes of task.";
            default: return "Uses manageable batches, fletching enough to reach the next reward goal.";
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

    public static String explanation(WintertodtObjective o)
    {
        if (o == null) { return "Visit Wintertodt on a mass world. Your next action and supply checks appear here automatically."; }
        String reason = o.getReason();
        switch (reason)
        {
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
            case "local_pyromancer_down": return "Use a rejuvenation potion on the pyromancer so the brazier can drain Wintertodt's energy again.";
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
