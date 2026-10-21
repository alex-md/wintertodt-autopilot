package com.wintertodtautopilot;

import com.google.gson.JsonElement;
import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtSoloCaptureTest
{
    private final WintertodtSoloCapture capture = new WintertodtSoloCapture(net.runelite.http.api.RuneLiteAPI.GSON);
    private final WintertodtSoloEngine source = new WintertodtSoloEngine();
    private final WintertodtSoloCapture.Replay replay = capture.new Replay();
    private boolean first = true;
    private long sequence;

    private String record(int tick, int energy, int roots, WintertodtSoloCapture.Lifecycle event, boolean finish)
    {
        if (event == WintertodtSoloCapture.Lifecycle.SUSPEND) { source.suspendObservations(); }
        if (event == WintertodtSoloCapture.Lifecycle.RESET) { source.reset(); }
        WintertodtSnapshot s = WintertodtSoloReliabilityTest.observed(tick, energy, 100, roots, BrazierState.UNLIT)
            .toBuilder().interaction("Private player name").build();
        JsonElement before = capture.checkpoint(source);
        source.configure(5000, finish);
        WintertodtObjective o = source.decide(s, WintertodtSoloReliabilityTest.SETTINGS);
        String encoded = capture.encode(s, WintertodtSoloReliabilityTest.SETTINGS, 5000, finish,
            event, first, sequence++, before, o, source.getPhase(), null);
        first = false;
        replay.accept(capture.decode("client-log-prefix " + encoded));
        return encoded;
    }

    @Test public void checkpointAndSequentialReplayRetainRecoveryBatchesAndLifecycle()
    {
        record(1, 28, 0, WintertodtSoloCapture.Lifecycle.NONE, false);
        record(2, 9, 1, WintertodtSoloCapture.Lifecycle.NONE, false);
        record(2, 9, 1, WintertodtSoloCapture.Lifecycle.NONE, false); // Duplicate tick retained.
        record(10, 11, 2, WintertodtSoloCapture.Lifecycle.SUSPEND, false);
        record(11, 12, 3, WintertodtSoloCapture.Lifecycle.NONE, false);
        record(12, 12, 4, WintertodtSoloCapture.Lifecycle.NONE, false);
        record(13, 12, 4, WintertodtSoloCapture.Lifecycle.NONE, true);
        record(14, 12, 4, WintertodtSoloCapture.Lifecycle.NONE, false); // Finish stays latched.
        record(15, 100, 0, WintertodtSoloCapture.Lifecycle.RESET, false);
        assertEquals(9, replay.getDecisions());
    }

    @Test public void captureCanStartMidRoundAndDoesNotRecordInteractionNames()
    {
        source.decide(WintertodtSoloReliabilityTest.observed(1, 9, 100, 0, BrazierState.UNLIT), WintertodtSoloReliabilityTest.SETTINGS);
        String line = record(3, 11, 2, WintertodtSoloCapture.Lifecycle.NONE, false);
        assertFalse(line.contains("Private player name"));
        assertEquals(1, replay.getDecisions());
    }

    @Test(expected = IllegalStateException.class) public void missingDecisionIsDetectedRatherThanReseedingEveryTick()
    {
        record(1, 28, 0, WintertodtSoloCapture.Lifecycle.NONE, false);
        // A missing observation enters recovery; replay must detect the state discontinuity.
        source.decide(WintertodtSoloReliabilityTest.observed(2, 9, 100, 1, BrazierState.UNLIT), WintertodtSoloReliabilityTest.SETTINGS);
        record(3, 11, 2, WintertodtSoloCapture.Lifecycle.NONE, false);
    }

    @Test(expected = IllegalArgumentException.class) public void incompatibleOrIncompleteCaptureIsRejected()
    { capture.decode(WintertodtSoloCapture.PREFIX + "{\"revision\":\"unknown\"}"); }

    @Test(expected = IllegalStateException.class) public void droppedDuplicateTickRecordIsDetectedEvenWithUnchangedControllerState()
    {
        record(1, 28, 0, WintertodtSoloCapture.Lifecycle.NONE, false);
        sequence++; // A dropped log record, even if it caused no controller mutation.
        record(1, 28, 0, WintertodtSoloCapture.Lifecycle.NONE, false);
    }

    @Test public void summaryReportsObservedCoverageAndEmitsOnceAtVerifiedBoundary()
    {
        WintertodtSoloRunSummary summary = new WintertodtSoloRunSummary();
        WintertodtSnapshot s = WintertodtSoloReliabilityTest.observed(1, 28, 100, 0, BrazierState.LIT);
        WintertodtObjective o = source.decide(s, WintertodtSoloReliabilityTest.SETTINGS);
        assertNull(summary.observe(s, o, source.getPhase()));
        assertNull(summary.observe(s, o, source.getPhase()));
        String end = summary.observe(s.toBuilder().roundState(RoundState.COMPLETE).build(), o, SoloPhase.PREPARE);
        assertTrue(end.contains("coverage=observed_only"));
        assertTrue(end.contains("observations=1"));
        assertNull(summary.end("reset"));
    }

    @Test public void writesAnExplicitlySyntheticFixtureForTheReplayCommand() throws Exception
    {
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("Synthetic planner observations. This is not a live gameplay capture.");
        lines.add(record(1, 100, 0, WintertodtSoloCapture.Lifecycle.NONE, false));
        lines.add(record(2, 28, 1, WintertodtSoloCapture.Lifecycle.NONE, false));
        lines.add(record(3, 9, 2, WintertodtSoloCapture.Lifecycle.NONE, false));
        lines.add(record(10, 11, 3, WintertodtSoloCapture.Lifecycle.SUSPEND, false));
        lines.add(record(11, 12, 4, WintertodtSoloCapture.Lifecycle.NONE, false));
        lines.add(record(12, 12, 4, WintertodtSoloCapture.Lifecycle.NONE, false));
        lines.add(record(13, 12, 4, WintertodtSoloCapture.Lifecycle.NONE, true));
        java.nio.file.Path fixture = java.nio.file.Path.of("build/reports/solo-replay/synthetic.log");
        java.nio.file.Files.createDirectories(fixture.getParent());
        java.nio.file.Files.write(fixture, lines, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test public void replayRetainsLearnedReservesAndEffortChangesWithoutReseeding()
    {
        for (int tick = 0; tick <= 315; tick++)
        {
            PlannerSettings settings = tick < 312 ? WintertodtEffortPolicyTest.ACTIVE : WintertodtEffortPolicyTest.COMFORTABLE;
            WintertodtSnapshot s = WintertodtEffortPolicyTest.scene(tick, 25, 24).toBuilder()
                .personalDosesUsed(tick % 10 == 0 ? 1 : 0).build();
            JsonElement before = capture.checkpoint(source);
            source.configure(5000, tick >= 313);
            WintertodtObjective chosen = source.decide(s, settings);
            String line = capture.encode(s, settings, 5000, tick >= 313,
                WintertodtSoloCapture.Lifecycle.NONE, tick == 0, tick, before, chosen, source.getPhase(), null);
            replay.accept(capture.decode(line));
            if (tick == 311) { assertEquals(40, source.getPreparationDoseTarget()); }
        }
        assertEquals(316, replay.getDecisions()); assertTrue(source.isFinishRequested());
    }
}
