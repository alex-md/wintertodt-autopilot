package com.wintertodtautopilot;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.HintArrowType;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

/** Only claim a free arrow; only clear the arrow we still own. */
@Singleton
public class WintertodtHintArrow
{
    @Inject private Client client;
    private NPC npc;
    private WorldPoint point;
    private boolean owns()
    {
        return npc != null && client.getHintArrowType() == HintArrowType.NPC && client.getHintArrowNpc() == npc
            || point != null && client.getHintArrowType() == HintArrowType.COORDINATE && point.equals(client.getHintArrowPoint());
    }
    public void clear()
    {
        if (owns()) { client.clearHintArrow(); }
        npc = null; point = null;
    }
    public void update(NPC nextNpc, WorldPoint nextPoint, boolean enabled)
    {
        if (nextNpc != null) { nextPoint = null; }
        if (!enabled || nextNpc == null && nextPoint == null) { clear(); return; }
        if (!owns() && client.getHintArrowType() != HintArrowType.NONE)
        {
            npc = null; point = null; return;
        }
        if (nextNpc == npc && java.util.Objects.equals(point, nextPoint) && owns()) { return; }
        clear();
        if (nextNpc != null) { npc = nextNpc; client.setHintArrow(npc); }
        else { point = nextPoint; client.setHintArrow(point); }
    }
}
