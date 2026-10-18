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
    @Test public void idleCannotDelayEmergencyWarmthOrRoundStart()
    {
        WintertodtAttentionTracker tracker = new WintertodtAttentionTracker();
        WintertodtAutopilotConfig config = new WintertodtAutopilotConfig() {
            @Override public boolean notifyIdle() { return true; }
            @Override public boolean notifyRoundStart() { return true; }
        };
        WintertodtSnapshot idle = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE)
            .warmth(80).playerIdle(true).inactiveTicks(10).build();
        assertEquals("Wintertodt: Feed the brazier", tracker.notification(idle, feed, config, 100));
        assertEquals("Wintertodt: low warmth - restore now",
            tracker.notification(idle.toBuilder().warmth(18).build(), feed, config, 101));
        assertEquals("Wintertodt round starts in 5 seconds", tracker.notification(idle.toBuilder()
            .roundState(RoundState.COUNTDOWN).warmth(80).secondsToStart(5).build(), feed, config, 102));
    }

    @Test public void emergencyDoesNotConsumeTheIdleCooldown()
    {
        WintertodtAttentionTracker tracker = new WintertodtAttentionTracker();
        WintertodtAutopilotConfig config = new WintertodtAutopilotConfig() {
            @Override public boolean notifyIdle() { return true; }
        };
        WintertodtSnapshot idle = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE)
            .warmth(18).playerIdle(true).inactiveTicks(10).build();
        assertEquals("Wintertodt: low warmth - restore now", tracker.notification(idle, feed, config, 100));
        assertEquals("Wintertodt: Feed the brazier", tracker.notification(idle.toBuilder().warmth(80).build(), feed, config, 101));
    }

    @Test public void earnedRewardNotifiesOnceAndResetsNextRound()
    {
        WintertodtAttentionTracker tracker = new WintertodtAttentionTracker();
        WintertodtAutopilotConfig config = rewards();
        WintertodtSnapshot s = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).warmth(80).points(490)
            .brumaKindling(20).build();
        assertNull(tracker.notification(s, feed, config, 100)); // Carried fuel cannot secure a reward.
        assertEquals("Wintertodt: 500 points secured - reward minimum reached",
            tracker.notification(s.toBuilder().points(515).build(), feed, config, 101));
        assertNull(tracker.notification(s.toBuilder().points(1000).build(), feed, config, 102));
        tracker.notification(s.toBuilder().roundState(RoundState.COUNTDOWN).points(0).build(), feed, config, 103);
        assertNull(tracker.notification(s.toBuilder().points(0).build(), feed, config, 104));
        assertNotNull(tracker.notification(s.toBuilder().points(500).build(), feed, config, 105));
    }

    @Test public void emergencyWinsAndRewardRemainsPendingUntilWarmthRecovers()
    {
        WintertodtAttentionTracker tracker = new WintertodtAttentionTracker();
        WintertodtSnapshot s = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).warmth(80).points(490).build();
        tracker.notification(s, feed, rewards(), 100);
        s = s.toBuilder().points(500).warmth(18).build();
        assertEquals("Wintertodt: low warmth - restore now", tracker.notification(s, feed, rewards(), 101));
        assertNull(tracker.notification(s, feed, rewards(), 102));
        assertEquals("Wintertodt: 500 points secured - reward minimum reached",
            tracker.notification(s.toBuilder().warmth(80).build(), feed, rewards(), 103));
    }

    @Test public void joiningOrReconnectingAboveThresholdDoesNotAnnounceAnOldReward()
    {
        WintertodtAttentionTracker tracker = new WintertodtAttentionTracker();
        WintertodtSnapshot s = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).warmth(80).points(600).build();
        assertNull(tracker.notification(s, feed, rewards(), 100));
        tracker.reset();
        assertNull(tracker.notification(s, feed, rewards(), 101));
        assertNull(tracker.notification(s.toBuilder().points(-1).build(), feed, rewards(), 102));
        assertNull(tracker.notification(s.toBuilder().points(700).build(), feed, rewards(), 103));
    }

    @Test public void unknownPointReadingPreservesObservedCrossingAndRewardIsOptIn()
    {
        WintertodtAttentionTracker tracker = new WintertodtAttentionTracker();
        WintertodtSnapshot s = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).warmth(80).points(490).build();
        tracker.notification(s, feed, rewards(), 100);
        assertNull(tracker.notification(s.toBuilder().points(-1).build(), feed, rewards(), 101));
        assertNotNull(tracker.notification(s.toBuilder().points(500).build(), feed, rewards(), 102));
        tracker.reset();
        tracker.notification(s, feed, new WintertodtAutopilotConfig() { }, 200);
        assertNull(tracker.notification(s.toBuilder().points(500).build(), feed, new WintertodtAutopilotConfig() { }, 201));
    }

    private WintertodtAutopilotConfig rewards()
    {
        return new WintertodtAutopilotConfig() { @Override public boolean notifyRewardSecured() { return true; } };
    }
}
