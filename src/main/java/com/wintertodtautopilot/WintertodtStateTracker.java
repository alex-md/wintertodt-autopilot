package com.wintertodtautopilot;

import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.util.Text;

@Singleton
public class WintertodtStateTracker
{
    @Inject private Client client;
    @Inject private WintertodtInventoryTracker inventory;
    @Inject private WintertodtObjectTracker objects;
    @Inject private WintertodtNpcTracker npcs;
    @Inject private WintertodtRoundTracker rounds;
    private PlayerActivity activity = PlayerActivity.IDLE;
    private int lastActionTick = -100;
    private int interruptedAt = -100;
    private boolean interrupted;
    private WorldPoint clickedCorner;
    private Boolean canMixPotion;
    private WintertodtSnapshot previous = WintertodtSnapshot.builder().build();

    public WintertodtSnapshot snapshot(WorkingCorner configured)
    {
        Player player = client.getLocalPlayer();
        if (player == null || player.getWorldLocation().getRegionID() != WintertodtConstants.REGION)
        {
            reset();
            return previous;
        }
        int tick = client.getTickCount();
        WorldPoint location = player.getWorldLocation();
        WorkingCorner corner = objects.select(location, configured, clickedCorner);
        clickedCorner = null;
        if (canMixPotion == null) { canMixPotion = Quest.DRUIDIC_RITUAL.getState(client) == QuestState.FINISHED; }
        WintertodtSnapshot.WintertodtSnapshotBuilder b = WintertodtSnapshot.builder().playerLocation(location)
            .workingCorner(corner).localBrazierState(objects.brazier(location))
            .canMixPotion(canMixPotion).firemakingLevel(client.getRealSkillLevel(Skill.FIREMAKING));
        inventory.fill(b);
        // Region 6462 includes the safe preparation area INSIDE the Doors of Dinh.
        // Do not mistake that area for an outside lobby when joining mid-round.
        rounds.fill(b, true, objects.litCount());
        WintertodtSnapshot partial = b.build();
        boolean boundary = partial.active() != previous.active();
        if (boundary)
        {
            activity = PlayerActivity.IDLE;
            interrupted = false;
            lastActionTick = tick;
        }
        int warmth = partial.active() ? Math.max(0, Math.min(100, client.getVarbitValue(VarbitID.WINT_WARMTH) / 10)) : -1;
        NPC pyro = npcs.target(TargetKind.PYROMANCER, corner.location());
        b.warmth(warmth).localPyromancerKnown(pyro != null)
            .localPyromancerIncapacitated(pyro != null && WintertodtNpcTracker.id(pyro) == NpcID.WINT_WIZARD_DOWN);
        boolean moving = previous.getPlayerLocation() != null && !location.equals(previous.getPlayerLocation());
        PlayerActivity animation = WintertodtConstants.activity(player.getAnimation());
        if (tick - interruptedAt > 1 && animation != PlayerActivity.IDLE) { setActivity(animation); }
        boolean bagChanged = inventory.changed();
        if (!boundary && partial.active())
        {
            if (partial.getBrumaRoots() > previous.getBrumaRoots()) { setActivity(PlayerActivity.CHOPPING); }
            else if (partial.getBrumaKindling() > previous.getBrumaKindling()
                && partial.getBrumaRoots() < previous.getBrumaRoots()) { setActivity(PlayerActivity.FLETCHING); }
            else if (partial.getPoints() > previous.getPoints()
                && partial.getBrumaRoots() + partial.getBrumaKindling() < previous.getBrumaRoots() + previous.getBrumaKindling())
            { setActivity(PlayerActivity.FEEDING); }
            if (partial.getRejuvenationDoses() > previous.getRejuvenationDoses()
                && partial.getUnfinishedPotionCount() < previous.getUnfinishedPotionCount()) { setActivity(PlayerActivity.MAKING_POTION); }
            if (warmth > previous.getWarmth() + 10 && bagChanged) { setActivity(PlayerActivity.RESTORING_WARMTH); }
        }
        if (moving) { lastActionTick = tick; }
        if (activity == PlayerActivity.FLETCHING && partial.getBrumaRoots() == 0
            || activity == PlayerActivity.FEEDING && partial.getBrumaRoots() + partial.getBrumaKindling() == 0
            || tick - lastActionTick >= 5)
        {
            activity = PlayerActivity.IDLE;
        }
        Actor interaction = player.getInteracting();
        boolean idle = !moving && activity == PlayerActivity.IDLE && interaction == null && tick - lastActionTick >= 5;
        b.currentActivity(moving ? PlayerActivity.MOVING : activity).playerIdle(idle).interrupted(interrupted)
            .inactiveTicks(Math.max(0, tick - lastActionTick))
            .interaction(interaction == null || interaction.getName() == null ? "None" : interaction.getName());
        previous = b.build();
        return previous;
    }
    public void animation(Actor actor)
    {
        if (actor != client.getLocalPlayer() || previous.getRoundState() == RoundState.OUTSIDE) { return; }
        PlayerActivity observed = WintertodtConstants.activity(actor.getAnimation());
        if (observed != PlayerActivity.IDLE && client.getTickCount() - interruptedAt > 1) { setActivity(observed); }
    }
    private void setActivity(PlayerActivity value)
    {
        activity = value;
        lastActionTick = client.getTickCount();
        interrupted = false;
    }
    public void clicked(MenuOptionClicked event)
    {
        if (previous.getRoundState() == RoundState.OUTSIDE) { return; }
        lastActionTick = client.getTickCount();
        String option = Text.removeTags(event.getMenuOption()).toLowerCase(Locale.ROOT);
        String target = Text.removeTags(event.getMenuTarget()).toLowerCase(Locale.ROOT);
        if (target.contains("bruma root") || target.contains("brazier"))
        {
            MenuAction action = event.getMenuAction();
            if (action == MenuAction.GAME_OBJECT_FIRST_OPTION || action == MenuAction.GAME_OBJECT_SECOND_OPTION
                || action == MenuAction.GAME_OBJECT_THIRD_OPTION || action == MenuAction.GAME_OBJECT_FOURTH_OPTION)
            {
                clickedCorner = WorldPoint.fromScene(client.getTopLevelWorldView(), event.getParam0(), event.getParam1(), client.getTopLevelWorldView().getPlane());
            }
        }
        if (option.equals("eat") || option.equals("drink") && target.contains("rejuvenation")) { setActivity(PlayerActivity.RESTORING_WARMTH); }
        else if (target.contains("pyromancer") && (option.equals("help") || option.equals("use"))) { setActivity(PlayerActivity.HEALING); }
    }
    public void chat(ChatMessage event)
    {
        if (!previous.active() || event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM) { return; }
        String text = Text.removeTags(event.getMessage());
        if (text.startsWith("You carefully fletch the root")) { setActivity(PlayerActivity.FLETCHING); return; }
        boolean damage = text.startsWith("The cold of") || text.startsWith("The freezing cold attack")
            || text.startsWith("The brazier is broken and shrapnel");
        boolean stopped = text.startsWith("You have run out of bruma roots") || text.startsWith("Your inventory is too full")
            || text.startsWith("The brazier has gone out.");
        if (stopped || damage && activity != PlayerActivity.CHOPPING && activity != PlayerActivity.IDLE)
        {
            activity = PlayerActivity.IDLE;
            interrupted = true;
            interruptedAt = client.getTickCount();
            // Grace period still protects idle emphasis from animation gaps.
            lastActionTick = interruptedAt - 3;
        }
        if (text.startsWith("You fix the brazier") || text.startsWith("You light the brazier"))
        {
            activity = PlayerActivity.IDLE; lastActionTick = client.getTickCount();
        }
    }
    public void reset()
    {
        activity = PlayerActivity.IDLE; lastActionTick = -100; interruptedAt = -100;
        interrupted = false; clickedCorner = null; canMixPotion = null;
        previous = WintertodtSnapshot.builder().build();
        inventory.reset(); rounds.reset(); objects.resetCorner();
    }
}
