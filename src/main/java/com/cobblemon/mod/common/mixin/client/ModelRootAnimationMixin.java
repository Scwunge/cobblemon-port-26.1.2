/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.client.render.player.MountedPlayerRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin adds to the head of the renderToBuffer function and allows us to detect if it is a HumanoidModel and
 * is being rendered as part of a player in the world who is riding a PokemonEntity. If so we need to obey any
 * root animations that have been applied earlier.
 *
 * Humanoid models were AgeableListModels until 1.21.2; every model renders through Model#renderToBuffer now. The model
 * may be rendered more than once from the same pose stack (outlines, block breaking), so the root transform is pushed
 * and popped around each render rather than left on the stack.
 *
 * @author Hiroku
 * @since May 16, 2025
 */
@Mixin(Model.class)
public class ModelRootAnimationMixin {
    @Unique
    private boolean cobblemon$pushedRootAnimation = false;

    @Inject(method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", at = @At("HEAD"))
    private void cobblemon$renderToBuffer(
        PoseStack poseStack,
        VertexConsumer vertexConsumer,
        int packedLight,
        int packedOverlay,
        int colour,
        CallbackInfo ci
    ) {
        Model<?> model = (Model<?>) (Object) this;
        cobblemon$pushedRootAnimation = model instanceof HumanoidModel<?> && MountedPlayerRenderer.shouldApplyRootAnimation;
        if (cobblemon$pushedRootAnimation) {
            poseStack.pushPose();
            MountedPlayerRenderer.INSTANCE.animateRoot(poseStack);
        }
    }

    @Inject(method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", at = @At("TAIL"))
    private void cobblemon$popRootAnimation(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int colour, CallbackInfo ci) {
        if (cobblemon$pushedRootAnimation) {
            poseStack.popPose();
            cobblemon$pushedRootAnimation = false;
        }
    }
}
