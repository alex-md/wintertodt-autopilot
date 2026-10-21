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
    private final WintertodtCornerTracker corners = new WintertodtCornerTracker();
    private final WintertodtActionTracker actions = new WintertodtActionTracker();
    private PlayerActivity activity = PlayerActivity.IDLE;
    private int lastActionTick = -100;
    private int interruptedAt = -100;
    private boolean interrupted;
    private int warmthRequestedAt = -1;
    private int warmthRequestedItem = -1;
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
            .brazierDistance(objects.brazierDistance(player))
            .canMixPotion(canMixPotion).firemakingLevel(client.getRealSkillLevel(Skill.FIREMAKING));
        int runEnergy = client.getEnergy();
        b.runEnergyPercent(runEnergy >= 0 && runEnergy <= 10000 ? runEnergy / 100 : -1);
        inventory.fill(b);
        // Region 6462 includes the safe preparation area INSIDE the Doors of Dinh.
        // Do not mistake that area for an outside lobby when joining mid-round.
        rounds.fill(b, true, objects.litCount());
        WintertodtSnapshot partial = b.build();
        boolean boundary = partial.active() != previous.active();
        if (boundary)
        {
            actions.reset();
            activity = PlayerActivity.IDLE;
            interrupted = false;
            lastActionTick = tick;
            warmthRequestedAt = -1;
        }
        if (boundary) { corners.reset(); }
        b.observationTick(tick).corners(corners.observe(tick, objects, npcs, partial.active() ? client : null));
        int rawWarmth = client.getVarbitValue(VarbitID.WINT_WARMTH);
        int warmth = rawWarmth >= 0 && rawWarmth <= 1000 && (partial.active() || rawWarmth > 0) ? rawWarmth / 10 : -1;
        NPC pyro = npcs.target(TargetKind.PYROMANCER, corner.location());
        b.warmth(warmth).rawWarmth(rawWarmth).localPyromancerKnown(pyro != null)
            .localPyromancerIncapacitated(pyro != null && WintertodtNpcTracker.id(pyro) == NpcID.WINT_WIZARD_DOWN);
        boolean moving = previous.getPlayerLocation() != null && !location.equals(previous.getPlayerLocation());
        PlayerActivity animation = WintertodtConstants.activity(player.getAnimation());
        if (tick - interruptedAt > 1 && animation != PlayerActivity.IDLE) { setActivity(animation); }
        boolean consumedRequestedItem = warmthRequestedAt >= 0 && inventory.quantityDecreased(warmthRequestedItem);
        boolean bagChanged = inventory.changed();
        if (activity == PlayerActivity.RESTORING_WARMTH) { activity = PlayerActivity.IDLE; }
        WintertodtSnapshot.WarmthAction warmthAction = WintertodtSnapshot.WarmthAction.NONE;
        if (warmthRequestedAt >= 0)
        {
            warmthAction = tick >= warmthRequestedAt && tick - warmthRequestedAt <= 1
                ? WintertodtSnapshot.WarmthAction.REQUESTED : WintertodtSnapshot.WarmthAction.UNCONFIRMED;
        }
        if (!boundary && partial.active() && partial.isInventoryKnown() && previous.isInventoryKnown())
        {
            if (partial.getBrumaRoots() > previous.getBrumaRoots()) { setActivity(PlayerActivity.CHOPPING); }
            else if (partial.getBrumaKindling() > previous.getBrumaKindling()
                && partial.getBrumaRoots() < previous.getBrumaRoots()) { setActivity(PlayerActivity.FLETCHING); }
            else if (partial.getPoints() > previous.getPoints()
                && partial.getBrumaRoots() + partial.getBrumaKindling() < previous.getBrumaRoots() + previous.getBrumaKindling())
            { setActivity(PlayerActivity.FEEDING); }
            if (partial.getRejuvenationDoses() > previous.getRejuvenationDoses()
                && partial.getUnfinishedPotionCount() < previous.getUnfinishedPotionCount()) { setActivity(PlayerActivity.MAKING_POTION); }
            boolean consumed = partial.getRejuvenationDoses() < previous.getRejuvenationDoses()
                || consumedRequestedItem;
            // Inventory change alone also covers healing a pyro or obtaining a root.
            // Only a consumption signal together with a real warmth rise confirms restoration.
            if (warmthAction == WintertodtSnapshot.WarmthAction.REQUESTED
                && previous.getWarmth() >= 0 && warmth > previous.getWarmth() && consumed && bagChanged)
            {
                setActivity(PlayerActivity.RESTORING_WARMTH);
                warmthAction = WintertodtSnapshot.WarmthAction.CONFIRMED;
                warmthRequestedAt = -1;
            }
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
        b.warmthAction(warmthAction).currentActivity(moving ? PlayerActivity.MOVING : activity).playerIdle(idle).interrupted(interrupted)
            .inactiveTicks(Math.max(0, tick - lastActionTick))
            .interaction(interaction == null || interaction.getName() == null ? "None" : interaction.getName());
        WintertodtSnapshot observed = actions.observe(previous, b.build(), animation);
        if (observed.getActionActivity() == PlayerActivity.HEALING && observed.getActionCorner() != WorkingCorner.AUTOMATIC
            && observed.isInventoryKnown() && previous.isInventoryKnown()
            && observed.getRejuvenationDoses() < previous.getRejuvenationDoses())
        {
            WorkingCorner healed = observed.getActionCorner();
            corners.invalidateHealth(healed);
            java.util.List<CornerObservation> updated = new java.util.ArrayList<>();
            for (CornerObservation c : observed.getCorners())
            {
                updated.add(c.getCorner() != healed ? c : new CornerObservation(c.getCorner(), c.getBrazierState(),
                    c.getPyromancerState(), -1, c.getBrazierSource(), c.getPyromancerSource(), c.getBrazierObservedTick(),
                    c.getPyromancerObservedTick(), c.isBrazierVisible(), c.isPyromancerVisible(), -1));
            }
            observed = observed.toBuilder().clearCorners().corners(updated).build();
        }
        previous = observed;
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
        if (option.equals("eat") || option.equals("drink") && target.contains("rejuvenation"))
        { warmthRequestedAt = client.getTickCount(); warmthRequestedItem = event.getItemId(); }
        else { warmthRequestedAt = -1; }
        PlayerActivity requested = PlayerActivity.IDLE;
        WorkingCorner destination = clickedCorner == null ? WorkingCorner.AUTOMATIC : WorkingCorner.nearest(clickedCorner);
        if (option.equals("eat") || option.equals("drink") && target.contains("rejuvenation")) { requested = PlayerActivity.RESTORING_WARMTH; }
        else if (target.contains("pyromancer") && (option.equals("help") || option.equals("use")))
        { requested = PlayerActivity.HEALING; destination = npcs.cornerForIndex(event.getId()); }
        else if (target.contains("brazier"))
        {
            if (option.equals("light")) { requested = PlayerActivity.LIGHTING; }
            else if (option.equals("fix") || option.equals("repair")) { requested = PlayerActivity.REPAIRING; }
            else if (option.equals("feed") || option.equals("use")) { requested = PlayerActivity.FEEDING; }
        }
        else if (target.contains("bruma root") && option.contains("chop")) { requested = PlayerActivity.CHOPPING; }
        else if (target.contains("bruma root") && option.equals("use")) { requested = PlayerActivity.FLETCHING; }
        else if (target.contains("brew'ma") || option.equals("use") && (target.contains("bruma herb") || target.contains("unfinished rejuvenation")))
        { requested = PlayerActivity.MAKING_POTION; }
        actions.request(requested, destination, previous, client.getTickCount());
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
        warmthRequestedAt = -1;
        warmthRequestedItem = -1;
        previous = WintertodtSnapshot.builder().build();
        actions.reset(); inventory.reset(); rounds.reset(); objects.resetCorner(); corners.reset();
    }
}
