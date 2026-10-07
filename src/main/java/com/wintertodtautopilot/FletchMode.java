package com.wintertodtautopilot;

public enum FletchMode
{
    AUTOMATIC("Automatic"),
    ALWAYS("Always"),
    NEVER("Never");
    private final String label;
    FletchMode(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
