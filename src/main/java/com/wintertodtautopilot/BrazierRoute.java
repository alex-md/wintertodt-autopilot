package com.wintertodtautopilot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/** Current scene route evidence. Negative cost means observed unreachable; absence means unknown. */
@Value
public class BrazierRoute
{
    WorkingCorner destination;
    int walkingCost;
    List<WorldPoint> path;
    List<WorkingCorner> onTheWay;
    public BrazierRoute(WorkingCorner destination, int walkingCost, List<WorldPoint> path)
    { this(destination, walkingCost, path, Collections.emptyList()); }
    public BrazierRoute(WorkingCorner destination, int walkingCost, List<WorldPoint> path, List<WorkingCorner> onTheWay)
    {
        this.destination = destination; this.walkingCost = walkingCost;
        this.path = Collections.unmodifiableList(new ArrayList<>(path));
        this.onTheWay = Collections.unmodifiableList(new ArrayList<>(onTheWay));
    }
}
