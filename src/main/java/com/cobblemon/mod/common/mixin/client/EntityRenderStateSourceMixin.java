/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.duck.EntityRenderStateSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** See {@link EntityRenderStateSource}. */
public class EntityRenderStateSourceMixin {
    @Mixin(EntityRenderState.class)
    public static class State implements EntityRenderStateSource {
        @Unique
        private @Nullable Entity cobblemon$entity;
        @Unique
        private float cobblemon$partialTick;

        @Override
        public @Nullable Entity cobblemon$getEntity() {
            return cobblemon$entity;
        }

        @Override
        public float cobblemon$getPartialTick() {
            return cobblemon$partialTick;
        }

        @Override
        public void cobblemon$setSource(Entity entity, float partialTick) {
            cobblemon$entity = entity;
            cobblemon$partialTick = partialTick;
        }
    }

    @Mixin(EntityRenderer.class)
    public static class Renderer {
        @Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("RETURN"))
        private void cobblemon$rememberSource(Entity entity, float partialTicks, CallbackInfoReturnable<EntityRenderState> cir) {
            ((EntityRenderStateSource) cir.getReturnValue()).cobblemon$setSource(entity, partialTicks);
        }
    }
}
