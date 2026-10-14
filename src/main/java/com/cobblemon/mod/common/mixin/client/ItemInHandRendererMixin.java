/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.OrientationControllable;
import com.cobblemon.mod.common.client.render.player.FirstPersonRecallBallRenderer;
import com.cobblemon.mod.common.item.PokedexItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import com.cobblemon.mod.common.client.render.legacy.DeferredBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Inject(method = "renderArmWithItem", at = @At(value = "HEAD"), cancellable = true)
    private void cobblemon$renderArmWithItem(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int j, CallbackInfo ci) {
        // First person hands are submitted since 1.21.9; the recall ball still draws into buffers, which are submitted for it
        MultiBufferSource multiBufferSource = new DeferredBufferSource(submitNodeCollector);
        if (abstractClientPlayer.isUsingItem() && abstractClientPlayer.getUseItem().getItem() instanceof PokedexItem) {
            ci.cancel();
        }
        else if (interactionHand == InteractionHand.MAIN_HAND && FirstPersonRecallBallRenderer.INSTANCE.render(poseStack, multiBufferSource, j, f)) {
            ci.cancel();
        }
        else if (abstractClientPlayer instanceof OrientationControllable controllable && controllable.getOrientationController().isActive()) {

        }
    }

    // PoseStack#mulPose takes a Quaternionfc since 1.21.5
    @Redirect(method = "renderHandsWithItems", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"))
    private void cobblemon$renderHandswithItems(PoseStack instance, Quaternionfc quaternion) {
        if (!(Minecraft.getInstance().player != null && Minecraft.getInstance().player.getVehicle() instanceof OrientationControllable controllable && controllable.getOrientationController().isActive())) {
            instance.mulPose(quaternion);
        }
    }
}
