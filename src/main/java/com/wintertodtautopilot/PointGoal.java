package com.wintertodtautopilot;

public enum PointGoal
{
    MINIMUM_500(500), POINTS_1000(1000), POINTS_1500(1500), POINTS_2000(2000),
    NEXT_THRESHOLD(0), MAXIMUM_PRACTICAL(-1);
    private final int points;
    PointGoal(int points) { this.points = points; }
    @Override public String toString()
    {
        if (this == NEXT_THRESHOLD) { return "Next reward threshold"; }
        if (this == MAXIMUM_PRACTICAL) { return "Maximum practical"; }
        return points == 500 ? "Minimum 500" : String.format(java.util.Locale.ROOT, "%,d", points);
    }
    public int target(int current)
    {
        return points > 0 ? points : Math.max(500, (Math.max(0, current) / 500 + 1) * 500);
    }
}
