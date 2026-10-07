package com.wintertodtautopilot;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class WintertodtHintArrowTest
{
    @Test public void existingArrowIsNeitherOverwrittenNorCleared() throws Exception
    {
        Client client = mock(Client.class); WintertodtHintArrow arrow = new WintertodtHintArrow();
        TrackerTestSupport.inject(arrow, "client", client);
        when(client.getHintArrowType()).thenReturn(HintArrowType.NPC);
        when(client.getHintArrowNpc()).thenReturn(mock(NPC.class));
        arrow.update(null, WorkingCorner.SOUTHEAST.location(), true); arrow.clear();
        verify(client, never()).setHintArrow(any(WorldPoint.class)); verify(client, never()).clearHintArrow();
    }
    @Test public void ownedArrowIsSetOnceAndClearedOnDisable() throws Exception
    {
        Client client = mock(Client.class); WintertodtHintArrow arrow = new WintertodtHintArrow();
        TrackerTestSupport.inject(arrow, "client", client);
        WorldPoint point = WorkingCorner.SOUTHEAST.location();
        arrow.update(null, point, true);
        when(client.getHintArrowType()).thenReturn(HintArrowType.COORDINATE);
        when(client.getHintArrowPoint()).thenReturn(point);
        arrow.update(null, point, true); arrow.update(null, point, false);
        verify(client, times(1)).setHintArrow(point); verify(client, times(1)).clearHintArrow();
    }
    @Test public void npcArrowDoesNotResetEveryTick() throws Exception
    {
        Client client = mock(Client.class); WintertodtHintArrow arrow = new WintertodtHintArrow();
        TrackerTestSupport.inject(arrow, "client", client); NPC npc = mock(NPC.class);
        arrow.update(npc, WorkingCorner.SOUTHEAST.location(), true);
        when(client.getHintArrowType()).thenReturn(HintArrowType.NPC); when(client.getHintArrowNpc()).thenReturn(npc);
        arrow.update(npc, WorkingCorner.SOUTHEAST.location(), true);
        verify(client, times(1)).setHintArrow(npc); verify(client, never()).clearHintArrow();
    }
}
