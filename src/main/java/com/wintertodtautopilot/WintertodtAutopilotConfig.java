package com.wintertodtautopilot;

import net.runelite.client.config.*;

@ConfigGroup("wintertodtautopilot")
public interface WintertodtAutopilotConfig extends Config
{
    @ConfigItem(position = 0, keyName = "strategy", name = "Strategy", description = "Mass-world action priorities")
    default WintertodtStrategy strategy() { return WintertodtStrategy.MASS_BALANCED; }

    @ConfigItem(position = 1, keyName = "workingCorner", name = "Working brazier", description = "Automatic follows your established corner")
    default WorkingCorner workingCorner() { return WorkingCorner.AUTOMATIC; }

    @ConfigItem(position = 2, keyName = "pointGoal", name = "Reward goal", description = "Continue earning after securing a goal; use it to plan useful batches")
    default PointGoal pointGoal() { return PointGoal.MINIMUM_500; }

    @ConfigItem(position = 3, keyName = "fletchMode", name = "Fletch roots", description = "Automatic uses your strategy; ending rounds may require feeding immediately")
    default FletchMode fletchMode() { return FletchMode.AUTOMATIC; }

    @ConfigItem(position = 4, keyName = "warmthWarning", name = "Warmth warning", description = "Color the warmth display below this percentage")
    @Range(min = 1, max = 100)
    default int warmthWarning() { return 50; }

    @ConfigItem(position = 5, keyName = "restoreWarmth", name = "Restore warmth at", description = "Interrupt routine actions below this percentage")
    @Range(min = 1, max = 100)
    default int restoreWarmth() { return 35; }

    @ConfigItem(position = 6, keyName = "emergencyWarmth", name = "Emergency warmth at", description = "Strong warning below this percentage")
    @Range(min = 1, max = 100)
    default int emergencyWarmth() { return 20; }

    @ConfigItem(position = 7, keyName = "highlightTarget", name = "Highlight current objective", description = "Outline the single scene target")
    default boolean highlightTarget() { return true; }

    @ConfigItem(position = 8, keyName = "highlightInventory", name = "Highlight inventory items", description = "Outline items needed for the current action")
    default boolean highlightInventory() { return true; }

    @ConfigItem(position = 9, keyName = "showHintArrow", name = "Show hint arrow", description = "Show an arrow when another plugin or the game has no active arrow")
    default boolean showHintArrow() { return true; }

    @ConfigItem(position = 10, keyName = "showPath", name = "Show walking path", description = "Draw a route through loaded walkable tiles to the current target")
    default boolean showPath() { return true; }

    @ConfigItem(position = 11, keyName = "showOverlay", name = "Show main overlay", description = "Movable next-action panel")
    default boolean showOverlay() { return true; }

    @ConfigItem(position = 12, keyName = "overlayDetail", name = "Overlay detail", description = "Amount of supporting information")
    default OverlayDetail overlayDetail() { return OverlayDetail.NORMAL; }

    @ConfigItem(position = 13, keyName = "notifyIdle", name = "Notify on idle", description = "Notify once per attention episode after the inactivity delay")
    default boolean notifyIdle() { return false; }

    @ConfigItem(position = 14, keyName = "notifyLowWarmth", name = "Notify low warmth", description = "Notify on emergency warmth with a cooldown")
    default boolean notifyLowWarmth() { return true; }

    @ConfigItem(position = 15, keyName = "notifyRoundStart", name = "Notify round start", description = "Notify when a round starts in five seconds")
    default boolean notifyRoundStart() { return false; }

    @ConfigItem(position = 16, keyName = "notifyStateChange", name = "Notify important changes", description = "Notify about broken braziers and downed pyromancers after inactivity")
    default boolean notifyStateChange() { return false; }

    @ConfigItem(position = 17, keyName = "inactivitySeconds", name = "Attention delay", description = "Seconds without meaningful action before attention notifications")
    @Range(min = 1, max = 60)
    default int inactivitySeconds() { return 6; }

    @ConfigItem(position = 18, keyName = "debug", name = "Debug overlay and logs", description = "Show observed state and log reasons when objectives change")
    default boolean debug() { return false; }

    @ConfigItem(position = 19, keyName = "highlightStandingTiles", name = "Highlight standing tiles", description = "Mark the walkable interaction tile and nearby route tiles")
    default boolean highlightStandingTiles() { return true; }

    @ConfigItem(position = 20, keyName = "pulseInterruptions", name = "Pulse interrupted actions", description = "Gently pulse relevant highlights until you restart an interrupted action")
    default boolean pulseInterruptions() { return true; }

}
