package com.wintertodtautopilot;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtPluginPanelTest
{
    private PlannerSettings settings = new PlannerSettings(WintertodtStrategy.MASS_XP, PointGoal.POINTS_1000, FletchMode.AUTOMATIC, 35, 20);

    @Test public void outsideChecksRemainUnknownAndXpKnifeIsOptional()
    {
        WintertodtPanelState state = new WintertodtPanelState(WintertodtSnapshot.builder().build(), null, settings,
            WorkingCorner.AUTOMATIC, 0, false, true);
        for (WintertodtPanelState.Requirement row : state.requirements())
        { assertEquals(WintertodtPanelState.Status.UNKNOWN, row.getStatus()); }
        assertEquals("Optional for this strategy", state.requirements().get(2).getDetail());
    }
    @Test public void observedSupplyChecksUseEquippedToolsAndDoseReserve()
    {
        WintertodtSnapshot s = WintertodtSnapshot.builder().roundState(RoundState.ACTIVE).hasAxe(true)
            .hasHammer(true).hasBrumaTorch(true).warmClothingCount(4).rejuvenationDoses(3).build();
        WintertodtPanelState state = new WintertodtPanelState(s, null, settings, WorkingCorner.AUTOMATIC, 4, false, true);
        assertEquals(WintertodtPanelState.Status.READY, state.requirements().get(0).getStatus());
        assertEquals(WintertodtPanelState.Status.READY, state.requirements().get(3).getStatus());
        assertEquals("3 / 4 doses", state.requirements().get(4).getDetail());
        assertEquals(WintertodtPanelState.Status.CHECK, state.requirements().get(4).getStatus());
    }
    @Test public void refreshingControlsDoesNotWriteSettingsAndUserChangesDo()
        throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            List<String> writes = new ArrayList<>();
            int[] pauses = {0};
            WintertodtPluginPanel panel = new WintertodtPluginPanel(() -> pauses[0]++, (key, value) -> writes.add(key + "=" + value));
            panel.update(new WintertodtPanelState(WintertodtSnapshot.builder().build(), null, settings,
                WorkingCorner.NORTHEAST, 0, true, false));
            assertTrue(writes.isEmpty());
            List<Component> children = components(panel);
            JComboBox<?> strategy = (JComboBox<?>) children.stream().filter(c -> c instanceof JComboBox).findFirst().get();
            strategy.setSelectedItem(WintertodtStrategy.MASS_BALANCED);
            assertEquals(1, writes.size()); assertTrue(writes.get(0).startsWith("strategy="));
            JButton button = (JButton) children.stream().filter(c -> c instanceof JButton
                && ((JButton) c).getText().equals("Resume guidance")).findFirst().get();
            button.doClick(); assertEquals(1, pauses[0]);
        });
    }
    private static List<Component> components(Container parent)
    {
        List<Component> result = new ArrayList<>();
        for (Component child : parent.getComponents())
        {
            result.add(child);
            if (child instanceof Container) { result.addAll(components((Container) child)); }
        }
        return result;
    }
}
