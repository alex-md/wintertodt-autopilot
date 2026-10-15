package com.wintertodtautopilot;

public enum SoloPhase
{
    PREPARE("Prepare"), REDUCE_ENERGY("Reduce energy"), SUSTAIN("Sustain"),
    RECOVER_ENERGY("Recover energy"), FINISH("Finish"), REASSESS("Check observations");
    private final String label;
    SoloPhase(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
