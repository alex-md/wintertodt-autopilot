package com.wintertodtautopilot;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.Value;

@Value
public class WintertodtObjective
{
    ObjectiveType type;
    String instruction;
    String reason;
    TargetKind target;
    Set<Integer> itemIds;
    int quantity;
    boolean urgent;
    public WintertodtObjective(ObjectiveType type, String instruction, String reason,
        TargetKind target, int quantity, boolean urgent, int... items)
    {
        this.type = type; this.instruction = instruction; this.reason = reason;
        this.target = target; this.quantity = quantity; this.urgent = urgent;
        Set<Integer> ids = new HashSet<>();
        for (int id : items) { if (id >= 0) { ids.add(id); } }
        itemIds = Collections.unmodifiableSet(ids);
    }
    public boolean actionable()
    {
        return type != ObjectiveType.WAIT && type != ObjectiveType.ROUND_COMPLETE;
    }
}
