package com.wintertodtautopilot;

import net.runelite.api.coords.WorldPoint;

public enum WorkingCorner
{
    AUTOMATIC(0, 0), NORTHWEST(1621, 4016), NORTHEAST(1639, 4016),
    SOUTHWEST(1621, 3998), SOUTHEAST(1639, 3998);
    private final int x, y;
    WorkingCorner(int x, int y) { this.x = x; this.y = y; }
    @Override public String toString()
    {
        String name = name().toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    public WorldPoint location() { return new WorldPoint(x, y, 0); }
    public static WorkingCorner nearest(WorldPoint at)
    {
        WorkingCorner result = SOUTHWEST;
        int distance = Integer.MAX_VALUE;
        for (WorkingCorner corner : values())
        {
            if (corner == AUTOMATIC) { continue; }
            int d = corner.location().distanceTo(at);
            if (d < distance) { distance = d; result = corner; }
        }
        return result;
    }
}
