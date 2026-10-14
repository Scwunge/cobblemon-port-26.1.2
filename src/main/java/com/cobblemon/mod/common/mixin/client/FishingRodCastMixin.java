/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.FishingRodCast;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Without this, the fishing rod item model appears cast when using a PokeRod in the other hand. This was
 * ItemPropertiesMixin's hook on the fishing rod's "cast" item property, which became the fishing_rod/cast item model
 * condition in 1.21.4.
 */
@Mixin(FishingRodCast.class)
public abstract class FishingRodCastMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void cobblemon$preventRodMixup(ItemStack itemStack, ClientLevel level, LivingEntity owner, int seed, ItemDisplayContext displayContext, CallbackInfoReturnable<Boolean> cir) {
        if (owner instanceof Player player && player.fishing instanceof PokeRodFishingBobberEntity) {
            cir.setReturnValue(false);
        }
    }
}
