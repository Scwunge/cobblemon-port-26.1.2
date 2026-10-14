/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.duck.VillagerNameHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the villager's name on its render state, see {@link VillagerNameHolder}. */
public class VillagerRendererMixin {
    @Mixin(VillagerRenderer.class)
    public static class Villagers {
        @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/npc/villager/Villager;Lnet/minecraft/client/renderer/entity/state/VillagerRenderState;F)V", at = @At("TAIL"))
        private void cobblemon$keepName(Villager entity, VillagerRenderState state, float partialTicks, CallbackInfo ci) {
            ((VillagerNameHolder) state).cobblemon$setEntityName(ChatFormatting.stripFormatting(entity.getName().getString()));
        }
    }

    @Mixin(ZombieVillagerRenderer.class)
    public static class ZombieVillagers {
        @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/zombie/ZombieVillager;Lnet/minecraft/client/renderer/entity/state/ZombieVillagerRenderState;F)V", at = @At("TAIL"))
        private void cobblemon$keepName(ZombieVillager entity, ZombieVillagerRenderState state, float partialTicks, CallbackInfo ci) {
            ((VillagerNameHolder) state).cobblemon$setEntityName(ChatFormatting.stripFormatting(entity.getName().getString()));
        }
    }
}
