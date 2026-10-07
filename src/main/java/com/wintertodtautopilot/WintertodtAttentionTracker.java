package com.wintertodtautopilot;

/** Rate limiting and attention episodes, independent of rendering and wall-clock time. */
public final class WintertodtAttentionTracker
{
    private int lastNotification = -1000;
    private boolean lowNotified;
    private boolean startNotified;
    private boolean attentionNotified;
    private RoundState lastRound = RoundState.OUTSIDE;

    public void reset()
    {
        lastNotification = -1000; lowNotified = false; startNotified = false;
        attentionNotified = false; lastRound = RoundState.OUTSIDE;
    }
    public String notification(WintertodtSnapshot s, WintertodtObjective objective,
        WintertodtAutopilotConfig config, int tick)
    {
        if (s.getRoundState() == RoundState.OUTSIDE) { reset(); return null; }
        boolean low = s.active() && s.getWarmth() >= 0
            && s.getWarmth() <= Math.min(config.emergencyWarmth(), config.restoreWarmth())
            && s.getCurrentActivity() != PlayerActivity.RESTORING_WARMTH;
        if (!low) { lowNotified = false; }
        if (s.getRoundState() == RoundState.COUNTDOWN && lastRound != RoundState.COUNTDOWN) { startNotified = false; }
        lastRound = s.getRoundState();
        if (!s.isPlayerIdle() || !objective.actionable()) { attentionNotified = false; }
        if (tick - lastNotification < 50) { return null; }
        String message = null;
        if (low && !lowNotified && config.notifyLowWarmth())
        {
            lowNotified = true; message = "Wintertodt: low warmth - restore now";
        }
        else if (config.notifyRoundStart() && !startNotified && s.getRoundState() == RoundState.COUNTDOWN
            && s.getSecondsToStart() > 0 && s.getSecondsToStart() <= 5)
        {
            startNotified = true; message = "Wintertodt round starts in " + s.getSecondsToStart() + " seconds";
        }
        else if (s.isPlayerIdle() && objective.actionable() && !attentionNotified
            && s.getInactiveTicks() * 0.6 >= config.inactivitySeconds())
        {
            boolean important = objective.getType() == ObjectiveType.REPAIR || objective.getType() == ObjectiveType.LIGHT
                || objective.getType() == ObjectiveType.HEAL_PYROMANCER || objective.getType() == ObjectiveType.MAKE_REJUVENATION;
            if (config.notifyIdle() || config.notifyStateChange() && important)
            {
                attentionNotified = true; message = "Wintertodt: " + objective.getInstruction();
            }
        }
        if (message != null) { lastNotification = tick; }
        return message;
    }
}
