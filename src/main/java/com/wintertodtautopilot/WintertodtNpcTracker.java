package com.wintertodtautopilot;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.inject.Singleton;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;

@Singleton
public class WintertodtNpcTracker
{
    private final Set<NPC> npcs = Collections.newSetFromMap(new IdentityHashMap<>());
    public void add(NPC npc)
    {
        if (relevant(npc.getId()) || relevant(id(npc))) { npcs.add(npc); }
    }
    public void remove(NPC npc) { npcs.remove(npc); }
    public void reset() { npcs.clear(); }
    private static boolean relevant(int id)
    {
        return id == NpcID.WINT_WIZARD || id == NpcID.WINT_WIZARD_DOWN || id == NpcID.WINT_BREWMA;
    }
    public static int id(NPC npc)
    {
        NPCComposition composition = npc.getTransformedComposition();
        return composition == null ? -1 : composition.getId();
    }
    public NPC target(TargetKind target, WorldPoint anchor)
    {
        NPC result = null;
        int distance = Integer.MAX_VALUE;
        for (NPC npc : npcs)
        {
            int id = id(npc);
            if (target == TargetKind.BREWMA ? id != NpcID.WINT_BREWMA
                : id != NpcID.WINT_WIZARD && id != NpcID.WINT_WIZARD_DOWN) { continue; }
            int d = npc.getWorldLocation().distanceTo(anchor);
            if (target == TargetKind.PYROMANCER && d > 8) { continue; }
            if (d < distance) { result = npc; distance = d; }
        }
        return result;
    }
}
