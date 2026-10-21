package com.wintertodtautopilot;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static com.wintertodtautopilot.WintertodtEffortPolicyTest.*;

public class WintertodtHealingCueTest
{
    private WintertodtSnapshot down(int tick, PlayerActivity activity, WintertodtSnapshot.ActionState request)
    {
        return corner(scene(tick, 25, 24), WorkingCorner.SOUTHWEST, BrazierState.UNLIT,
            CornerObservation.PyromancerState.INCAPACITATED, 0, tick).toBuilder()
            .currentActivity(activity).actionActivity(PlayerActivity.HEALING).actionCorner(WorkingCorner.SOUTHWEST)
            .actionState(request).build();
    }
    @Test public void comfortableKeepsTheNpcOutlinedDuringPendingApproachArrivalAndFailedRequest()
    {
        WintertodtSoloEngine engine = new WintertodtSoloEngine();
        WintertodtAutopilotPlugin plugin = mock(WintertodtAutopilotPlugin.class);
        ModelOutlineRenderer outlines = mock(ModelOutlineRenderer.class);
        NPC npc = mock(NPC.class);
        WintertodtSceneOverlay overlay = new WintertodtSceneOverlay(mock(Client.class), plugin,
            new WintertodtAutopilotConfig() { }, outlines);
        when(plugin.getTargetNpc()).thenReturn(npc);
        Graphics2D graphics = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try
        {
            for (int tick = 10; tick <= 12; tick++)
            {
                WintertodtSnapshot s = down(tick, tick == 10 ? PlayerActivity.MOVING : PlayerActivity.IDLE,
                    tick == 12 ? WintertodtSnapshot.ActionState.UNCONFIRMED : WintertodtSnapshot.ActionState.REQUESTED);
                WintertodtObjective o = engine.decide(s, COMFORTABLE);
                assertEquals(TargetKind.PYROMANCER, o.getTarget()); assertEquals(WorkingCorner.SOUTHWEST, o.getDestination());
                assertFalse(WintertodtGuidance.performing(s, o));
                assertEquals(tick == 12 ? ObjectiveType.HEAL_PYROMANCER : ObjectiveType.WAIT, o.getType());
                when(plugin.getSnapshot()).thenReturn(s); when(plugin.getObjective()).thenReturn(o);
                overlay.render(graphics);
            }
            verify(outlines, times(3)).drawOutline(eq(npc), eq(1), any(java.awt.Color.class), eq(1));
            // Once the observed transformation confirms recovery, normal work replaces this target.
            WintertodtSnapshot healed = scene(13, 25, 23).toBuilder().actionActivity(PlayerActivity.HEALING)
                .actionCorner(WorkingCorner.SOUTHWEST).actionState(WintertodtSnapshot.ActionState.CONFIRMED).build();
            WintertodtObjective next = engine.decide(healed, COMFORTABLE);
            assertNotEquals(TargetKind.PYROMANCER, next.getTarget());
        }
        finally { graphics.dispose(); }
    }
    private WintertodtSnapshot injury(int tick, PlayerActivity activity, int health, int healthTick)
    {
        return corner(scene(tick, 25, 24), WorkingCorner.NORTHEAST, BrazierState.UNLIT,
            CornerObservation.PyromancerState.ALIVE, health, healthTick).toBuilder()
            .brumaRoots(3).freeSlots(3).currentActivity(activity).build();
    }
    @Test public void anOptionalHealingTargetSurvivesMovementAndArrivalWithoutStartingNewDetours()
    {
        WintertodtSoloEngine engine = new WintertodtSoloEngine();
        assertEquals("solo_points_heal", engine.decide(injury(10, PlayerActivity.FLETCHING, 50, 10), ACTIVE).getReason());
        for (PlayerActivity activity : new PlayerActivity[]{PlayerActivity.MOVING, PlayerActivity.IDLE})
        {
            WintertodtObjective o = engine.decide(injury(activity == PlayerActivity.MOVING ? 11 : 12, activity, 50, 10), ACTIVE);
            assertEquals("solo_points_heal", o.getReason()); assertEquals(WorkingCorner.NORTHEAST, o.getDestination());
        }
        WintertodtSoloEngine fresh = new WintertodtSoloEngine();
        assertNotEquals("solo_points_heal", fresh.decide(injury(10, PlayerActivity.MOVING, 50, 10), ACTIVE).getReason());
    }
    @Test public void approachDoesNotOverrideExpiredHealthSafetyOrObservedHealing()
    {
        WintertodtSoloEngine engine = new WintertodtSoloEngine();
        engine.decide(injury(10, PlayerActivity.FLETCHING, 50, 10), ACTIVE);
        assertNotEquals(TargetKind.PYROMANCER, engine.decide(injury(19, PlayerActivity.MOVING, 50, 10), ACTIVE).getTarget());
        engine.decide(injury(20, PlayerActivity.FLETCHING, 50, 20), ACTIVE);
        assertEquals(ObjectiveType.RESTORE_WARMTH, engine.decide(injury(21, PlayerActivity.MOVING, 50, 20)
            .toBuilder().warmth(10).build(), ACTIVE).getType());
        assertNotEquals(TargetKind.PYROMANCER, engine.decide(injury(22, PlayerActivity.MOVING, 50, 20), ACTIVE).getTarget());
        engine.decide(injury(23, PlayerActivity.FLETCHING, 50, 23), ACTIVE);
        assertNotEquals(TargetKind.PYROMANCER, engine.decide(injury(24, PlayerActivity.MOVING, 100, 24), ACTIVE).getTarget());
    }
}
