/*
 * Copyright (c) 2018, terminatusx <jbfleischman@gmail.com>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * Copyright (c) 2020, loldudester <HannahRyanster@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.wintertodtautopilot;

import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ItemID;

public final class WintertodtConstants
{
    public static final int REGION = 6462;
    public static final int ROOT_POINTS = 10;
    public static final int KINDLING_POINTS = 25;
    public static final int LIGHT_POINTS = 25;
    public static final int REPAIR_POINTS = 25;
    public static final int HEAL_POINTS = 75;
    private WintertodtConstants() { }
    public static int doses(int id)
    {
        switch (id)
        {
            case ItemID.WINT_POTION4: return 4;
            case ItemID.WINT_POTION3: return 3;
            case ItemID.WINT_POTION2: return 2;
            case ItemID.WINT_POTION1: return 1;
            default: return 0;
        }
    }
    public static PlayerActivity activity(int animation)
    {
        switch (animation)
        {
			case AnimationID.HUMAN_WOODCUTTING_BRONZE_AXE:
			case AnimationID.HUMAN_WOODCUTTING_IRON_AXE:
			case AnimationID.HUMAN_WOODCUTTING_STEEL_AXE:
			case AnimationID.HUMAN_WOODCUTTING_BLACK_AXE:
			case AnimationID.HUMAN_WOODCUTTING_MITHRIL_AXE:
			case AnimationID.HUMAN_WOODCUTTING_ADAMANT_AXE:
			case AnimationID.HUMAN_WOODCUTTING_RUNE_AXE:
			case AnimationID.HUMAN_WOODCUTTING_GILDED_AXE:
			case AnimationID.HUMAN_WOODCUTTING_DRAGON_AXE:
			case AnimationID.HUMAN_WOODCUTTING_TRAILBLAZER_AXE_NO_INFERNAL:
			case AnimationID.HUMAN_WOODCUTTING_INFERNAL_AXE:
			case AnimationID.HUMAN_WOODCUTTING_3A_AXE:
			case AnimationID.HUMAN_WOODCUTTING_CRYSTAL_AXE:
			case AnimationID.HUMAN_OPENHEAVYCHEST:
			case AnimationID.HUMAN_WOODCUTTING_TRAILBLAZER_RELOADED_AXE_NO_INFERNAL:
			case AnimationID.HUMAN_WOODCUTTING_TRAILBLAZER_AXE:
			case AnimationID.HUMAN_WOODCUTTING_TRAILBLAZER_RELOADED_AXE:
			case AnimationID.FORESTRY_2H_AXE_CHOPPING_BRONZE:
			case AnimationID.FORESTRY_2H_AXE_CHOPPING_IRON:
			case AnimationID.FORESTRY_2H_AXE_CHOPPING_STEEL:
			case AnimationID.FORESTRY_2H_AXE_CHOPPING_BLACK:
			case AnimationID.FORESTRY_2H_AXE_CHOPPING_MITHRIL:
			case AnimationID.FORESTRY_2H_AXE_CHOPPING_ADAMANT:
			case AnimationID.FORESTRY_2H_AXE_CHOPPING_RUNE:
			case AnimationID.FORESTRY_2H_AXE_CHOPPING_DRAGON:
			case AnimationID.FORESTRY_2H_AXE_CHOPPING_CRYSTAL:
			case AnimationID.FORESTRY_2H_AXE_CHOPPING_CRYSTAL_INACTIVE:
			case AnimationID.FORESTRY_2H_AXE_CHOPPING_3A:
                return PlayerActivity.CHOPPING;
            case AnimationID.HUMAN_FLETCHING: return PlayerActivity.FLETCHING;
            case AnimationID.HUMAN_PICKUPTABLE: return PlayerActivity.FEEDING;
            case AnimationID.HUMAN_CREATEFIRE:
            case AnimationID.WINT_LIGHT_BRAZIER_BRUMA: return PlayerActivity.LIGHTING;
            case AnimationID.HUMAN_POH_BUILD:
            case AnimationID.HUMAN_POH_BUILD_IMCANDO_HAMMER: return PlayerActivity.REPAIRING;
            default: return PlayerActivity.IDLE;
        }
    }
}
