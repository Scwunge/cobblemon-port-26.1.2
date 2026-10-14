/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.client.render.entity.MountedLivingEntityRenderer;
import com.cobblemon.mod.common.duck.EntityRenderStateSource;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Positions entities riding a Pokémon at their seat. The renderer only has the render state since 1.21.2; the entity
 * and partial tick come from {@link EntityRenderStateSource}, and the bob is the state's age in ticks as before.
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "setupRotations", at = @At("HEAD"))
    private void cobblemon$modifyRoll(LivingEntityRenderState state, PoseStack poseStack, float yBodyRot, float scale, CallbackInfo ci) {
        if (((EntityRenderStateSource) state).cobblemon$getEntity() instanceof LivingEntity entity && entity.isPassenger()) {
            var vehicle = entity.getVehicle();
            if (vehicle instanceof PokemonEntity pokemonEntity) {
                MountedLivingEntityRenderer.INSTANCE.render(entity, pokemonEntity, poseStack, state.ageInTicks, yBodyRot, ((EntityRenderStateSource) state).cobblemon$getPartialTick(), scale);
            }
        }
    }
}
