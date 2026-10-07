package com.wintertodtautopilot;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtAttentionTrackerTest
{
    private final WintertodtObjective feed = new WintertodtObjective(ObjectiveType.FEED, "Feed the brazier", "batch", TargetKind.BRAZIER, 1, false);
    @Test public void warmthNotifiesOncePerEpisodeAndPendingWarningSurvivesCooldown()
    {
        WintertodtAttentionTracker tracker = new WintertodtAttentionTracker();
        WintertodtAutopilotConfig config = new WintertodtAutopilotConfig() { };
        WintertodtSnapshot low = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).warmth(18).warmthItem(ItemID.CAKE).build();
        assertNotNull(tracker.notification(low, feed, config, 100));
        assertNull(tracker.notification(low, feed, config, 200));
        assertNull(tracker.notification(low.toBuilder().warmth(80).build(), feed, config, 201));
        assertNotNull(tracker.notification(low, feed, config, 202));
        tracker.notification(low.toBuilder().warmth(80).build(), feed, config, 203);
        assertNull(tracker.notification(low, feed, config, 204));
        assertNotNull(tracker.notification(low, feed, config, 253));
    }
    @Test public void idleUsesGraceInactivityAndEpisodeGating()
    {
        WintertodtAttentionTracker tracker = new WintertodtAttentionTracker();
        WintertodtAutopilotConfig config = new WintertodtAutopilotConfig() { @Override public boolean notifyIdle() { return true; } };
        WintertodtSnapshot idle = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).warmth(80).playerIdle(true).inactiveTicks(9).build();
        assertNull(tracker.notification(idle, feed, config, 100));
        idle = idle.toBuilder().inactiveTicks(10).build();
        assertNotNull(tracker.notification(idle, feed, config, 101));
        assertNull(tracker.notification(idle, feed, config, 201));
        tracker.notification(idle.toBuilder().playerIdle(false).build(), feed, config, 202);
        assertNotNull(tracker.notification(idle, feed, config, 203));
    }
    @Test public void waitDoesNotNotifyAndConsumedWarmthClearsAlert()
    {
        WintertodtAttentionTracker tracker = new WintertodtAttentionTracker();
        WintertodtSnapshot s = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).warmth(18)
            .currentActivity(PlayerActivity.RESTORING_WARMTH).build();
        assertNull(tracker.notification(s, feed, new WintertodtAutopilotConfig() { }, 100));
    }
    @Test public void countdownNotifiesOnceThenResetsForNextRound()
    {
        WintertodtAttentionTracker tracker = new WintertodtAttentionTracker();
        WintertodtAutopilotConfig config = new WintertodtAutopilotConfig() { @Override public boolean notifyRoundStart() { return true; } };
        WintertodtSnapshot s = WintertodtSnapshot.builder().roundState(RoundState.COUNTDOWN).secondsToStart(5).build();
        assertNotNull(tracker.notification(s, feed, config, 100));
        assertNull(tracker.notification(s, feed, config, 200));
        tracker.notification(s.toBuilder().roundState(RoundState.ACTIVE).warmth(90).build(), feed, config, 201);
        assertNotNull(tracker.notification(s, feed, config, 202));
    }
}
