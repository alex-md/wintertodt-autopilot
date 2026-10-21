package com.wintertodtautopilot;

import net.runelite.client.config.*;

@ConfigGroup("wintertodtautopilot")
public interface WintertodtAutopilotConfig extends Config
{
    @ConfigSection(name = "Your plan", description = "Choose your mode and shared preferences.", position = 0)
    String plan = "plan";
    @ConfigSection(name = "Mass settings", description = "Strategy and reward goal for mass worlds. Used only in Mass mode.", position = 1)
    String mass = "mass";
    @ConfigSection(name = "Solo settings", description = "Strategy, point goal and finishing controls. Used only in Solo mode.", position = 2, closedByDefault = true)
    String solo = "solo";
    @ConfigSection(name = "Warmth", description = "Optional recovery thresholds. The defaults suit mass worlds.", position = 3, closedByDefault = true)
    String warmth = "warmth";
    @ConfigSection(name = "On-screen guidance", description = "Choose how much help appears in the game", position = 4)
    String guidance = "guidance";
    @ConfigSection(name = "Notifications", description = "Attention alerts", position = 5, closedByDefault = true)
    String notifications = "notifications";
    @ConfigSection(name = "Advanced", description = "Diagnostics for live validation", position = 6, closedByDefault = true)
    String advanced = "advanced";

    @ConfigItem(position = -1, keyName = "pauseGuidance", name = "Pause guidance",
        description = "Pause all guidance and notifications, including warmth warnings. Uncheck to resume. This choice is saved.", section = plan)
    default boolean pauseGuidance() { return false; }

    @ConfigItem(position = -2, keyName = "mode", name = "Mode",
        description = "Mass or Solo guidance. Solo assumes you chose an empty world; it cannot guarantee exclusive control.", section = plan)
    default WintertodtMode mode() { return WintertodtMode.MASS; }

    @ConfigItem(position = 0, keyName = "soloStrategy", name = "Solo strategy",
        description = "Points / Rewards fletches roots. Firemaking XP feeds roots. Fletch roots can override either choice.", section = solo)
    default SoloStrategy soloStrategy() { return SoloStrategy.POINTS; }

    @ConfigItem(position = 1, keyName = "soloPointGoal", name = "Solo point goal",
        description = "Solo finishes when this many points are earned. Carried fuel must be fed first. This is a score target, not a reward-cap estimate.", section = solo)
    @Range(min = 500, max = 100000)
    default int soloPointGoal() { return 5000; }

    @ConfigItem(position = 2, keyName = "soloEffort", name = "Solo effort",
        description = "Comfortable finishes batches, avoids optional healing and finishes with one brazier. Active Points adds observed healing opportunities; Active finishes with an adjacent pair when run energy is known and above 10%.", section = solo)
    default SoloEffort soloEffort() { return SoloEffort.COMFORTABLE; }

    @ConfigItem(position = 3, keyName = "finishSoloRound", name = "Finish this Solo round",
        description = "Finish the current Solo round early. Once requested, it stays active until the round ends, you change mode or hop worlds.", section = solo)
    default boolean finishSoloRound() { return false; }

    @ConfigItem(position = 0, keyName = "strategy", name = "Strategy", description = "Mass strategy. Balanced: all-round play. Firemaking XP: feed roots. Points / Rewards: more kindling. Low Attention: fewer task changes.", section = mass)
    default WintertodtStrategy strategy() { return WintertodtStrategy.MASS_BALANCED; }

    @ConfigItem(position = 1, keyName = "workingCorner", name = "Working brazier", description = "Automatic follows your nearby working corner in Mass and Solo, with a margin to prevent switching back and forth. Choose a brazier only to stay at a specific corner.", section = plan)
    default WorkingCorner workingCorner() { return WorkingCorner.AUTOMATIC; }

    @ConfigItem(position = 2, keyName = "pointGoal", name = "Reward goal", description = "500 points secures a reward. Guidance continues toward the next threshold after reaching your goal; carried fuel must be fed to count.", section = mass)
    default PointGoal pointGoal() { return PointGoal.MINIMUM_500; }

    @ConfigItem(position = 3, keyName = "fletchMode", name = "Fletch roots", description = "Leave Automatic selected to let your strategy decide. Always and Never override it; urgent feeding can still take priority.", section = plan)
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

    @ConfigItem(position = 9, keyName = "highlightTarget", name = "Highlight current objective", description = "Outline the single scene target", section = guidance)
    default boolean highlightTarget() { return true; }

    @ConfigItem(position = 10, keyName = "highlightInventory", name = "Highlight inventory items", description = "Outline items needed for the current action", section = guidance)
    default boolean highlightInventory() { return true; }

    @ConfigItem(position = 11, keyName = "showHintArrow", name = "Show hint arrow", description = "Show an arrow when another plugin or the game has no active arrow", section = guidance)
    default boolean showHintArrow() { return true; }

    @ConfigItem(position = 12, keyName = "showPath", name = "Show walking path", description = "Draw a route to the current target. Solo can use the northern gap at 60 Agility; you click each jump yourself.", section = guidance)
    default boolean showPath() { return false; }

    @ConfigItem(position = 7, keyName = "showOverlay", name = "Show main overlay", description = "Movable next-action panel", section = guidance)
    default boolean showOverlay() { return true; }

    @ConfigItem(position = 8, keyName = "overlayDetail", name = "Overlay detail", description = "Minimal: action and safety. Normal: adds a short explanation and finish estimate. Detailed: adds supplies and planning estimates.", section = guidance)
    default OverlayDetail overlayDetail() { return OverlayDetail.NORMAL; }

    @ConfigItem(position = 20, keyName = "notifyIdle", name = "Notify on idle", description = "Notify once per attention episode after the inactivity delay", section = notifications)
    default boolean notifyIdle() { return false; }

    @ConfigItem(position = 21, keyName = "notifyLowWarmth", name = "Notify low warmth", description = "Notify once per emergency warmth episode, with its own 30-second cooldown. Other alerts never delay this warning.", section = notifications)
    default boolean notifyLowWarmth() { return true; }

    @ConfigItem(position = 22, keyName = "notifyRoundStart", name = "Notify round start", description = "Notify when a round starts in five seconds", section = notifications)
    default boolean notifyRoundStart() { return false; }

    @ConfigItem(position = 23, keyName = "notifyStateChange", name = "Notify important changes", description = "Notify about broken braziers and downed pyromancers after inactivity", section = notifications)
    default boolean notifyStateChange() { return false; }

    @ConfigItem(position = 25, keyName = "notifyRewardSecured", name = "Notify reward secured",
        description = "Notify once when earned points reach 500 this round. Carried fuel does not count. Pausing guidance pauses this alert too.", section = notifications)
    default boolean notifyRewardSecured() { return false; }

    @ConfigItem(position = 24, keyName = "inactivitySeconds", name = "Attention delay", description = "Seconds without meaningful action before attention notifications", section = notifications)
    @Range(min = 1, max = 60)
    default int inactivitySeconds() { return 6; }

    @ConfigItem(position = 30, keyName = "debug", name = "Debug overlay and logs", description = "Show observed state, log objective changes and per-tick corner observation traces", section = advanced)
    default boolean debug() { return false; }

    @ConfigItem(position = 13, keyName = "highlightStandingTiles", name = "Highlight standing tiles", description = "Optional floor marker for the destination; off by default.", section = guidance)
    default boolean highlightStandingTiles() { return false; }

    @ConfigItem(position = 14, keyName = "pulseInterruptions", name = "Pulse attention cues", description = "Pulse borders for stopped actions, repairs, relighting, low warmth and urgent feeding", section = guidance)
    default boolean pulseInterruptions() { return true; }

    @ConfigItem(position = 15, keyName = "highlightBorderWidth", name = "Highlight border width",
        description = "Border width in pixels for scene targets and inventory items. Attention pulses change opacity, not width.", section = guidance)
    @Range(min = 1, max = 4)
    default int highlightBorderWidth() { return 1; }

}
