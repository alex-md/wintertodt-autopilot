package com.wintertodtautopilot;

public enum WintertodtStrategy
{
    MASS_BALANCED("Mass - Balanced"),
    MASS_XP("Mass - Firemaking XP"),
    MASS_POINTS("Mass - Points / Rewards"),
    LOW_ATTENTION("Low Attention");
    private final String label;
    WintertodtStrategy(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
