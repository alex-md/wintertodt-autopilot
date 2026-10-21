package com.wintertodtautopilot;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/** Opt-in diagnostic format. No chat, account identifiers, scene handles or unbounded history. */
final class WintertodtSoloCapture
{
    static final String PREFIX = "solo_capture_v1 ";
    // Checkpoints deliberately bind replay to this controller revision. Bump on incompatible changes.
    private static final String REVISION = "phase3-healing-cues-4";
    private static final Gson GSON = new Gson();
    enum Lifecycle { NONE, RESET, SUSPEND }

    static JsonElement checkpoint(WintertodtSoloEngine engine) { return GSON.toJsonTree(engine); }
    static WintertodtSoloEngine restore(JsonElement checkpoint)
    { return GSON.fromJson(checkpoint, WintertodtSoloEngine.class); }

    static String encode(WintertodtSnapshot snapshot, PlannerSettings settings, int pointGoal,
        boolean requestFinish, Lifecycle lifecycle, boolean segmentStart, long sequence, JsonElement before,
        WintertodtObjective expected, SoloPhase phase, Client client)
    {
        Record r = new Record();
        r.revision = REVISION;
        r.snapshot = snapshot.toBuilder().interaction("None".equals(snapshot.getInteraction()) ? "None" : "Present").build();
        r.settings = settings; r.pointGoal = pointGoal; r.requestFinish = requestFinish;
        r.lifecycle = lifecycle; r.segmentStart = segmentStart; r.before = before;
        r.sequence = sequence;
        r.expected = expected; r.phase = phase;
        r.hud = hud(client);
        java.util.Map<WorkingCorner, BrazierState> hudStates = WintertodtBrazierHud.read(client);
        r.hudDecoded = hudStates;
        for (CornerObservation c : snapshot.getCorners())
        {
            BrazierState remote = hudStates.get(c.getCorner());
            if (c.getBrazierSource() == CornerObservation.Source.DIRECT_SCENE && c.brazierFresh(snapshot.getObservationTick())
                && remote != null && remote != BrazierState.UNKNOWN && remote != c.getBrazierState()) { r.hudSceneConflict = true; }
        }
        return PREFIX + GSON.toJson(r);
    }

    static Record decode(String line)
    {
        int start = line.indexOf(PREFIX);
        if (start < 0) { throw new IllegalArgumentException("Not a Solo capture line"); }
        if (line.length() > 1_000_000) { throw new IllegalArgumentException("Capture line is too large"); }
        Record r = GSON.fromJson(line.substring(start + PREFIX.length()), Record.class);
        if (r == null || !REVISION.equals(r.revision) || r.snapshot == null || r.settings == null
            || r.before == null || !r.before.isJsonObject() || r.expected == null || r.phase == null || r.lifecycle == null)
        { throw new IllegalArgumentException("Unsupported or incomplete Solo capture"); }
        return r;
    }

    private static List<HudWidget> hud(Client client)
    {
        List<HudWidget> result = new ArrayList<>();
        if (client == null) { return result; }
        for (int id : new int[]{InterfaceID.WintStatus.BRAZIER1, InterfaceID.WintStatus.BRAZIER2,
            InterfaceID.WintStatus.BRAZIER3, InterfaceID.WintStatus.BRAZIER4})
        {
            HudWidget h = new HudWidget(); h.id = id;
            Widget w = client.getWidget(id);
            h.present = w != null;
            if (w != null)
            {
                h.hidden = w.isHidden(); h.sprite = w.getSpriteId();
                Rectangle b = w.getBounds();
                if (b != null) { h.x = b.x; h.y = b.y; h.width = b.width; h.height = b.height; }
            }
            result.add(h);
        }
        return result;
    }

    static final class Record
    {
        String revision;
        WintertodtSnapshot snapshot;
        PlannerSettings settings;
        int pointGoal;
        boolean requestFinish;
        Lifecycle lifecycle;
        boolean segmentStart;
        long sequence;
        JsonElement before;
        WintertodtObjective expected;
        SoloPhase phase;
        List<HudWidget> hud;
        java.util.Map<WorkingCorner, BrazierState> hudDecoded;
        boolean hudSceneConflict;
    }
    private static final class HudWidget
    {
        int id, sprite = -1, x, y, width, height;
        boolean present, hidden;
    }

    /** Replays consecutive decisions, seeding only at an explicitly marked capture boundary. */
    static final class Replay
    {
        private WintertodtSoloEngine engine;
        private int decisions;
        private long previousSequence;
        int getDecisions() { return decisions; }
        void accept(Record r)
        {
            if (engine == null || r.segmentStart) { engine = restore(r.before); }
            else
            {
                if (r.sequence != previousSequence + 1)
                { throw new IllegalStateException("Missing or out-of-order capture record at tick " + r.snapshot.getObservationTick()); }
                if (r.lifecycle == Lifecycle.RESET) { engine.reset(); }
                else if (r.lifecycle == Lifecycle.SUSPEND) { engine.suspendObservations(); }
            }
            if (!checkpoint(engine).equals(r.before))
            { throw new IllegalStateException("Controller continuity mismatch at tick " + r.snapshot.getObservationTick()); }
            engine.configure(r.pointGoal, r.requestFinish);
            WintertodtObjective actual = engine.decide(r.snapshot, r.settings);
            if (!actual.equals(r.expected) || engine.getPhase() != r.phase)
            { throw new IllegalStateException("Decision mismatch at tick " + r.snapshot.getObservationTick()
                + ": expected " + r.expected + " in " + r.phase + ", got " + actual + " in " + engine.getPhase()); }
            decisions++;
            previousSequence = r.sequence;
        }
    }
}
