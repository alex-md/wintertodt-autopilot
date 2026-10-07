package com.wintertodtautopilot;

import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtHighlightTest
{
    @Test public void interruptionCuePersistsUntilResumingAndExcludesMovementAndWaiting()
    {
        WintertodtSnapshot s = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).interrupted(true).build();
        WintertodtObjective o = new WintertodtObjective(ObjectiveType.FLETCH, "Continue fletching", "batch_fletch", TargetKind.NONE, 12, false);
        assertTrue(WintertodtHighlight.needsRestart(s, o));
        assertFalse(WintertodtHighlight.needsRestart(s.toBuilder().currentActivity(PlayerActivity.FLETCHING).build(), o));
        assertFalse(WintertodtHighlight.needsRestart(s.toBuilder().currentActivity(PlayerActivity.MOVING).build(), o));
        assertFalse(WintertodtHighlight.needsRestart(s.toBuilder().roundState(RoundState.COMPLETE).build(), o));
        assertFalse(WintertodtHighlight.needsRestart(s,
            new WintertodtObjective(ObjectiveType.WAIT, "Wait", "countdown", TargetKind.NONE, 0, false)));
    }
}
