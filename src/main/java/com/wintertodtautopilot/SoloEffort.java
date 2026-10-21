package com.wintertodtautopilot;

/** Effort is independent of fuel strategy. All interactions remain manual. */
public enum SoloEffort
{
    COMFORTABLE("Comfortable"), ACTIVE("Active");
    private final String label;
    SoloEffort(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
