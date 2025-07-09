package com.lartsal.autosell.callbacks;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface IEntityInteractionCallback {
    InteractionResult invoke(Player player, Entity entity, InteractionHand hand);
}
