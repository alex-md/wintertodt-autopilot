package com.wintertodtautopilot;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WintertodtRoundTrackerTest
{
    private Client client;
    private WintertodtRoundTracker tracker;
    private Widget energy, points;
    @Before public void setup() throws Exception
    {
        client = mock(Client.class); tracker = new WintertodtRoundTracker();
        TrackerTestSupport.inject(tracker, "client", client);
        energy = mock(Widget.class); points = mock(Widget.class);
        when(client.getWidget(InterfaceID.WintStatus.ENERGY)).thenReturn(energy);
        when(client.getWidget(InterfaceID.WintStatus.POINTS)).thenReturn(points);
    }
    private WintertodtSnapshot observe(int tick, int e, int p, int delay)
    {
        when(client.getTickCount()).thenReturn(tick);
        when(energy.getText()).thenReturn("Wintertodt's Energy: " + e + "%");
        when(points.getText()).thenReturn("Points: " + p);
        when(client.getVarbitValue(VarbitID.WINT_TRANSMIT_RESPAWNDELAY)).thenReturn(delay);
        WintertodtSnapshot.WintertodtSnapshotBuilder b = WintertodtSnapshot.builder();
        tracker.fill(b, true, 4); return b.build();
    }
    @Test public void readsCurrentWidgetAndTimerSources()
    {
        WintertodtSnapshot s = observe(1, 0, 0, 20);
        assertEquals(RoundState.COUNTDOWN, s.getRoundState()); assertEquals(12, s.getSecondsToStart());
        assertEquals(RoundState.ACTIVE, observe(2, 100, 0, 0).getRoundState());
    }
    @Test public void endThenCountdownThenNewRound()
    {
        observe(1, 30, 500, 0);
        assertEquals(RoundState.ENDING, observe(2, 2, 550, 0).getRoundState());
        assertEquals(RoundState.COMPLETE, observe(3, 0, 575, 75).getRoundState());
        assertEquals(575, observe(4, 0, 0, 74).getPoints());
        assertEquals(RoundState.COUNTDOWN, observe(8, 0, 0, 70).getRoundState());
        assertEquals(RoundState.ACTIVE, observe(100, 100, 0, 0).getRoundState());
    }
    @Test public void missingWidgetIsUnknownRatherThanZero()
    {
        when(client.getWidget(InterfaceID.WintStatus.ENERGY)).thenReturn(null);
        WintertodtSnapshot.WintertodtSnapshotBuilder b = WintertodtSnapshot.builder();
        tracker.fill(b, true, 0);
        assertEquals(-1, b.build().getWintertodtEnergy()); assertFalse(b.build().active());
    }
    @Test public void joiningMidRoundReadsVisibleBarTitleWhenLegacyEnergyIsHidden()
    {
        Widget title = mock(Widget.class);
        when(client.getWidget(InterfaceID.WintStatus.ENERGY_TITLE)).thenReturn(title);
        when(title.getText()).thenReturn("<col=00ff00>Wintertodt's Energy: 70%</col>");
        when(energy.isHidden()).thenReturn(true);
        WintertodtSnapshot s = observe(100, 0, 0, 0);
        assertEquals(70, s.getWintertodtEnergy());
        assertEquals(RoundState.ACTIVE, s.getRoundState());
        WintertodtObjective objective = new WintertodtDecisionEngine().decide(s,
            new PlannerSettings(WintertodtStrategy.MASS_BALANCED, PointGoal.MINIMUM_500, FletchMode.AUTOMATIC, 35, 20));
        assertNotEquals(ObjectiveType.ENTER, objective.getType());
    }
    @Test public void hiddenHudOutsideDoesNotStartRoundFromStaleText()
    {
        Widget title = mock(Widget.class);
        when(client.getWidget(InterfaceID.WintStatus.ENERGY_TITLE)).thenReturn(title);
        when(title.getText()).thenReturn("Wintertodt's Energy: 70%");
        when(title.isHidden()).thenReturn(true);
        when(energy.isHidden()).thenReturn(true);
        when(energy.getText()).thenReturn("Wintertodt's Energy: 70%");
        WintertodtSnapshot.WintertodtSnapshotBuilder b = WintertodtSnapshot.builder();
        tracker.fill(b, false, 0);
        assertEquals(-1, b.build().getWintertodtEnergy());
        assertEquals(RoundState.LOBBY, b.build().getRoundState());
    }
    @Test public void parserHandlesTagsCommasAndMalformedValues()
    {
        assertEquals(1025, WintertodtRoundTracker.parseNumber("<col=ff0000>Points: 1,025</col>"));
        assertEquals(-1, WintertodtRoundTracker.parseNumber("Waiting for Wintertodt"));
        assertEquals(-1, WintertodtRoundTracker.parseNumber(null));
        assertEquals(-1, WintertodtRoundTracker.parseNumber("99999999999999999999"));
    }
    @Test public void classificationsUseObservedRateWithoutExactCountdown()
    {
        assertEquals(EndUrgency.NORMAL, tracker.classify(70, 1, 4));
        assertEquals(EndUrgency.ENDING_SOON, tracker.classify(40, 21, 4));
        assertEquals(EndUrgency.IMMINENT, tracker.classify(3, 22, 4));
        tracker.reset();
        assertEquals(EndUrgency.NORMAL, tracker.classify(100, 100, 4));
    }
}
