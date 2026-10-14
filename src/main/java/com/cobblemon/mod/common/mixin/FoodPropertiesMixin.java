/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.item.LeftoversCreatedEvent;
import com.cobblemon.mod.common.api.tags.CobblemonItemTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leftovers from eating food tagged with {@link CobblemonItemTags#LEAVES_LEFTOVERS}. This was PlayerMixin's hook on
 * Player#eat, which became FoodProperties#onConsume when food became a consumable in 1.21.2. It runs right after the
 * food was eaten, as before.
 */
@Mixin(FoodProperties.class)
public abstract class FoodPropertiesMixin {
    @Inject(
        method = "onConsume",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V",
            shift = At.Shift.AFTER
        )
    )
    private void cobblemon$createLeftovers(Level level, LivingEntity user, ItemStack stack, Consumable consumable, CallbackInfo ci) {
        if (!level.isClientSide() && user instanceof ServerPlayer player) {
            if (stack.is(CobblemonItemTags.LEAVES_LEFTOVERS) && level.getRandom().nextDouble() < Cobblemon.config.getAppleLeftoversChance()) {
                ItemStack leftovers = new ItemStack(CobblemonItems.LEFTOVERS);
                CobblemonEvents.LEFTOVERS_CREATED.postThen(
                    new LeftoversCreatedEvent(player, leftovers),
                    leftoversCreatedEvent -> null,
                    leftoversCreatedEvent -> {
                        if (!player.addItem(leftoversCreatedEvent.getLeftovers())) {
                            var itemPos = player.getLookAngle().scale(0.5f).add(player.position());
                            level.addFreshEntity(new ItemEntity(level, itemPos.x(), itemPos.y(), itemPos.z(), leftoversCreatedEvent.getLeftovers()));
                        }
                        return null;
                    }
                );
            }
        }
    }
}
