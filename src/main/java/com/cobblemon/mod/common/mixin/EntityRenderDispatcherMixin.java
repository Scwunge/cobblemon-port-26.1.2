/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.entity.PosableEntity;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws a small green box at each locator of a posable entity along with its hitbox (F3+B). Hitboxes were drawn by
 * EntityRenderDispatcher#renderHitbox until they became debug gizmos in 1.21.9.
 *
 * Cobblemon's player layers are added through Fabric's LivingEntityRenderLayerRegistrationCallback since 26.1.
 */
@Mixin(EntityHitboxDebugRenderer.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "showHitboxes", at = @At("TAIL"))
    private void renderLocators(Entity entity, float partialTicks, boolean isServerEntity, CallbackInfo ci) {
        if (isServerEntity) return;
        if (entity instanceof PosableEntity posableEntity) {
            if (posableEntity.getDelegate() instanceof PosableState state) {
                Vec3 offset = entity.getPosition(partialTicks).subtract(entity.position());
                state.getLocatorStates().forEach((locator, matrix) -> {
                    Vec3 pos = matrix.getOrigin().add(offset);
                    Gizmos.cuboid(AABB.ofSize(pos, 0.25, 0.25, 0.25), GizmoStyle.stroke(0xFF00FF00));
                });
            }
        }
    }
}
