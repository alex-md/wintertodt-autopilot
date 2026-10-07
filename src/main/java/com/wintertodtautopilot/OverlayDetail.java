package com.wintertodtautopilot;

public enum OverlayDetail
{
    MINIMAL("Minimal"),
    NORMAL("Normal"),
    DETAILED("Detailed");
    private final String label;
    OverlayDetail(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
