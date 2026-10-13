package com.wintertodtautopilot;

import net.runelite.client.config.*;

@ConfigGroup("wintertodtautopilot")
public interface WintertodtAutopilotConfig extends Config
{
    @ConfigSection(name = "Your plan", description = "Strategy and resource goals", position = 0)
    String plan = "plan";
    @ConfigSection(name = "Warmth", description = "Recovery and warning thresholds", position = 1)
    String warmth = "warmth";
    @ConfigSection(name = "Guidance", description = "Overlay, targets and walking cues", position = 2)
    String guidance = "guidance";
    @ConfigSection(name = "Notifications", description = "Attention alerts", position = 3, closedByDefault = true)
    String notifications = "notifications";
    @ConfigSection(name = "Advanced", description = "Diagnostics for live validation", position = 4, closedByDefault = true)
    String advanced = "advanced";

    @ConfigItem(position = 0, keyName = "strategy", name = "Strategy", description = "Mass-world action priorities", section = plan)
    default WintertodtStrategy strategy() { return WintertodtStrategy.MASS_BALANCED; }

    @ConfigItem(position = 1, keyName = "workingCorner", name = "Working brazier", description = "Automatic follows your established corner", section = plan)
    default WorkingCorner workingCorner() { return WorkingCorner.AUTOMATIC; }

    @ConfigItem(position = 2, keyName = "pointGoal", name = "Reward goal", description = "Continue earning after securing a goal; use it to plan useful batches", section = plan)
    default PointGoal pointGoal() { return PointGoal.MINIMUM_500; }

    @ConfigItem(position = 3, keyName = "fletchMode", name = "Fletch roots", description = "Automatic uses your strategy; ending rounds may require feeding immediately", section = plan)
    default FletchMode fletchMode() { return FletchMode.AUTOMATIC; }

    @ConfigItem(position = 4, keyName = "warmthWarning", name = "Warmth warning", description = "Color the warmth display below this percentage", section = warmth)
    @Range(min = 1, max = 100)
    default int warmthWarning() { return 50; }

    @ConfigItem(position = 5, keyName = "restoreWarmth", name = "Restore warmth at", description = "Interrupt routine actions below this percentage", section = warmth)
    @Range(min = 1, max = 100)
    default int restoreWarmth() { return 50; }

    @ConfigItem(position = 6, keyName = "emergencyWarmth", name = "Emergency warmth at", description = "Strong warning below this percentage", section = warmth)
    @Range(min = 1, max = 100)
    default int emergencyWarmth() { return 20; }

    @ConfigItem(position = 7, keyName = "highlightTarget", name = "Highlight current objective", description = "Outline the single scene target", section = guidance)
    default boolean highlightTarget() { return true; }

    @ConfigItem(position = 8, keyName = "highlightInventory", name = "Highlight inventory items", description = "Outline items needed for the current action", section = guidance)
    default boolean highlightInventory() { return true; }

    @ConfigItem(position = 9, keyName = "showHintArrow", name = "Show hint arrow", description = "Show an arrow when another plugin or the game has no active arrow", section = guidance)
    default boolean showHintArrow() { return true; }

    @ConfigItem(position = 10, keyName = "showPath", name = "Show walking path", description = "Draw a route through loaded walkable tiles to the current target", section = guidance)
    default boolean showPath() { return true; }

    @ConfigItem(position = 11, keyName = "showOverlay", name = "Show main overlay", description = "Movable next-action panel", section = guidance)
    default boolean showOverlay() { return true; }

    @ConfigItem(position = 12, keyName = "overlayDetail", name = "Overlay detail", description = "Amount of supporting information", section = guidance)
    default OverlayDetail overlayDetail() { return OverlayDetail.NORMAL; }

    @ConfigItem(position = 13, keyName = "notifyIdle", name = "Notify on idle", description = "Notify once per attention episode after the inactivity delay", section = notifications)
    default boolean notifyIdle() { return false; }

    @ConfigItem(position = 14, keyName = "notifyLowWarmth", name = "Notify low warmth", description = "Notify on emergency warmth with a cooldown", section = notifications)
    default boolean notifyLowWarmth() { return true; }

    @ConfigItem(position = 15, keyName = "notifyRoundStart", name = "Notify round start", description = "Notify when a round starts in five seconds", section = notifications)
    default boolean notifyRoundStart() { return false; }

    @ConfigItem(position = 16, keyName = "notifyStateChange", name = "Notify important changes", description = "Notify about broken braziers and downed pyromancers after inactivity", section = notifications)
    default boolean notifyStateChange() { return false; }

    @ConfigItem(position = 17, keyName = "inactivitySeconds", name = "Attention delay", description = "Seconds without meaningful action before attention notifications", section = notifications)
    @Range(min = 1, max = 60)
    default int inactivitySeconds() { return 6; }

    @ConfigItem(position = 18, keyName = "debug", name = "Debug overlay and logs", description = "Show observed state and log reasons when objectives change", section = advanced)
    default boolean debug() { return false; }

    @ConfigItem(position = 19, keyName = "highlightStandingTiles", name = "Highlight standing tiles", description = "Mark the walkable interaction tile and nearby route tiles", section = guidance)
    default boolean highlightStandingTiles() { return true; }

    @ConfigItem(position = 20, keyName = "pulseInterruptions", name = "Pulse attention cues", description = "Pulse borders for stopped actions, repairs, relighting, low warmth and urgent feeding", section = guidance)
    default boolean pulseInterruptions() { return true; }

}
