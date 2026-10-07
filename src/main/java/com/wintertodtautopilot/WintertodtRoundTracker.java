package com.wintertodtautopilot;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

@Singleton
public class WintertodtRoundTracker
{
    private static final Pattern NUMBER = Pattern.compile("([0-9][0-9,]*)");
    @Inject private Client client;
    private RoundState previous = RoundState.OUTSIDE;
    private final Deque<int[]> energySamples = new ArrayDeque<>();
    private int completedAt = -1;
    private int finalPoints = -1;

    public void reset()
    {
        previous = RoundState.OUTSIDE;
        energySamples.clear();
        completedAt = -1;
        finalPoints = -1;
    }
    public void fill(WintertodtSnapshot.WintertodtSnapshotBuilder b, boolean insidePrison, int lit)
    {
        int tick = client.getTickCount();
        // The current HUD puts "Wintertodt's Energy: 70%" in the bar title.
        // ENERGY is a legacy widget and can be hidden/empty in the live interface.
        int energy = readNumber(client.getWidget(InterfaceID.WintStatus.ENERGY_TITLE));
        if (energy < 0 || energy > 100)
        {
            energy = readNumber(client.getWidget(InterfaceID.WintStatus.ENERGY));
        }
        if (energy > 100) { energy = -1; }
        int points = readNumber(client.getWidget(InterfaceID.WintStatus.POINTS));
        int seconds = client.getVarbitValue(VarbitID.WINT_TRANSMIT_RESPAWNDELAY) * 30 / 50;
        RoundState state;
        if (!insidePrison) { state = RoundState.LOBBY; }
        else if (seconds > 0) { state = RoundState.COUNTDOWN; }
        else if (energy > 0) { state = RoundState.ACTIVE; }
        else { state = RoundState.COUNTDOWN; }
        boolean wasActive = previous == RoundState.ACTIVE || previous == RoundState.ENDING;
        if (insidePrison && wasActive && (energy == 0 || seconds > 0))
        {
            completedAt = tick;
            state = RoundState.COMPLETE;
        }
        if (insidePrison && completedAt >= 0 && tick - completedAt < 5 && energy <= 0)
        {
            state = RoundState.COMPLETE;
        }
        if (state == RoundState.ACTIVE && !wasActive)
        {
            energySamples.clear(); completedAt = -1; finalPoints = -1;
        }
        EndUrgency urgency = EndUrgency.NORMAL;
        if (state == RoundState.ACTIVE)
        {
            if (points >= 0) { finalPoints = points; }
            urgency = classify(energy, tick, lit);
            if (urgency != EndUrgency.NORMAL) { state = RoundState.ENDING; }
        }
        else { energySamples.clear(); }
        if (state == RoundState.COMPLETE && finalPoints >= 0)
        {
            finalPoints = Math.max(points, finalPoints);
            points = finalPoints;
        }
        b.roundState(state).wintertodtEnergy(energy).points(points).secondsToStart(seconds)
            .endUrgency(urgency).litBraziers(lit);
        previous = state;
    }
    EndUrgency classify(int energy, int tick, int lit)
    {
        if (!energySamples.isEmpty() && energy > energySamples.getLast()[1]) { energySamples.clear(); }
        energySamples.addLast(new int[]{tick, energy});
        while (!energySamples.isEmpty() && tick - energySamples.getFirst()[0] > 30) { energySamples.removeFirst(); }
        double secondsLeft = Double.POSITIVE_INFINITY;
        if (energySamples.size() >= 2)
        {
            int[] first = energySamples.getFirst();
            int drain = first[1] - energy;
            if (drain > 0 && tick - first[0] >= 10) { secondsLeft = energy * (tick - first[0]) * 0.6 / drain; }
        }
        // Energy cutoffs are conservative categories, not promises of an exact finish time.
        if (energy <= 3 || secondsLeft <= 10) { return EndUrgency.IMMINENT; }
        if (energy <= 12 && lit > 0 || secondsLeft <= 35) { return EndUrgency.ENDING_SOON; }
        return EndUrgency.NORMAL;
    }
    static int readNumber(Widget widget)
    {
        if (widget == null || widget.isHidden()) { return -1; }
        return parseNumber(widget.getText());
    }
    static int parseNumber(String text)
    {
        if (text == null) { return -1; }
        Matcher matcher = NUMBER.matcher(Text.removeTags(text));
        if (!matcher.find()) { return -1; }
        try { return Integer.parseInt(matcher.group(1).replace(",", "")); }
        catch (NumberFormatException e) { return -1; }
    }
}
