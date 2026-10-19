package com.wintertodtautopilot;

/** Rate limiting and attention episodes, independent of rendering and wall-clock time. */
public final class WintertodtAttentionTracker
{
    private int lastAttentionNotification = -1000;
    private int lastWarmthNotification = -1000;
    private boolean lowNotified;
    private boolean startNotified;
    private boolean attentionNotified;
    private RoundState lastRound = RoundState.OUTSIDE;
    private int lastPoints = -1;
    private boolean rewardPending;
    private boolean rewardNotified;

    public void reset()
    {
        lastAttentionNotification = -1000; lastWarmthNotification = -1000;
        lowNotified = false; startNotified = false;
        attentionNotified = false; lastRound = RoundState.OUTSIDE;
        lastPoints = -1; rewardPending = false; rewardNotified = false;
    }
    public String notification(WintertodtSnapshot s, WintertodtObjective objective,
        WintertodtAutopilotConfig config, int tick)
    {
        if (s.getRoundState() == RoundState.OUTSIDE || s.getRoundState() == RoundState.LOBBY)
        { reset(); return null; }
        boolean newCountdown = s.getRoundState() == RoundState.COUNTDOWN && lastRound != RoundState.COUNTDOWN;
        boolean newActive = s.active() && lastRound != RoundState.ACTIVE && lastRound != RoundState.ENDING;
        if (newCountdown || newActive)
        {
            lastPoints = -1; rewardPending = false; rewardNotified = false;
            attentionNotified = false;
        }
        if (s.active() || s.getRoundState() == RoundState.COMPLETE)
        {
            if (lastPoints >= 0 && lastPoints < 500 && s.getPoints() >= 500 && !rewardNotified
                && config.notifyRewardSecured()) { rewardPending = true; }
            if (s.getPoints() >= 0) { lastPoints = s.getPoints(); }
        }
        boolean low = (s.active() || objective.getType() == ObjectiveType.RESTORE_WARMTH) && s.getWarmth() >= 0
            && s.getWarmth() < 100 && s.getWarmth() <= Math.min(config.emergencyWarmth(), config.restoreWarmth())
            && s.getCurrentActivity() != PlayerActivity.RESTORING_WARMTH;
        if (!low) { lowNotified = false; }
        if (newCountdown) { startNotified = false; }
        lastRound = s.getRoundState();
        if (!s.isPlayerIdle() || !objective.actionable()) { attentionNotified = false; }
        String message = null;
        if (low && !lowNotified && config.notifyLowWarmth() && tick - lastWarmthNotification >= 50)
        {
            lowNotified = true; lastWarmthNotification = tick;
            message = "Wintertodt: low warmth - restore now";
        }
        else if (config.notifyRoundStart() && !startNotified && s.getRoundState() == RoundState.COUNTDOWN
            && s.getSecondsToStart() > 0 && s.getSecondsToStart() <= 5)
        {
            startNotified = true; message = "Wintertodt round starts in " + s.getSecondsToStart() + " seconds";
        }
        else if (rewardPending && config.notifyRewardSecured() && !low)
        {
            rewardPending = false; rewardNotified = true;
            message = "Wintertodt: 500 points secured - reward minimum reached";
        }
        else if (s.isPlayerIdle() && objective.actionable() && !attentionNotified
            && tick - lastAttentionNotification >= 50
            && s.getInactiveTicks() * 0.6 >= config.inactivitySeconds())
        {
            boolean important = objective.getType() == ObjectiveType.REPAIR || objective.getType() == ObjectiveType.LIGHT
                || objective.getType() == ObjectiveType.HEAL_PYROMANCER || objective.getType() == ObjectiveType.MAKE_REJUVENATION;
            if (config.notifyIdle() || config.notifyStateChange() && important)
            {
                attentionNotified = true; lastAttentionNotification = tick;
                message = "Wintertodt: " + objective.getInstruction();
            }
        }
        return message;
    }
}
