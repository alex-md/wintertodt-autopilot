package com.wintertodtautopilot;

public enum WintertodtMode
{
    MASS("Mass"), SOLO("Solo (experimental)");
    private final String label;
    WintertodtMode(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
