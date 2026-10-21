package com.wintertodtautopilot;

import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WintertodtBrazierHudTest
{
    private final Client client = mock(Client.class);
    private Widget widget(int id, int x, int y, int sprite)
    {
        Widget widget = mock(Widget.class);
        when(widget.getBounds()).thenReturn(new Rectangle(x, y, 20, 20));
        when(widget.getSpriteId()).thenReturn(sprite); when(client.getWidget(id)).thenReturn(widget);
        return widget;
    }
    private void hud()
    {
        // Deliberately scramble child order: layout establishes compass direction.
        widget(InterfaceID.WintStatus.BRAZIER1, 100, 200, 1399);
        widget(InterfaceID.WintStatus.BRAZIER2, 200, 100, 1398);
        widget(InterfaceID.WintStatus.BRAZIER3, 100, 100, 1397);
        widget(InterfaceID.WintStatus.BRAZIER4, 200, 200, 1398);
    }
    @Test public void usesNamedCurrentWidgetsAndCompassLayout()
    {
        hud(); Map<WorkingCorner, BrazierState> observed = WintertodtBrazierHud.read(client);
        assertEquals(BrazierState.LIT, observed.get(WorkingCorner.SOUTHWEST));
        assertEquals(BrazierState.BROKEN, observed.get(WorkingCorner.NORTHWEST));
        assertEquals(BrazierState.UNLIT, observed.get(WorkingCorner.NORTHEAST));
        assertEquals(BrazierState.UNLIT, observed.get(WorkingCorner.SOUTHEAST));
    }
    @Test public void unknownSpriteHiddenAndInvalidLayoutsNeverBecomeUnlit()
    {
        hud(); Widget ne = client.getWidget(InterfaceID.WintStatus.BRAZIER2);
        when(ne.getSpriteId()).thenReturn(99999);
        assertEquals(BrazierState.UNKNOWN, WintertodtBrazierHud.read(client).get(WorkingCorner.NORTHEAST));
        when(ne.isHidden()).thenReturn(true); assertTrue(WintertodtBrazierHud.read(client).isEmpty());
        when(ne.isHidden()).thenReturn(false); when(ne.getBounds()).thenReturn(new Rectangle());
        assertTrue(WintertodtBrazierHud.read(client).isEmpty());
        when(ne.getBounds()).thenReturn(new Rectangle(100, 200, 20, 20));
        assertTrue(WintertodtBrazierHud.read(client).isEmpty());
    }
    @Test public void sceneOverridesHudAndConflictInvalidatesRemoteReadings()
    {
        hud(); WintertodtObjectTracker objects = mock(WintertodtObjectTracker.class);
        WintertodtNpcTracker npcs = mock(WintertodtNpcTracker.class);
        when(objects.brazierAt(WorkingCorner.SOUTHWEST)).thenReturn(BrazierState.LIT);
        WintertodtCornerTracker tracker = new WintertodtCornerTracker();
        List<CornerObservation> first = tracker.observe(10, objects, npcs, client);
        WintertodtSnapshot s = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).observationTick(10).corners(first).build();
        assertEquals(1, s.observedGlobalLitBraziers());
        assertEquals(CornerObservation.Source.HUD_WIDGET, s.corner(WorkingCorner.NORTHWEST).getBrazierSource());
        assertFalse(s.corner(WorkingCorner.NORTHWEST).isBrazierVisible());
        when(objects.brazierAt(WorkingCorner.SOUTHWEST)).thenReturn(BrazierState.UNLIT);
        s = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).observationTick(11).corners(tracker.observe(11, objects, npcs, client)).build();
        assertEquals(BrazierState.UNLIT, s.corner(WorkingCorner.SOUTHWEST).getBrazierState());
        assertEquals(BrazierState.UNKNOWN, s.corner(WorkingCorner.NORTHWEST).getBrazierState());
        assertEquals(-1, s.observedGlobalLitBraziers());
        when(client.getWidget(InterfaceID.WintStatus.BRAZIER1).getSpriteId()).thenReturn(1398);
        s = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).observationTick(12).corners(tracker.observe(12, objects, npcs, client)).build();
        assertEquals(0, s.observedGlobalLitBraziers());
    }
    @Test public void disappearingHudDoesNotReuseRemoteState()
    {
        hud(); WintertodtCornerTracker tracker = new WintertodtCornerTracker();
        WintertodtObjectTracker objects = mock(WintertodtObjectTracker.class);
        WintertodtNpcTracker npcs = mock(WintertodtNpcTracker.class);
        tracker.observe(10, objects, npcs, client);
        when(client.getWidget(InterfaceID.WintStatus.BRAZIER1)).thenReturn(null);
        WintertodtSnapshot s = WintertodtSnapshot.builder().inventoryKnown(true).equipmentKnown(true).observationTick(11).corners(tracker.observe(11, objects, npcs, client)).build();
        assertEquals(BrazierState.UNKNOWN, s.corner(WorkingCorner.NORTHWEST).getBrazierState());
        assertEquals(10, s.corner(WorkingCorner.NORTHWEST).getBrazierObservedTick());
        assertEquals(-1, s.observedGlobalLitBraziers());
    }
}
