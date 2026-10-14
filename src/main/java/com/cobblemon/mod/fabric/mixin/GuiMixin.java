/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.mixin;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.BooleanSupplier;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to give us a hook to render the PartyOverlay below the Chat
 *
 * @author Qu
 * @since 2022-02-22
 */
@Mixin(Gui.class)
public class GuiMixin {
    private Long lastTimeMillis = null;

    @Inject(method = "extractCameraOverlays", at = @At("HEAD"))
    private void beforeChatHook(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (lastTimeMillis != null) {
            CobblemonClient.INSTANCE.beforeChatRender(context, (System.currentTimeMillis() - lastTimeMillis) / 1000F * 20);
        }
        lastTimeMillis = System.currentTimeMillis();
    }

    // Hides the HUD while the Pokédex scanner is open in first person. Gui was a LayeredDraw whose two hideGui-gated
    // layers this used to wrap; it extracts directly since 1.21.6, so the same hideGui checks are wrapped instead.
    @ModifyExpressionValue(
        method = "extractRenderState",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;hideGui:Z")
    )
    private boolean cobblemon$dontRenderUiInPokedex(boolean hideGui) {
        if (CobblemonClient.INSTANCE.getPokedexUsageContext().getScanningGuiOpen() && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return true;
        }
        return hideGui;
    }
}
