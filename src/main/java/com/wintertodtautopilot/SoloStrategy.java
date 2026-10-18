package com.wintertodtautopilot;

public enum SoloStrategy
{
    POINTS("Points / Rewards"), FIREMAKING_XP("Firemaking XP");
    private final String label;
    SoloStrategy(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
