package com.wintertodtautopilot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.*;

public class WintertodtSoloRefinementTest
{
    private final WintertodtSoloEngine engine = new WintertodtSoloEngine();
    private final PlannerSettings settings = WintertodtSoloReliabilityTest.SETTINGS;
    private final Map<WorkingCorner, BrazierState> states = new EnumMap<>(WorkingCorner.class);

    private WintertodtSnapshot scene(int tick, WorkingCorner player)
    {
        List<CornerObservation> corners = new ArrayList<>();
        for (WorkingCorner c : WorkingCorner.values())
        {
            if (c == WorkingCorner.AUTOMATIC) { continue; }
            corners.add(new CornerObservation(c, states.getOrDefault(c, BrazierState.LIT), CornerObservation.PyromancerState.ALIVE,
                -1, CornerObservation.Source.DIRECT_SCENE, CornerObservation.Source.DIRECT_SCENE, tick, tick, true, true));
        }
        return WintertodtSoloReliabilityTest.observed(tick, 80, 100, 0, BrazierState.LIT).toBuilder()
            .playerLocation(player.location()).workingCorner(player).clearCorners().corners(corners).build();
    }
    private BrazierRoute route(WorkingCorner c, int cost, WorkingCorner... onWay)
    { return new BrazierRoute(c, cost, Collections.singletonList(c.location()), Arrays.asList(onWay)); }

    @Test public void oppositeCornersNoLongerTieAdjacentOpeningBraziers()
    {
        for (WorkingCorner player : new WorkingCorner[]{WorkingCorner.SOUTHEAST, WorkingCorner.NORTHEAST})
        {
            engine.reset(); states.clear();
            for (WorkingCorner c : WorkingCorner.values()) { states.put(c, BrazierState.UNLIT); }
            states.put(player, BrazierState.LIT);
            WorkingCorner opposite = player == WorkingCorner.SOUTHEAST ? WorkingCorner.NORTHWEST : WorkingCorner.SOUTHWEST;
            WintertodtObjective o = engine.decide(scene(1, player), settings);
            assertEquals(ObjectiveType.LIGHT, o.getType());
            assertNotEquals(opposite, o.getDestination());
        }
    }

    @Test public void passedBrazierIsServicedThenOriginalDestinationResumesInBothDiagonalDirections()
    {
        for (WorkingCorner player : new WorkingCorner[]{WorkingCorner.SOUTHEAST, WorkingCorner.NORTHEAST})
        {
            engine.reset(); states.clear(); engine.configure(13500, false);
            WorkingCorner primary = player == WorkingCorner.SOUTHEAST ? WorkingCorner.NORTHWEST : WorkingCorner.SOUTHWEST;
            WorkingCorner stop = player == WorkingCorner.SOUTHEAST ? WorkingCorner.SOUTHWEST : WorkingCorner.SOUTHEAST;
            states.put(primary, BrazierState.UNLIT);
            WintertodtSnapshot first = scene(1, player).toBuilder().brazierRoute(route(primary, 30, stop)).brazierRoute(route(stop, 3)).build();
            assertEquals(primary, engine.decide(first, settings).getDestination());
            states.put(stop, BrazierState.UNLIT);
            WintertodtSnapshot second = scene(2, player).toBuilder().brazierRoutes(first.getBrazierRoutes()).build();
            WintertodtObjective intermediate = engine.decide(second, settings);
            assertEquals(stop, intermediate.getDestination());
            assertEquals(SoloAdvice.Reason.ON_THE_WAY, intermediate.getSoloAdvice().getReason());
            assertEquals(primary, intermediate.getSoloAdvice().getDeferredDestination());
            assertTrue(WintertodtGuidance.reassurance(second, intermediate).contains("13,500"));
            WintertodtSnapshot approaching = scene(3, player).toBuilder()
                .brazierRoute(route(primary, 28)).brazierRoute(route(stop, 1)).build();
            assertEquals(stop, engine.decide(approaching, settings).getDestination()); // Keep the stop through path changes while approaching.
            states.put(stop, BrazierState.LIT);
            assertEquals(primary, engine.decide(scene(4, player).toBuilder().brazierRoutes(first.getBrazierRoutes()).build(), settings).getDestination());
        }
    }

    @Test public void observedWalkingCostAndInteractionCostDetermineOpeningOrder()
    {
        states.put(WorkingCorner.NORTHWEST, BrazierState.BROKEN); states.put(WorkingCorner.NORTHEAST, BrazierState.UNLIT);
        WintertodtSnapshot s = scene(1, WorkingCorner.SOUTHEAST).toBuilder()
            .brazierRoute(route(WorkingCorner.NORTHWEST, 5)).brazierRoute(route(WorkingCorner.NORTHEAST, 5)).build();
        assertEquals(WorkingCorner.NORTHEAST, engine.decide(s, settings).getDestination());
        engine.reset();
        assertEquals(WorkingCorner.NORTHWEST, engine.decide(s.toBuilder().clearBrazierRoutes()
            .brazierRoute(route(WorkingCorner.NORTHWEST, 0)).brazierRoute(route(WorkingCorner.NORTHEAST, 20)).build(), settings).getDestination());
    }

    @Test public void opportunisticStopRequiresFreshPhysicalEvidenceAndAnActualPathOpportunity()
    {
        states.put(WorkingCorner.NORTHWEST, BrazierState.UNLIT);
        WintertodtSnapshot first = scene(1, WorkingCorner.SOUTHEAST).toBuilder()
            .brazierRoute(route(WorkingCorner.NORTHWEST, 30, WorkingCorner.SOUTHWEST))
            .brazierRoute(route(WorkingCorner.SOUTHWEST, 3)).build();
        engine.decide(first, settings);
        states.put(WorkingCorner.SOUTHWEST, BrazierState.UNLIT);
        WintertodtSnapshot second = scene(2, WorkingCorner.SOUTHEAST).toBuilder().brazierRoutes(first.getBrazierRoutes()).build();
        List<CornerObservation> unknownPyro = new ArrayList<>();
        for (CornerObservation c : second.getCorners())
        {
            unknownPyro.add(c.getCorner() != WorkingCorner.SOUTHWEST ? c : new CornerObservation(c.getCorner(), c.getBrazierState(),
                CornerObservation.PyromancerState.UNKNOWN, -1, c.getBrazierSource(), CornerObservation.Source.NONE, 2, -1, true, false));
        }
        assertEquals(WorkingCorner.NORTHWEST, engine.decide(second.toBuilder().clearCorners().corners(unknownPyro).build(), settings).getDestination());
        WintertodtSnapshot offRoute = second.toBuilder().clearBrazierRoutes().brazierRoute(route(WorkingCorner.NORTHWEST, 30))
            .brazierRoute(route(WorkingCorner.SOUTHWEST, 3)).build();
        assertEquals(WorkingCorner.NORTHWEST, engine.decide(offRoute, settings).getDestination());
    }

    @Test public void knownUnreachableRequiredFireProducesAnHonestWait()
    {
        states.put(WorkingCorner.NORTHWEST, BrazierState.UNLIT);
        WintertodtObjective o = engine.decide(scene(1, WorkingCorner.SOUTHEAST).toBuilder()
            .brazierRoute(route(WorkingCorner.NORTHWEST, -1)).build(), settings);
        assertEquals("solo_route_blocked", o.getReason()); assertEquals(ObjectiveType.WAIT, o.getType());
    }

    @Test public void finishNeverLightsAnUnselectedThirdFireJustBecauseTheRoutePassesIt()
    {
        states.put(WorkingCorner.NORTHEAST, BrazierState.UNLIT); states.put(WorkingCorner.SOUTHWEST, BrazierState.UNLIT);
        states.put(WorkingCorner.NORTHWEST, BrazierState.UNLIT);
        engine.configure(13500, false);
        WintertodtSnapshot s = scene(1, WorkingCorner.SOUTHEAST).toBuilder().wintertodtEnergy(8).points(13500)
            .brazierRoute(route(WorkingCorner.NORTHEAST, 10, WorkingCorner.NORTHWEST))
            .brazierRoute(route(WorkingCorner.SOUTHWEST, 30)).brazierRoute(route(WorkingCorner.NORTHWEST, 2)).build();
        PlannerSettings active = new PlannerSettings(settings.getStrategy(), settings.getPointGoal(), settings.getFletchMode(),
            settings.getRestoreWarmth(), settings.getEmergencyWarmth(), SoloEffort.ACTIVE);
        WintertodtObjective o = engine.decide(s.toBuilder().runEnergyPercent(100).build(), active);
        assertEquals(WorkingCorner.NORTHEAST, o.getDestination());
        assertEquals(SoloAdvice.Reason.FINISH, o.getSoloAdvice().getReason());
        assertTrue(WintertodtGuidance.reassurance(s, o).contains("goal reached"));
        assertTrue(WintertodtGuidance.reassurance(s, o).contains("leave other braziers alone"));
    }

    @Test public void recoveryAndLocalSustainIgnoreOtherUnlitRouteOpportunities()
    {
        states.put(WorkingCorner.NORTHWEST, BrazierState.UNLIT); states.put(WorkingCorner.SOUTHEAST, BrazierState.UNLIT);
        engine.configure(13500, false);
        WintertodtSnapshot recovery = scene(1, WorkingCorner.SOUTHEAST).toBuilder().wintertodtEnergy(9)
            .brazierRoute(route(WorkingCorner.NORTHWEST, 10)).build();
        WintertodtObjective o = engine.decide(recovery, settings);
        assertEquals(ObjectiveType.CHOP, o.getType());
        assertEquals(SoloAdvice.Reason.RECOVERY, o.getSoloAdvice().getReason());
        assertTrue(WintertodtGuidance.reassurance(recovery, o).contains("100 / 13,500 points"));
        assertTrue(WintertodtGuidance.reassurance(recovery, o).contains("remaining fires"));
        engine.reset();
        states.put(WorkingCorner.SOUTHEAST, BrazierState.LIT);
        WintertodtSnapshot local = scene(2, WorkingCorner.SOUTHEAST).toBuilder().wintertodtEnergy(26).build();
        assertEquals(ObjectiveType.CHOP, engine.decide(local, settings).getType());
    }

    @Test public void manualFinishBelowTheGoalNeverClaimsGoalReachedAndSafetyHasNoStrategicOverlay()
    {
        engine.configure(13500, true);
        WintertodtSnapshot s = scene(1, WorkingCorner.SOUTHEAST).toBuilder().wintertodtEnergy(9).build();
        WintertodtObjective o = engine.decide(s, settings);
        assertTrue(WintertodtGuidance.reassurance(s, o).contains("Finish requested at 100 / 13,500 points"));
        assertFalse(WintertodtGuidance.reassurance(s, o).contains("goal reached"));
        assertNull(engine.decide(s.toBuilder().observationTick(2).warmth(15).build(), settings).getSoloAdvice());
    }

    @Test public void completeRoundAndNewCountdownReuseUnconsumedSupplies()
    {
        WintertodtSnapshot active = scene(1, WorkingCorner.SOUTHWEST).toBuilder().wintertodtEnergy(26).rejuvenationDoses(12).build();
        assertEquals(ObjectiveType.CHOP, engine.decide(active, settings).getType());
        assertEquals(ObjectiveType.ROUND_COMPLETE, engine.decide(active.toBuilder().observationTick(2)
            .roundState(RoundState.COMPLETE).wintertodtEnergy(0).build(), settings).getType());
        for (int tick = 3; tick < 8; tick++)
        {
            assertEquals(ObjectiveType.POSITION, engine.decide(active.toBuilder().observationTick(tick)
                .roundState(RoundState.COUNTDOWN).secondsToStart(60 - tick).wintertodtEnergy(-1).build(), settings).getType());
        }
        assertNotEquals(ObjectiveType.MAKE_REJUVENATION, engine.decide(active.toBuilder().observationTick(8).wintertodtEnergy(100).build(), settings).getType());
    }

    @Test public void earningGoalWhileTravelingCancelsTheOpeningCircuit()
    {
        engine.configure(500, false); states.put(WorkingCorner.NORTHWEST, BrazierState.UNLIT);
        WintertodtSnapshot opening = scene(1, WorkingCorner.SOUTHEAST).toBuilder()
            .brazierRoute(route(WorkingCorner.NORTHWEST, 30)).build();
        assertEquals(WorkingCorner.NORTHWEST, engine.decide(opening, settings).getDestination());
        WintertodtSnapshot goal = scene(2, WorkingCorner.SOUTHEAST).toBuilder().points(500).wintertodtEnergy(20).build();
        WintertodtObjective finish = engine.decide(goal, settings);
        assertEquals(SoloPhase.FINISH, engine.getPhase());
        assertEquals("solo_finish_drain", finish.getReason());
        assertTrue(WintertodtGuidance.reassurance(goal, finish).contains("goal reached"));
        assertNotEquals(WorkingCorner.NORTHWEST, finish.getDestination());
    }
}
